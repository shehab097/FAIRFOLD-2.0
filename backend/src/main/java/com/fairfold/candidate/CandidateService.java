package com.fairfold.candidate;

import com.fairfold.application.Application;
import com.fairfold.application.ApplicationRepository;
import com.fairfold.candidate.dto.AddSkillRequest;
import com.fairfold.candidate.dto.CandidateSkillResponse;
import com.fairfold.candidate.dto.DashboardResponse;
import com.fairfold.candidate.dto.ProfileRequest;
import com.fairfold.candidate.dto.ProfileResponse;
import com.fairfold.candidate.dto.UpdateSkillRequest;
import com.fairfold.common.errors.ApiException;
import com.fairfold.job.Job;
import com.fairfold.job.JobRepository;
import com.fairfold.resume.ResumeRepository;
import com.fairfold.skill.Skill;
import com.fairfold.skill.SkillRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CandidateService {

    private final CandidateProfileRepository profileRepository;
    private final CandidateSkillRepository candidateSkillRepository;
    private final SkillRepository skillRepository;
    private final ResumeRepository resumeRepository;
    private final ApplicationRepository applicationRepository;
    private final JobRepository jobRepository;

    public CandidateService(CandidateProfileRepository profileRepository,
                            CandidateSkillRepository candidateSkillRepository,
                            SkillRepository skillRepository,
                            ResumeRepository resumeRepository,
                            ApplicationRepository applicationRepository,
                            JobRepository jobRepository) {
        this.profileRepository = profileRepository;
        this.candidateSkillRepository = candidateSkillRepository;
        this.skillRepository = skillRepository;
        this.resumeRepository = resumeRepository;
        this.applicationRepository = applicationRepository;
        this.jobRepository = jobRepository;
    }

    // --- profile -----------------------------------------------------------

    @Transactional(readOnly = true)
    public ProfileResponse getProfile(UUID userId) {
        return profileRepository.findById(userId)
                .map(this::toProfileResponse)
                .orElseGet(() -> new ProfileResponse(null, null, null, null,
                        RemotePreference.any.name(), null, null, List.of()));
    }

    @Transactional
    public ProfileResponse updateProfile(UUID userId, ProfileRequest request) {
        CandidateProfile profile = profileRepository.findById(userId).orElseGet(() -> {
            CandidateProfile p = new CandidateProfile();
            p.setUserId(userId);
            return p;
        });
        profile.setPhone(trimToNull(request.phone()));
        profile.setTitle(trimToNull(request.title()));
        profile.setBio(trimToNull(request.bio()));
        profile.setLocation(trimToNull(request.location()));
        profile.setRemotePreference(parseRemotePreference(request.remotePreference()));
        profile.setTotalExperienceYears(request.totalExperienceYears());
        profile.setEducationLevel(parseEducationLevel(request.educationLevel()));
        profile.setPreviousJobTitles(cleanTitles(request.previousJobTitles()));
        profileRepository.save(profile);
        return toProfileResponse(profile);
    }

    // --- skills ------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<CandidateSkillResponse> listSkills(UUID userId) {
        List<CandidateSkill> skills = candidateSkillRepository.findByUserIdOrderByCreatedAtAsc(userId);
        Map<UUID, String> names = skillNames(skills);
        return skills.stream()
                .map(s -> toSkillResponse(s, names.get(s.getSkillId())))
                .toList();
    }

    @Transactional
    public CandidateSkillResponse addSkill(UUID userId, AddSkillRequest request) {
        if (request.skillId() == null) {
            throw ApiException.badRequest("SKILL_NOT_FOUND", "Choose a skill.");
        }
        Skill skill = skillRepository.findById(request.skillId())
                .orElseThrow(() -> ApiException.badRequest("SKILL_NOT_FOUND", "Skill not found."));
        if (candidateSkillRepository.existsByUserIdAndSkillId(userId, skill.getId())) {
            throw ApiException.conflict("SKILL_EXISTS", "You already have this skill.");
        }
        CandidateSkill candidateSkill = new CandidateSkill();
        candidateSkill.setUserId(userId);
        candidateSkill.setSkillId(skill.getId());
        candidateSkill.setLevel(parseLevel(request.level()));
        candidateSkill.setYears(validateYears(request.years()));
        candidateSkillRepository.save(candidateSkill);
        return toSkillResponse(candidateSkill, skill.getName());
    }

    @Transactional
    public CandidateSkillResponse updateSkill(UUID userId, UUID skillId, UpdateSkillRequest request) {
        CandidateSkill candidateSkill = candidateSkillRepository.findByIdAndUserId(skillId, userId)
                .orElseThrow(() -> ApiException.notFound("SKILL_NOT_FOUND", "Skill not found."));
        candidateSkill.setLevel(parseLevel(request.level()));
        candidateSkill.setYears(validateYears(request.years()));
        candidateSkillRepository.save(candidateSkill);
        String name = skillRepository.findById(candidateSkill.getSkillId())
                .map(Skill::getName).orElse(null);
        return toSkillResponse(candidateSkill, name);
    }

    @Transactional
    public void deleteSkill(UUID userId, UUID skillId) {
        CandidateSkill candidateSkill = candidateSkillRepository.findByIdAndUserId(skillId, userId)
                .orElseThrow(() -> ApiException.notFound("SKILL_NOT_FOUND", "Skill not found."));
        candidateSkillRepository.delete(candidateSkill);
    }

    // --- dashboard ---------------------------------------------------------

    @Transactional(readOnly = true)
    public DashboardResponse dashboard(UUID userId) {
        CandidateProfile profile = profileRepository.findById(userId).orElse(null);
        boolean profileSaved = profile != null
                && notBlank(profile.getTitle()) && notBlank(profile.getLocation());
        boolean resumeUploaded = resumeRepository.findByUserIdAndCurrentTrue(userId).isPresent();
        boolean skillsAdded = candidateSkillRepository.countByUserId(userId) > 0;

        List<Application> latest = applicationRepository.findTop5ByCandidateIdOrderByCreatedAtDesc(userId);
        Map<UUID, Job> jobs = jobRepository.findAllById(
                        latest.stream().map(Application::getJobId).toList()).stream()
                .collect(Collectors.toMap(Job::getId, Function.identity()));

        List<DashboardResponse.LatestApplication> items = latest.stream()
                .map(a -> new DashboardResponse.LatestApplication(
                        a.getId(), a.getJobId(),
                        jobs.containsKey(a.getJobId()) ? jobs.get(a.getJobId()).getTitle() : null,
                        a.getStatus().name(), null))
                .toList();

        return new DashboardResponse(
                new DashboardResponse.Checklist(profileSaved, resumeUploaded, skillsAdded), items);
    }

    // --- helpers -----------------------------------------------------------

    private Map<UUID, String> skillNames(List<CandidateSkill> skills) {
        List<UUID> ids = skills.stream().map(CandidateSkill::getSkillId).toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return skillRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Skill::getId, Skill::getName));
    }

    private CandidateSkillResponse toSkillResponse(CandidateSkill skill, String name) {
        return new CandidateSkillResponse(
                skill.getId(), skill.getSkillId(), name,
                skill.getLevel().name(), skill.getYears(), skill.isVerified());
    }

    private ProfileResponse toProfileResponse(CandidateProfile profile) {
        return new ProfileResponse(
                profile.getPhone(), profile.getTitle(), profile.getBio(), profile.getLocation(),
                profile.getRemotePreference() == null ? RemotePreference.any.name() : profile.getRemotePreference().name(),
                profile.getTotalExperienceYears(),
                profile.getEducationLevel() == null ? null : profile.getEducationLevel().name(),
                profile.getPreviousJobTitles());
    }

    private java.util.List<String> cleanTitles(java.util.List<String> titles) {
        java.util.List<String> out = new java.util.ArrayList<>();
        if (titles != null) {
            for (String title : titles) {
                String value = trimToNull(title);
                if (value != null && out.size() < 20) {
                    out.add(value.length() > 255 ? value.substring(0, 255) : value);
                }
            }
        }
        return out;
    }

    private CandidateSkillLevel parseLevel(String value) {
        try {
            return CandidateSkillLevel.valueOf(value.trim().toLowerCase());
        } catch (RuntimeException e) {
            throw ApiException.badRequest("INVALID_LEVEL",
                    "level must be one of beginner, intermediate, expert.");
        }
    }

    private BigDecimal validateYears(BigDecimal years) {
        if (years == null) {
            return null;
        }
        if (years.compareTo(BigDecimal.ZERO) < 0 || years.compareTo(BigDecimal.valueOf(60)) > 0) {
            throw ApiException.badRequest("INVALID_YEARS", "years must be between 0 and 60.");
        }
        return years;
    }

    private RemotePreference parseRemotePreference(String value) {
        if (value == null || value.isBlank()) {
            return RemotePreference.any;
        }
        try {
            return RemotePreference.valueOf(value.trim().toLowerCase());
        } catch (RuntimeException e) {
            throw ApiException.badRequest("INVALID_REMOTE_PREFERENCE",
                    "remotePreference must be one of onsite, hybrid, remote, any.");
        }
    }

    private EducationLevel parseEducationLevel(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return EducationLevel.valueOf(value.trim().toLowerCase());
        } catch (RuntimeException e) {
            throw ApiException.badRequest("INVALID_EDUCATION_LEVEL",
                    "educationLevel is not a valid value.");
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    private boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}
