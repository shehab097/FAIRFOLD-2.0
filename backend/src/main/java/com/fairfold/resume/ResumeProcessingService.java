package com.fairfold.resume;

import com.fairfold.ai.AiGateway;
import com.fairfold.ai.IdentifierLeakException;
import com.fairfold.ai.ProfileSuggestions;
import com.fairfold.candidate.CandidateProfile;
import com.fairfold.candidate.CandidateProfileRepository;
import com.fairfold.common.audit.AuditService;
import com.fairfold.common.storage.StorageService;
import com.fairfold.user.User;
import com.fairfold.user.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Runs the resume pipeline off the request thread: read PDF text, strip
 * personal details, call the AI gateway, store suggestions and placeholder
 * mappings, and move the resume to ready or failed.
 */
@Service
public class ResumeProcessingService {

    private static final Logger log = LoggerFactory.getLogger(ResumeProcessingService.class);

    private final ResumeRepository resumeRepository;
    private final ResumeSuggestionRepository suggestionRepository;
    private final ResumePlaceholderRepository placeholderRepository;
    private final StripTermRepository stripTermRepository;
    private final ResumeTextExtractor extractor;
    private final ResumeStripper stripper = new ResumeStripper();
    private final AiGateway aiGateway;
    private final AuditService auditService;
    private final StorageService storageService;
    private final UserRepository userRepository;
    private final CandidateProfileRepository profileRepository;
    private final ObjectMapper objectMapper;

    public ResumeProcessingService(ResumeRepository resumeRepository,
                                   ResumeSuggestionRepository suggestionRepository,
                                   ResumePlaceholderRepository placeholderRepository,
                                   StripTermRepository stripTermRepository,
                                   ResumeTextExtractor extractor,
                                   AiGateway aiGateway,
                                   AuditService auditService,
                                   StorageService storageService,
                                   UserRepository userRepository,
                                   CandidateProfileRepository profileRepository,
                                   ObjectMapper objectMapper) {
        this.resumeRepository = resumeRepository;
        this.suggestionRepository = suggestionRepository;
        this.placeholderRepository = placeholderRepository;
        this.stripTermRepository = stripTermRepository;
        this.extractor = extractor;
        this.aiGateway = aiGateway;
        this.auditService = auditService;
        this.storageService = storageService;
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.objectMapper = objectMapper;
    }

    @Async("resumeExecutor")
    public void process(UUID resumeId) {
        processNow(resumeId);
    }

    /** Visible for recovery and tests; safe to call more than once. */
    public void processNow(UUID resumeId) {
        Resume resume = resumeRepository.findById(resumeId).orElse(null);
        if (resume == null || resume.getStatus() != ResumeStatus.processing) {
            return;
        }
        try {
            byte[] pdf = storageService.load(resume.getStorageKey());
            String text = extractor.extract(pdf);

            User user = userRepository.findById(resume.getUserId()).orElse(null);
            if (user == null) {
                fail(resume, "PROCESSING_FAILED");
                return;
            }
            CandidateProfile profile = profileRepository.findById(resume.getUserId()).orElse(null);
            ResumeStripper.Identifiers identifiers = new ResumeStripper.Identifiers(
                    user.getFirstName(), user.getLastName(), user.getEmail(),
                    profile == null ? null : profile.getPhone());
            ResumeStripper.Terms terms = loadTerms();
            ResumeStripper.Result stripped = stripper.strip(text, identifiers, terms);

            suggestionRepository.deleteByResumeId(resumeId);
            placeholderRepository.deleteByResumeId(resumeId);

            resume.setStrippedText(stripped.text());
            for (ResumeStripper.Mapping mapping : stripped.mappings()) {
                ResumePlaceholder row = new ResumePlaceholder();
                row.setResumeId(resumeId);
                row.setPlaceholder(mapping.placeholder());
                row.setKind(mapping.kind());
                row.setOriginalValue(mapping.originalValue());
                row.setStartMonth(mapping.startMonth());
                row.setEndMonth(mapping.endMonth());
                row.setCurrent(mapping.current());
                placeholderRepository.save(row);
            }

            AiGateway.ProfileExtraction extraction =
                    aiGateway.extractProfile(resume.getUserId(), resumeId, stripped.text(), identifiers);
            ProfileSuggestions suggestions = extraction.suggestions();
            ResumeSuggestion saved = new ResumeSuggestion();
            saved.setResumeId(resumeId);
            saved.setProvider("rule_based".equals(extraction.provider()) ? "rule_based" : "ai");
            saved.setData(objectMapper.writeValueAsString(suggestions));
            suggestionRepository.save(saved);

            resume.setStatus(ResumeStatus.ready);
            resume.setFailureReason(null);
            resumeRepository.save(resume);
            auditService.record(resume.getUserId(), "resume_stripped", "resume",
                    resumeId.toString(), "{\"provider\":\"" + saved.getProvider() + "\"}");
        } catch (ResumeExtractionException e) {
            fail(resume, e.getCode());
        } catch (IdentifierLeakException e) {
            fail(resume, "IDENTIFIER_LEAK");
        } catch (Exception e) {
            // Never log resume text or identifiers.
            log.warn("Resume processing failed for {}: {}", resumeId, e.getClass().getSimpleName());
            fail(resume, "PROCESSING_FAILED");
        }
    }

    private ResumeStripper.Terms loadTerms() {
        List<String> institutions = stripTermRepository.findByKind("institution").stream()
                .map(StripTerm::getTerm).toList();
        List<String> employers = stripTermRepository.findByKind("employer").stream()
                .map(StripTerm::getTerm).toList();
        return new ResumeStripper.Terms(institutions, employers);
    }

    private void fail(Resume resume, String code) {
        resume.setStatus(ResumeStatus.failed);
        resume.setFailureReason(code);
        resume.setStrippedText(null);
        resumeRepository.save(resume);
    }
}
