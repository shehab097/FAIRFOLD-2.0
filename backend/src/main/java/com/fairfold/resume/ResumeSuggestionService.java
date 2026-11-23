package com.fairfold.resume;

import com.fairfold.ai.ProfileSuggestions;
import com.fairfold.assessment.AssessmentQueueService;
import com.fairfold.candidate.CandidateProfile;
import com.fairfold.candidate.CandidateProfileRepository;
import com.fairfold.candidate.CandidateSkill;
import com.fairfold.candidate.CandidateSkillLevel;
import com.fairfold.candidate.CandidateSkillRepository;
import com.fairfold.candidate.EducationLevel;
import com.fairfold.common.errors.ApiException;
import com.fairfold.resume.dto.ApplySuggestionsRequest;
import com.fairfold.resume.dto.StripReportRequest;
import com.fairfold.resume.dto.SuggestionResponse;
import com.fairfold.skill.Skill;
import com.fairfold.skill.SkillCanonicalizer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ResumeSuggestionService {

    private final ResumeRepository resumeRepository;
    private final ResumeSuggestionRepository suggestionRepository;
    private final ResumePlaceholderRepository placeholderRepository;
    private final StripReportRepository stripReportRepository;
    private final CandidateProfileRepository profileRepository;
    private final CandidateSkillRepository candidateSkillRepository;
    private final SkillCanonicalizer canonicalizer;
    private final WorkHistoryRepository workHistoryRepository;
    private final AssessmentQueueService assessmentQueueService;
    private final ObjectMapper objectMapper;

    public ResumeSuggestionService(ResumeRepository resumeRepository,
                                   ResumeSuggestionRepository suggestionRepository,
                                   ResumePlaceholderRepository placeholderRepository,
                                   StripReportRepository stripReportRepository,
                                   CandidateProfileRepository profileRepository,
                                   CandidateSkillRepository candidateSkillRepository,
                                   SkillCanonicalizer canonicalizer,
                                   WorkHistoryRepository workHistoryRepository,
                                   AssessmentQueueService assessmentQueueService,
                                   ObjectMapper objectMapper) {
        this.resumeRepository = resumeRepository;
        this.suggestionRepository = suggestionRepository;
        this.placeholderRepository = placeholderRepository;
        this.stripReportRepository = stripReportRepository;
        this.profileRepository = profileRepository;
        this.candidateSkillRepository = candidateSkillRepository;
        this.canonicalizer = canonicalizer;
        this.workHistoryRepository = workHistoryRepository;
        this.assessmentQueueService = assessmentQueueService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public SuggestionResponse get(UUID userId) {
        Resume resume = currentResume(userId);
        if (resume.getStatus() != ResumeStatus.ready) {
            throw ApiException.conflict("RESUME_NOT_READY", "Resume suggestions are not ready yet.");
        }
        ResumeSuggestion stored = suggestionRepository.findByResumeId(resume.getId())
                .orElseThrow(() -> ApiException.notFound("SUGGESTIONS_NOT_FOUND",
                        "No suggestions were extracted from this resume."));
        ProfileSuggestions data = read(stored.getData());
        Map<String, ResumePlaceholder> placeholders = placeholderRepository
                .findByResumeIdOrderByPlaceholderAsc(resume.getId()).stream()
                .collect(Collectors.toMap(ResumePlaceholder::getPlaceholder, Function.identity(),
                        (a, b) -> a, LinkedHashMap::new));

        List<SuggestionResponse.Skill> skills = data.skills().stream()
                .map(skill -> {
                    var canonical = canonicalizer.resolve(skill.name());
                    return new SuggestionResponse.Skill(
                            canonical.map(Skill::getName).orElse(skill.name()),
                            canonical.map(Skill::getId).orElse(null),
                            normalizeLevel(skill.level()),
                            skill.years());
                })
                .toList();

        List<SuggestionResponse.Work> work = data.workHistory().stream()
                .map(item -> {
                    ResumePlaceholder mapping = placeholderFor(placeholders, item.employerPlaceholder());
                    return new SuggestionResponse.Work(
                            item.title(),
                            mapping == null ? null : mapping.getOriginalValue(),
                            mapping == null ? null : MonthFormat.format(mapping.getStartMonth()),
                            mapping == null ? null : MonthFormat.format(mapping.getEndMonth()),
                            mapping != null && mapping.isCurrent(),
                            item.skillsUsed());
                })
                .toList();

        return new SuggestionResponse(stored.getProvider(), skills, data.totalExperienceYears(),
                data.educationLevel(), data.jobTitles(), work);
    }

    @Transactional
    public void apply(UUID userId, ApplySuggestionsRequest request) {
        if (request == null) {
            throw ApiException.badRequest("INVALID_REQUEST", "Nothing to save.");
        }
        CandidateProfile profile = profileRepository.findById(userId).orElseGet(() -> {
            CandidateProfile created = new CandidateProfile();
            created.setUserId(userId);
            return created;
        });
        profile.setTotalExperienceYears(validateYears(request.totalExperienceYears()));
        profile.setEducationLevel(parseEducation(request.educationLevel()));
        profile.setPreviousJobTitles(cleanTitles(request.jobTitles()));
        profileRepository.save(profile);

        if (request.skills() != null) {
            for (ApplySuggestionsRequest.Skill skill : request.skills()) {
                if (skill == null || skill.name() == null || skill.name().isBlank()) {
                    continue;
                }
                // One odd AI-suggested name must not block the whole save.
                if (canonicalizer.resolve(skill.name()).isEmpty()
                        && !canonicalizer.isValidNewSkillName(skill.name())) {
                    continue;
                }
                Skill canonical = canonicalizer.resolveOrCreate(skill.name());
                CandidateSkill row = candidateSkillRepository
                        .findByUserIdAndSkillId(userId, canonical.getId())
                        .orElseGet(() -> {
                            CandidateSkill created = new CandidateSkill();
                            created.setUserId(userId);
                            created.setSkillId(canonical.getId());
                            return created;
                        });
                row.setLevel(parseLevel(skill.level()));
                row.setYears(validateSkillYears(skill.years()));
                candidateSkillRepository.save(row);
                assessmentQueueService.ensureTopicForSkill(canonical.getId());
            }
        }

        workHistoryRepository.deleteByUserId(userId);
        if (request.workHistory() != null) {
            for (ApplySuggestionsRequest.Work work : request.workHistory()) {
                if (work == null || work.title() == null || work.title().isBlank()) {
                    continue;
                }
                WorkHistory row = new WorkHistory();
                row.setUserId(userId);
                row.setTitle(limit(work.title().strip(), 255));
                row.setEmployer(limit(clean(work.employer()), 255));
                LocalDate start = MonthFormat.parse(work.startMonth(), "Start month");
                LocalDate end = MonthFormat.parse(work.endMonth(), "End month");
                row.setStartMonth(start);
                row.setEndMonth(work.isCurrent() ? null : end);
                row.setCurrent(work.isCurrent());
                row.setSkillsUsed(cleanSkills(work.skillsUsed()));
                workHistoryRepository.save(row);
            }
        }
    }

    @Transactional
    public void reportStrip(UUID userId, StripReportRequest request) {
        String note = clean(request == null ? null : request.note());
        if (note == null) {
            throw ApiException.badRequest("NOTE_REQUIRED", "Describe what was stripped wrongly.");
        }
        Resume resume = currentResume(userId);
        StripReport report = new StripReport();
        report.setResumeId(resume.getId());
        report.setUserId(userId);
        report.setNote(limit(note, 4000));
        report.setExcerpt(limit(clean(request.excerpt()), 2000));
        stripReportRepository.save(report);
    }

    private Resume currentResume(UUID userId) {
        return resumeRepository.findByUserIdAndCurrentTrue(userId)
                .orElseThrow(() -> ApiException.notFound("RESUME_NOT_FOUND", "No resume uploaded yet."));
    }

    private ResumePlaceholder placeholderFor(Map<String, ResumePlaceholder> placeholders, String value) {
        if (value == null) {
            return null;
        }
        String key = value.replace("[", "").replace("]", "").strip();
        return placeholders.get(key);
    }

    private ProfileSuggestions read(String json) {
        try {
            return objectMapper.readValue(json, ProfileSuggestions.class);
        } catch (Exception e) {
            throw new IllegalStateException("Stored resume suggestions are unreadable", e);
        }
    }

    private List<String> cleanTitles(List<String> titles) {
        List<String> out = new ArrayList<>();
        if (titles != null) {
            for (String title : titles) {
                String value = clean(title);
                if (value != null && out.size() < 20) {
                    out.add(limit(value, 255));
                }
            }
        }
        return out;
    }

    private List<String> cleanSkills(List<String> skills) {
        List<String> out = new ArrayList<>();
        if (skills != null) {
            for (String skill : skills) {
                String value = clean(skill);
                if (value != null && out.size() < 30) {
                    out.add(limit(value, 100));
                }
            }
        }
        return out;
    }

    private String clean(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.strip();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String limit(String value, int max) {
        return value == null || value.length() <= max ? value : value.substring(0, max);
    }

    private CandidateSkillLevel parseLevel(String value) {
        if (value == null || value.isBlank()) {
            return CandidateSkillLevel.beginner;
        }
        try {
            return CandidateSkillLevel.valueOf(value.strip().toLowerCase());
        } catch (RuntimeException e) {
            throw ApiException.badRequest("INVALID_LEVEL",
                    "level must be one of beginner, intermediate, expert.");
        }
    }

    private String normalizeLevel(String value) {
        return parseLevel(value).name();
    }

    private EducationLevel parseEducation(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return EducationLevel.valueOf(value.strip().toLowerCase());
        } catch (RuntimeException e) {
            throw ApiException.badRequest("INVALID_EDUCATION_LEVEL", "educationLevel is not valid.");
        }
    }

    private BigDecimal validateYears(BigDecimal years) {
        if (years == null) {
            return null;
        }
        if (years.compareTo(BigDecimal.ZERO) < 0 || years.compareTo(BigDecimal.valueOf(60)) > 0) {
            throw ApiException.badRequest("INVALID_YEARS", "Total experience must be between 0 and 60.");
        }
        return years;
    }

    private BigDecimal validateSkillYears(BigDecimal years) {
        if (years == null) {
            return null;
        }
        if (years.compareTo(BigDecimal.ZERO) < 0 || years.compareTo(BigDecimal.valueOf(60)) > 0) {
            throw ApiException.badRequest("INVALID_YEARS", "Skill years must be between 0 and 60.");
        }
        return years;
    }
}
