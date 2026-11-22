package com.fairfold.screening;

import com.fairfold.ai.AiGateway;
import com.fairfold.ai.AiResult;
import com.fairfold.ai.IdentifierLeakException;
import com.fairfold.ai.ProfileSuggestions;
import com.fairfold.application.Application;
import com.fairfold.application.ApplicationRepository;
import com.fairfold.application.ApplicationStatus;
import com.fairfold.application.ApplicationStatusService;
import com.fairfold.candidate.CandidateProfile;
import com.fairfold.candidate.CandidateProfileRepository;
import com.fairfold.candidate.CandidateSkill;
import com.fairfold.candidate.CandidateSkillRepository;
import com.fairfold.job.Job;
import com.fairfold.job.JobRepository;
import com.fairfold.job.JobSkill;
import com.fairfold.job.JobSkillKind;
import com.fairfold.resume.Resume;
import com.fairfold.resume.ResumeRepository;
import com.fairfold.resume.ResumeStatus;
import com.fairfold.resume.ResumeStripper;
import com.fairfold.resume.ResumeSuggestionRepository;
import com.fairfold.user.User;
import com.fairfold.user.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Processes exactly one screening item in its own transaction: hard filters,
 * AI or rule-based verdicts, quote validation, score, bias scan and status.
 */
@Service
public class ScreeningItemProcessor {

    private final ScreeningItemRepository itemRepository;
    private final ScreeningRunRepository runRepository;
    private final ApplicationRepository applicationRepository;
    private final ApplicationClaimRepository claimRepository;
    private final ApplicationStatusService statusService;
    private final JobRepository jobRepository;
    private final ResumeRepository resumeRepository;
    private final ResumeSuggestionRepository suggestionRepository;
    private final CandidateProfileRepository profileRepository;
    private final CandidateSkillRepository candidateSkillRepository;
    private final UserRepository userRepository;
    private final HardFilterService hardFilterService;
    private final ScoringService scoringService;
    private final BiasScanner biasScanner;
    private final AiGateway aiGateway;
    private final ObjectMapper objectMapper;

    public ScreeningItemProcessor(ScreeningItemRepository itemRepository,
                                  ScreeningRunRepository runRepository,
                                  ApplicationRepository applicationRepository,
                                  ApplicationClaimRepository claimRepository,
                                  ApplicationStatusService statusService,
                                  JobRepository jobRepository,
                                  ResumeRepository resumeRepository,
                                  ResumeSuggestionRepository suggestionRepository,
                                  CandidateProfileRepository profileRepository,
                                  CandidateSkillRepository candidateSkillRepository,
                                  UserRepository userRepository,
                                  HardFilterService hardFilterService,
                                  ScoringService scoringService,
                                  BiasScanner biasScanner,
                                  AiGateway aiGateway,
                                  ObjectMapper objectMapper) {
        this.itemRepository = itemRepository;
        this.runRepository = runRepository;
        this.applicationRepository = applicationRepository;
        this.claimRepository = claimRepository;
        this.statusService = statusService;
        this.jobRepository = jobRepository;
        this.resumeRepository = resumeRepository;
        this.suggestionRepository = suggestionRepository;
        this.profileRepository = profileRepository;
        this.candidateSkillRepository = candidateSkillRepository;
        this.userRepository = userRepository;
        this.hardFilterService = hardFilterService;
        this.scoringService = scoringService;
        this.biasScanner = biasScanner;
        this.aiGateway = aiGateway;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void process(UUID itemId) {
        ScreeningItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalStateException("Screening item not found"));
        if (item.getStatus() == ItemStatus.done) {
            return;
        }
        item.setStatus(ItemStatus.running);
        item.setAttempts(item.getAttempts() + 1);
        item.setError(null);
        itemRepository.save(item);

        ScreeningRun run = runRepository.findById(item.getRunId())
                .orElseThrow(() -> new IllegalStateException("Screening run not found"));
        Application application = applicationRepository.findById(item.getApplicationId())
                .orElseThrow(() -> new IllegalStateException("Application not found"));
        Resume resume = resumeRepository.findById(application.getResumeId())
                .orElseThrow(() -> new IllegalStateException("Resume not found"));
        if (resume.getStatus() != ResumeStatus.ready || resume.getStrippedText() == null
                || resume.getStrippedText().isBlank()) {
            throw new IllegalStateException("Resume is not ready");
        }
        Job job = jobRepository.findById(application.getJobId())
                .orElseThrow(() -> new IllegalStateException("Job not found"));
        CandidateProfile profile = profileRepository.findById(application.getCandidateId()).orElse(null);
        List<CandidateSkill> candidateSkills =
                candidateSkillRepository.findByUserIdOrderByCreatedAtAsc(application.getCandidateId());
        User user = userRepository.findById(application.getCandidateId())
                .orElseThrow(() -> new IllegalStateException("Candidate not found"));

        ScreeningMode mode = run.getMode();
        String stripped = resume.getStrippedText();

        if (!item.isSkipFilters()) {
            var failed = hardFilterService.evaluate(job, profile, candidateSkills, stripped, false);
            if (failed.isPresent()) {
                markNotMatched(application, run, failed.get());
                finishItem(item);
                return;
            }
        }

        List<ScoringService.Requirement> requirements = requirements(job, application, profile);
        List<AiResult.Requirement> aiRequirements = requirements.stream()
                .map(r -> new AiResult.Requirement(r.key(), r.text(), r.kind().name()))
                .toList();
        ResumeStripper.Identifiers identifiers = new ResumeStripper.Identifiers(
                user.getFirstName(), user.getLastName(), user.getEmail(),
                profile == null ? null : profile.getPhone());

        AiGateway.ScreeningOutcome outcome = aiGateway.screen(
                application.getCandidateId(), application.getId(), resume.getId(),
                new AiResult.ScreeningRequest(aiRequirements, stripped), identifiers);
        ClaimSource source = "rule_based".equals(outcome.provider())
                ? ClaimSource.rule_based : ClaimSource.ai;
        List<ScoringService.ProviderVerdict> verdicts = new ArrayList<>();
        for (AiResult.Verdict verdict : outcome.verdicts()) {
            verdicts.add(new ScoringService.ProviderVerdict(
                    verdict.key(), verdict.verdict(), verdict.quote()));
        }

        ScoringService.Result scored = scoringService.score(requirements, verdicts, stripped, source);
        BiasScanner.Result biased = biasScanner.scan(scored.claims());
        List<String> missing = scoringService.missingSkills(biased.claims(), requirements);

        claimRepository.deleteByApplicationId(application.getId());
        for (ScoringService.Claim claim : biased.claims()) {
            ApplicationClaim row = new ApplicationClaim();
            row.setApplicationId(application.getId());
            row.setRequirementKey(claim.key());
            row.setRequirementText(claim.text());
            row.setKind(claim.kind());
            row.setVerdict(claim.verdict());
            row.setQuote(claim.quote());
            row.setWeight(claim.weight());
            row.setSource(claim.source());
            claimRepository.save(row);
        }

        application.setScore(biased.score());
        application.setScoringMode(source == ClaimSource.ai ? ScreeningMode.ai : ScreeningMode.rule_based);
        application.setBiasStatus(biased.status());
        application.setBiasNote(biased.note());
        application.setMissingSkills(missing);
        application.setNotMatchedRule(null);
        applicationRepository.save(application);
        statusService.changeStatus(application, ApplicationStatus.screened, run.getStartedBy(), null);

        finishItem(item);
    }

