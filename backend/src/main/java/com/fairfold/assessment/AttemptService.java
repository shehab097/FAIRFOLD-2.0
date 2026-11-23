package com.fairfold.assessment;

import com.fairfold.assessment.dto.AttemptHistoryItem;
import com.fairfold.assessment.dto.AttemptQuestion;
import com.fairfold.assessment.dto.AttemptView;
import com.fairfold.assessment.dto.CandidateAssessment;
import com.fairfold.assessment.dto.CandidateResource;
import com.fairfold.candidate.CandidateSkill;
import com.fairfold.candidate.CandidateSkillLevel;
import com.fairfold.candidate.CandidateSkillRepository;
import com.fairfold.common.errors.ApiException;
import com.fairfold.common.paging.PageResponse;
import com.fairfold.config.AppProperties;
import com.fairfold.skill.Skill;
import com.fairfold.skill.SkillRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AttemptService {

    private static final int ANSWER_GRACE_SECONDS = 5;

    private final AssessmentRepository assessmentRepository;
    private final AssessmentQuestionRepository questionRepository;
    private final AssessmentAttemptRepository attemptRepository;
    private final LearningResourceRepository resourceRepository;
    private final CandidateSkillRepository candidateSkillRepository;
    private final SkillRepository skillRepository;
    private final GateService gateService;
    private final AppProperties properties;
    private final Clock clock;

    public AttemptService(AssessmentRepository assessmentRepository,
                          AssessmentQuestionRepository questionRepository,
                          AssessmentAttemptRepository attemptRepository,
                          LearningResourceRepository resourceRepository,
                          CandidateSkillRepository candidateSkillRepository,
                          SkillRepository skillRepository,
                          GateService gateService,
                          AppProperties properties,
                          Clock clock) {
        this.assessmentRepository = assessmentRepository;
        this.questionRepository = questionRepository;
        this.attemptRepository = attemptRepository;
        this.resourceRepository = resourceRepository;
        this.candidateSkillRepository = candidateSkillRepository;
        this.skillRepository = skillRepository;
        this.gateService = gateService;
        this.properties = properties;
        this.clock = clock;
    }

    // --- candidate catalogue ----------------------------------------------

    @Transactional(readOnly = true)
    public List<CandidateAssessment> listAssessments(UUID userId) {
        List<Assessment> assessments = assessmentRepository.findByActiveTrueOrderByCreatedAtDesc();
        Map<UUID, String> skillNames = skillNames(assessments.stream()
                .map(Assessment::getSkillId).toList());
        Instant now = clock.instant();
        List<CandidateAssessment> result = new ArrayList<>();
        for (Assessment assessment : assessments) {
            List<AssessmentAttempt> attempts = attemptRepository
                    .findByUserIdAndAssessmentIdOrderByStartedAtDesc(userId, assessment.getId());
            AssessmentAttempt inProgress = attempts.stream()
                    .filter(a -> a.getStatus() == AttemptStatus.in_progress)
                    .findFirst().orElse(null);
            AssessmentAttempt latestSubmitted = attempts.stream()
                    .filter(a -> a.getStatus() != AttemptStatus.in_progress)
                    .findFirst().orElse(null);
            Integer bestScore = attempts.stream()
                    .map(AssessmentAttempt::getScore)
                    .filter(score -> score != null)
                    .max(Integer::compareTo).orElse(null);
            boolean passed = attempts.stream().anyMatch(a -> Boolean.TRUE.equals(a.getPassed()));
            Instant lastAttemptAt = attempts.isEmpty() ? null
                    : attempts.get(0).getSubmittedAt() != null
                    ? attempts.get(0).getSubmittedAt() : attempts.get(0).getStartedAt();
            Instant retryAt = inProgress == null && latestSubmitted != null
                    ? retryAt(latestSubmitted) : null;
            if (retryAt != null && !now.isBefore(retryAt)) {
                retryAt = null;
            }
            result.add(new CandidateAssessment(
                    assessment.getId(),
                    assessment.getTitle(),
                    skillNames.get(assessment.getSkillId()),
                    (int) questionRepository.countByAssessmentId(assessment.getId()),
                    assessment.getDurationMinutes(),
                    assessment.getPassMark(),
                    bestScore,
                    passed,
                    lastAttemptAt,
                    retryAt,
                    inProgress == null ? null : inProgress.getId()));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public PageResponse<AttemptHistoryItem> history(UUID userId, Pageable pageable) {
        Page<AssessmentAttempt> page = attemptRepository
                .findByUserIdAndStatusNotOrderBySubmittedAtDesc(userId, AttemptStatus.in_progress, pageable);
        Set<UUID> assessmentIds = new HashSet<>();
        page.getContent().forEach(a -> assessmentIds.add(a.getAssessmentId()));
        Map<UUID, Assessment> assessments = assessmentRepository.findAllById(assessmentIds).stream()
                .collect(Collectors.toMap(Assessment::getId, Function.identity()));
        List<AttemptHistoryItem> items = page.getContent().stream()
                .map(a -> new AttemptHistoryItem(
                        a.getId(),
                        assessments.containsKey(a.getAssessmentId())
                                ? assessments.get(a.getAssessmentId()).getTitle() : "Assessment",
                        a.getScore(),
                        a.getPassed(),
                        a.getStatus().name(),
                        a.getSubmittedAt()))
                .toList();
        return PageResponse.of(items, page.getNumber(), page.getSize(), page.getTotalElements());
    }

    @Transactional(readOnly = true)
    public List<CandidateResource> resources(UUID skillId) {
        return resourceRepository
                .findBySkillIdAndActiveTrueAndBrokenFalseOrderByCreatedAtDesc(skillId).stream()
                .map(r -> new CandidateResource(r.getId(), r.getTitle(), r.getUrl(), r.getCost(),
                        r.getLanguage(), r.getLevel()))
                .toList();
    }

    // --- attempts ----------------------------------------------------------

    @Transactional
    public AttemptView start(UUID userId, UUID assessmentId) {
        Assessment assessment = requireAssessment(assessmentId);
        if (!assessment.isActive()) {
            throw ApiException.conflict("ASSESSMENT_INACTIVE",
                    "This assessment is not available.");
        }
        Instant now = clock.instant();

        Optional<AssessmentAttempt> existing = attemptRepository
                .findFirstByUserIdAndAssessmentIdAndStatusOrderByStartedAtDesc(
                        userId, assessmentId, AttemptStatus.in_progress);
        if (existing.isPresent()) {
            AssessmentAttempt attempt = existing.get();
            if (!now.isAfter(attempt.getEndsAt().plusSeconds(ANSWER_GRACE_SECONDS))) {
                return inProgressView(attempt, assessment);
            }
            finish(attempt, assessment, now, true);
        }

        Optional<AssessmentAttempt> latest = attemptRepository
                .findFirstByUserIdAndAssessmentIdAndStatusNotOrderBySubmittedAtDesc(
                        userId, assessmentId, AttemptStatus.in_progress);
        if (latest.isPresent()) {
            Instant retryAt = retryAt(latest.get());
            if (retryAt != null && now.isBefore(retryAt)) {
                throw ApiException.cooldown(retryAt,
                        "You can retake this assessment after " + retryAt + ".");
            }
        }

        AssessmentAttempt attempt = new AssessmentAttempt();
        attempt.setAssessmentId(assessment.getId());
        attempt.setUserId(userId);
        attempt.setStartedAt(now);
        attempt.setEndsAt(now.plus(Duration.ofMinutes(assessment.getDurationMinutes())));
        attempt.setStatus(AttemptStatus.in_progress);
        attempt.setAnswers(new LinkedHashMap<>());
        attemptRepository.save(attempt);
        return inProgressView(attempt, assessment);
    }

    @Transactional
    public Map<String, String> save(UUID userId, UUID attemptId, Map<String, String> answers) {
        AssessmentAttempt attempt = requireOwnedAttempt(userId, attemptId);
        if (attempt.getStatus() != AttemptStatus.in_progress
                || clock.instant().isAfter(attempt.getEndsAt().plusSeconds(ANSWER_GRACE_SECONDS))) {
            throw ApiException.conflict("ATTEMPT_CLOSED",
                    "This attempt is closed and no longer accepts answers.");
        }
        List<AssessmentQuestion> questions = questionRepository
                .findByAssessmentIdOrderByPositionAsc(attempt.getAssessmentId());
        Set<String> questionIds = questions.stream()
                .map(q -> q.getId().toString()).collect(Collectors.toCollection(HashSet::new));
        Map<String, Set<String>> options = questions.stream()
                .collect(Collectors.toMap(
                        q -> q.getId().toString(),
                        q -> q.getOptions().stream().map(o -> o.id()).collect(Collectors.toSet())));

        Map<String, String> merged = new LinkedHashMap<>(attempt.getAnswers());
        for (Map.Entry<String, String> entry : answers.entrySet()) {
            if (!questionIds.contains(entry.getKey()) || entry.getValue() == null) {
                continue;
            }
            if (!options.getOrDefault(entry.getKey(), Set.of()).contains(entry.getValue())) {
                continue;
            }
            merged.put(entry.getKey(), entry.getValue());
        }
        attempt.setAnswers(merged);
        attemptRepository.save(attempt);
        return merged;
    }

    @Transactional
    public AttemptView submit(UUID userId, UUID attemptId) {
        AssessmentAttempt attempt = requireOwnedAttempt(userId, attemptId);
        Assessment assessment = requireAssessment(attempt.getAssessmentId());
        if (attempt.getStatus() != AttemptStatus.in_progress) {
            return submittedView(attempt, assessment);
        }
        Instant now = clock.instant();
        boolean late = now.isAfter(attempt.getEndsAt().plusSeconds(ANSWER_GRACE_SECONDS));
        finish(attempt, assessment, now, late);
        return submittedView(attempt, assessment);
    }

    @Transactional
    public AttemptView view(UUID userId, UUID attemptId) {
        AssessmentAttempt attempt = requireOwnedAttempt(userId, attemptId);
        Assessment assessment = requireAssessment(attempt.getAssessmentId());
        return attempt.getStatus() == AttemptStatus.in_progress
                ? inProgressView(attempt, assessment)
                : submittedView(attempt, assessment);
    }

    /** Called by the sweeper: auto-submits every attempt past its grace period. */
    @Transactional
    public int submitExpired() {
        Instant now = clock.instant();
        List<AssessmentAttempt> expired = attemptRepository.findByStatusAndEndsAtBefore(
                AttemptStatus.in_progress, now.minusSeconds(ANSWER_GRACE_SECONDS));
        int submitted = 0;
        for (AssessmentAttempt attempt : expired) {
            Assessment assessment = assessmentRepository.findById(attempt.getAssessmentId())
                    .orElse(null);
            if (assessment == null) {
                continue;
            }
            finish(attempt, assessment, now, true);
            submitted++;
        }
        return submitted;
    }

    // --- scoring and badges ------------------------------------------------

    private void finish(AssessmentAttempt attempt, Assessment assessment, Instant now, boolean auto) {
        List<AssessmentQuestion> questions = questionRepository
                .findByAssessmentIdOrderByPositionAsc(assessment.getId());
        int total = questions.size();
        int correct = correctCount(questions, attempt.getAnswers());
        int score = total == 0 ? 0 : (int) Math.round(100.0 * correct / total);
        boolean passed = score >= assessment.getPassMark();

        attempt.setScore(score);
        attempt.setPassed(passed);
        attempt.setSubmittedAt(now);
        attempt.setStatus(auto ? AttemptStatus.auto_submitted : AttemptStatus.submitted);
        attemptRepository.save(attempt);

        if (passed) {
            verifySkill(attempt.getUserId(), assessment.getSkillId(), now);
        }
        gateService.recomputeForCandidateAssessment(attempt.getUserId(), assessment.getId());
    }

    private void verifySkill(UUID userId, UUID skillId, Instant now) {
        CandidateSkill skill = candidateSkillRepository
                .findByUserIdAndSkillId(userId, skillId)
                .orElseGet(() -> {
                    CandidateSkill created = new CandidateSkill();
                    created.setUserId(userId);
                    created.setSkillId(skillId);
                    created.setLevel(CandidateSkillLevel.beginner);
                    created.setYears(null);
                    return created;
                });
        skill.setVerified(true);
        skill.setVerifiedAt(now);
        candidateSkillRepository.save(skill);
    }

    private int correctCount(List<AssessmentQuestion> questions, Map<String, String> answers) {
        int correct = 0;
        for (AssessmentQuestion question : questions) {
            String answer = answers.get(question.getId().toString());
            if (answer != null && answer.equals(question.getCorrectOptionId())) {
                correct++;
            }
        }
        return correct;
    }

    private Instant retryAt(AssessmentAttempt last) {
        if (last == null || last.getSubmittedAt() == null || !Boolean.FALSE.equals(last.getPassed())) {
            return null;
        }
        int hours = properties.assessment() == null ? 0 : Math.max(0, properties.assessment().cooldownHours());
        return last.getSubmittedAt().plus(Duration.ofHours(hours));
    }

    // --- views -------------------------------------------------------------

    private AttemptView inProgressView(AssessmentAttempt attempt, Assessment assessment) {
        List<AttemptQuestion> questions = questionRepository
                .findByAssessmentIdOrderByPositionAsc(assessment.getId()).stream()
                .map(q -> new AttemptQuestion(q.getId().toString(), q.getPosition(), q.getText(), q.getOptions()))
                .toList();
        return new AttemptView(
                attempt.getId(),
                assessment.getId(),
                assessment.getTitle(),
                skillName(assessment.getSkillId()),
                attempt.getEndsAt(),
                clock.instant(),
                questions,
                attempt.getAnswers(),
                null, null, assessment.getPassMark(), null, questions.size(),
                attempt.getStatus().name(), null,
                attempt.getProctoringConsent(), attempt.getExplanation(), null, null);
    }

    private AttemptView submittedView(AssessmentAttempt attempt, Assessment assessment) {
        List<AssessmentQuestion> questions = questionRepository
                .findByAssessmentIdOrderByPositionAsc(assessment.getId());
        boolean passed = Boolean.TRUE.equals(attempt.getPassed());
        return new AttemptView(
                attempt.getId(),
                assessment.getId(),
                assessment.getTitle(),
                skillName(assessment.getSkillId()),
                null,
                clock.instant(),
                null,
                attempt.getAnswers(),
                attempt.getScore(),
                attempt.getPassed(),
                assessment.getPassMark(),
                correctCount(questions, attempt.getAnswers()),
                questions.size(),
                attempt.getStatus().name(),
                passed,
                attempt.getProctoringConsent(),
                attempt.getExplanation(),
                retryAt(attempt),
                passed ? null : resources(assessment.getSkillId()));
    }

    private Assessment requireAssessment(UUID assessmentId) {
        return assessmentRepository.findById(assessmentId)
                .orElseThrow(() -> ApiException.notFound("ASSESSMENT_NOT_FOUND", "Assessment not found."));
    }

    private AssessmentAttempt requireOwnedAttempt(UUID userId, UUID attemptId) {
        AssessmentAttempt attempt = attemptRepository.findWithLockById(attemptId)
                .orElseThrow(() -> ApiException.notFound("ATTEMPT_NOT_FOUND", "Attempt not found."));
        if (!attempt.getUserId().equals(userId)) {
            throw ApiException.notFound("ATTEMPT_NOT_FOUND", "Attempt not found.");
        }
        return attempt;
    }

    private String skillName(UUID skillId) {
        return skillRepository.findById(skillId).map(Skill::getName).orElse(null);
    }

    private Map<UUID, String> skillNames(List<UUID> skillIds) {
        if (skillIds.isEmpty()) {
            return Map.of();
        }
        return skillRepository.findAllById(new HashSet<>(skillIds)).stream()
                .collect(Collectors.toMap(Skill::getId, Skill::getName));
    }
}
