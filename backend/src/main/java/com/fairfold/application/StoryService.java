package com.fairfold.application;

import com.fairfold.ai.AiGateway;
import com.fairfold.ai.AiResult;
import com.fairfold.ai.IdentifierLeakException;
import com.fairfold.application.dto.StoryResponse;
import com.fairfold.candidate.CandidateProfile;
import com.fairfold.candidate.CandidateProfileRepository;
import com.fairfold.common.errors.ApiException;
import com.fairfold.job.Job;
import com.fairfold.job.JobRepository;
import com.fairfold.job.JobSkill;
import com.fairfold.job.JobSkillKind;
import com.fairfold.resume.Resume;
import com.fairfold.resume.ResumePlaceholder;
import com.fairfold.resume.ResumePlaceholderRepository;
import com.fairfold.resume.ResumeRepository;
import com.fairfold.resume.ResumeStatus;
import com.fairfold.resume.ResumeStripper;
import com.fairfold.user.User;
import com.fairfold.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** AI job story; never stored, placeholders restored before returning. */
@Service
public class StoryService {

    private final ApplicationRepository applicationRepository;
    private final JobRepository jobRepository;
    private final ResumeRepository resumeRepository;
    private final ResumePlaceholderRepository placeholderRepository;
    private final UserRepository userRepository;
    private final CandidateProfileRepository profileRepository;
    private final AiGateway aiGateway;

    public StoryService(ApplicationRepository applicationRepository,
                        JobRepository jobRepository,
                        ResumeRepository resumeRepository,
                        ResumePlaceholderRepository placeholderRepository,
                        UserRepository userRepository,
                        CandidateProfileRepository profileRepository,
                        AiGateway aiGateway) {
        this.applicationRepository = applicationRepository;
        this.jobRepository = jobRepository;
        this.resumeRepository = resumeRepository;
        this.placeholderRepository = placeholderRepository;
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.aiGateway = aiGateway;
    }

    @Transactional(readOnly = true)
    public StoryResponse story(UUID candidateId, UUID applicationId) {
        Application application = applicationRepository.findByIdAndCandidateId(applicationId, candidateId)
                .orElseThrow(() -> ApiException.notFound("APPLICATION_NOT_FOUND", "Application not found."));
        Job job = jobRepository.findById(application.getJobId())
                .orElseThrow(() -> ApiException.notFound("JOB_NOT_FOUND", "Job not found."));
        Resume resume = resumeRepository.findByUserIdAndCurrentTrue(candidateId)
                .orElseThrow(() -> ApiException.conflict("RESUME_REQUIRED",
                        "Upload and process a resume before writing a story."));
        if (resume.getStatus() != ResumeStatus.ready
                || resume.getStrippedText() == null || resume.getStrippedText().isBlank()) {
            throw ApiException.conflict("RESUME_REQUIRED",
                    "Upload and process a resume before writing a story.");
        }
        User user = userRepository.findById(candidateId).orElse(null);
        if (user == null) {
            throw ApiException.unauthorized("UNAUTHORIZED", "Authentication required.");
        }
        CandidateProfile profile = profileRepository.findById(candidateId).orElse(null);
        ResumeStripper.Identifiers identifiers = new ResumeStripper.Identifiers(
                user.getFirstName(), user.getLastName(), user.getEmail(),
                profile == null ? null : profile.getPhone());

        List<String> skills = job.getSkills().stream()
                .filter(skill -> skill.getKind() == JobSkillKind.required)
                .map(JobSkill::getSkill)
                .map(skill -> skill.getName())
                .toList();
        AiResult.JobText jobText = new AiResult.JobText(
                job.getTitle(), job.getDescription(), job.getRequirements(), skills);
        AiGateway.StoryOutcome outcome;
        try {
            outcome = aiGateway.writeStory(candidateId, applicationId, resume.getStrippedText(),
                    jobText, identifiers);
        } catch (IdentifierLeakException e) {
            throw ApiException.conflict("IDENTIFIER_LEAK",
                    "The resume still contains personal details. Fix the resume and try again.");
        }
        String story = restore(outcome.story(), resume.getId());
        return new StoryResponse(story, outcome.provider());
    }

    private String restore(String story, UUID resumeId) {
        Map<String, String> placeholders = new LinkedHashMap<>();
        for (ResumePlaceholder placeholder : placeholderRepository
                .findByResumeIdOrderByPlaceholderAsc(resumeId)) {
            placeholders.put("[" + placeholder.getPlaceholder() + "]", placeholder.getOriginalValue());
        }
        String restored = story;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            restored = restored.replace(entry.getKey(), entry.getValue());
        }
        return restored;
    }
}