    @Transactional
    public void markFailed(UUID itemId, String code) {
        ScreeningItem item = itemRepository.findById(itemId).orElse(null);
        if (item == null) {
            return;
        }
        item.setStatus(ItemStatus.failed);
        item.setError(code);
        item.setAttempts(item.getAttempts() + 1);
        itemRepository.save(item);
        runRepository.incrementFailed(item.getRunId());
    }

    private void markNotMatched(Application application, ScreeningRun run, String rule) {
        claimRepository.deleteByApplicationId(application.getId());
        application.setScore(null);
        application.setScoringMode(ScreeningMode.rule_based);
        application.setBiasStatus(BiasStatus.clear);
        application.setBiasNote(null);
        application.setMissingSkills(List.of());
        application.setNotMatchedRule(rule);
        applicationRepository.save(application);
        statusService.changeStatus(application, ApplicationStatus.not_matched, run.getStartedBy(), rule);
    }

    private void finishItem(ScreeningItem item) {
        item.setStatus(ItemStatus.done);
        item.setError(null);
        itemRepository.save(item);
        runRepository.incrementDone(item.getRunId());
    }

    private List<ScoringService.Requirement> requirements(Job job, Application application,
                                                         CandidateProfile profile) {
        List<ScoringService.Requirement> requirements = new ArrayList<>();
        for (JobSkill js : job.getSkills()) {
            UUID skillId = js.getSkill().getId();
            ClaimKind kind = js.getKind() == JobSkillKind.required
                    ? ClaimKind.required : ClaimKind.nice_to_have;
            requirements.add(new ScoringService.Requirement(
                    "skill:" + skillId,
                    js.getSkill().getName(),
                    kind,
                    js.getKind() == JobSkillKind.required ? 3 : 1,
                    js.getSkill().getName()));
        }
        BigDecimal years = effectiveYears(application.getResumeId(), profile);
        String figure = years == null ? "unknown" : years.stripTrailingZeros().toPlainString() + " years";
        requirements.add(new ScoringService.Requirement(
                "experience",
                "Experience level: " + job.getExperienceLevel().name() + " (profile: " + figure + ")",
                ClaimKind.experience,
                1,
                null));
        return requirements;
    }

    private BigDecimal effectiveYears(UUID resumeId, CandidateProfile profile) {
        try {
            var stored = suggestionRepository.findByResumeId(resumeId).orElse(null);
            if (stored != null) {
                ProfileSuggestions data = objectMapper.readValue(stored.getData(), ProfileSuggestions.class);
                if (data.totalExperienceYears() != null) {
                    return data.totalExperienceYears();
                }
            }
        } catch (Exception ignored) {
            // Fall back to the profile value.
        }
        return profile == null ? null : profile.getTotalExperienceYears();
    }
}
