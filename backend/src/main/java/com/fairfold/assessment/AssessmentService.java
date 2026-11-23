package com.fairfold.assessment;

import com.fairfold.assessment.dto.ActiveRequest;
import com.fairfold.assessment.dto.AssessmentDetail;
import com.fairfold.assessment.dto.AssessmentListItem;
import com.fairfold.assessment.dto.AssessmentRequest;
import com.fairfold.assessment.dto.EmployerAssessmentOption;
import com.fairfold.assessment.dto.QuestionOption;
import com.fairfold.assessment.dto.QuestionRequest;
import com.fairfold.ai.AiGateway;
import com.fairfold.ai.AiResult;
import com.fairfold.candidate.CandidateSkillRepository;
import com.fairfold.common.audit.AuditService;
import com.fairfold.common.errors.ApiException;
import com.fairfold.common.paging.PageResponse;
import com.fairfold.job.JobSkillRepository;
import com.fairfold.skill.Skill;
import com.fairfold.skill.SkillRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

@Service
public class AssessmentService {

    private final AssessmentRepository assessmentRepository;
    private final AssessmentQuestionRepository questionRepository;
    private final AssessmentAttemptRepository attemptRepository;
    private final SkillRepository skillRepository;
    private final CandidateSkillRepository candidateSkillRepository;
    private final JobSkillRepository jobSkillRepository;
    private final AiGateway aiGateway;
    private final AuditService auditService;

    public AssessmentService(AssessmentRepository assessmentRepository,
                             AssessmentQuestionRepository questionRepository,
                             AssessmentAttemptRepository attemptRepository,
                             SkillRepository skillRepository,
                             CandidateSkillRepository candidateSkillRepository,
                             JobSkillRepository jobSkillRepository,
                             AiGateway aiGateway,
                             AuditService auditService) {
        this.assessmentRepository = assessmentRepository;
        this.questionRepository = questionRepository;
        this.attemptRepository = attemptRepository;
        this.skillRepository = skillRepository;
        this.candidateSkillRepository = candidateSkillRepository;
        this.jobSkillRepository = jobSkillRepository;
        this.aiGateway = aiGateway;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public PageResponse<AssessmentListItem> list(Pageable pageable) {
        Page<Assessment> page = assessmentRepository.findAll(pageable);
        Map<UUID, Skill> skills = skillsFor(page.getContent().stream().map(Assessment::getSkillId).toList());
        List<AssessmentListItem> items = page.getContent().stream()
                .map(a -> new AssessmentListItem(
                        a.getId(),
                        a.getSkillId(),
                        a.getTitle(),
                        skillName(skills, a.getSkillId()),
                        (int) questionRepository.countByAssessmentId(a.getId()),
                        a.getDurationMinutes(),
                        a.getPassMark(),
                        a.isActive(),
                        attemptRepository.countByAssessmentId(a.getId()),
                        a.getReviewStatus(),
                        (int) questionRepository.countByAssessmentId(a.getId()),
                        a.getQuestionsPerAttempt(),
                        demandCount(a.getSkillId())))
                .toList();
        return PageResponse.of(items, page.getNumber(), page.getSize(), page.getTotalElements());
    }

    @Transactional(readOnly = true)
    public AssessmentDetail detail(UUID id) {
        Assessment assessment = require(id);
        Map<UUID, Skill> skills = skillsFor(List.of(assessment.getSkillId()));
        return toDetail(assessment, skills);
    }

    @Transactional(readOnly = true)
    public List<EmployerAssessmentOption> listActiveOptions() {
        List<Assessment> assessments = assessmentRepository.findByActiveTrueOrderByCreatedAtDesc();
        Map<UUID, Skill> skills = skillsFor(assessments.stream()
                .map(Assessment::getSkillId).toList());
        return assessments.stream()
                .map(a -> new EmployerAssessmentOption(
                        a.getId(), a.getTitle(), skillName(skills, a.getSkillId()),
                        a.getPassMark(), a.getDurationMinutes()))
                .toList();
    }

    @Transactional
    public AssessmentDetail create(UUID actorId, AssessmentRequest request) {
        List<QuestionRequest> questions = validate(request);
        Skill skill = requireSkill(request.skillId());

        Assessment assessment = new Assessment();
        assessment.setTitle(request.title().strip());
        assessment.setSkillId(skill.getId());
        assessment.setDurationMinutes(request.durationMinutes());
        assessment.setPassMark(request.passMark());
        assessment.setActive(request.active() == null || request.active());
        assessment.setQuestionsPerAttempt(request.questionsPerAttempt() == null
                ? 10 : request.questionsPerAttempt());
        assessment.setReviewStatus("approved");
        assessmentRepository.save(assessment);

        saveQuestions(assessment.getId(), questions);
        auditService.record(actorId, "assessment_created", "assessment",
                assessment.getId().toString(), "{}");
        return toDetail(assessment, Map.of(skill.getId(), skill));
    }

    @Transactional
    public AssessmentDetail update(UUID actorId, UUID id, AssessmentRequest request) {
        Assessment assessment = require(id);
        List<QuestionRequest> questions = validate(request);
        Skill skill = requireSkill(request.skillId());
        boolean locked = attemptRepository.countByAssessmentId(id) > 0;

        if (locked && contentChanged(assessment, skill.getId(), questions)) {
            throw ApiException.conflict("ASSESSMENT_HAS_ATTEMPTS",
                    "Questions can no longer change because candidates have attempted this assessment. "
                            + "Deactivate it and create a new assessment instead.");
        }

        assessment.setTitle(request.title().strip());
        assessment.setDurationMinutes(request.durationMinutes());
        assessment.setPassMark(request.passMark());
        assessment.setActive(request.active() == null ? assessment.isActive() : request.active());
        assessment.setQuestionsPerAttempt(request.questionsPerAttempt() == null
                ? assessment.getQuestionsPerAttempt() : request.questionsPerAttempt());
        assessment.setSkillId(skill.getId());
        assessmentRepository.save(assessment);

        if (!locked) {
            questionRepository.deleteByAssessmentId(id);
            saveQuestions(id, questions);
        }

        auditService.record(actorId, "assessment_updated", "assessment", id.toString(), "{}");
        return toDetail(assessment, Map.of(skill.getId(), skill));
    }

    @Transactional
    public AssessmentDetail setActive(UUID actorId, UUID id, ActiveRequest request) {
        Assessment assessment = require(id);
        boolean active = Boolean.TRUE.equals(request.active());
        assessment.setActive(active);
        assessmentRepository.save(assessment);
        auditService.record(actorId, active ? "assessment_activated" : "assessment_deactivated",
                "assessment", id.toString(), "{}");
        Map<UUID, Skill> skills = skillsFor(List.of(assessment.getSkillId()));
        return toDetail(assessment, skills);
    }

    @Transactional
    public AssessmentDetail generate(UUID actorId, UUID id) {
        return generate(actorId, id, 30);
    }

    @Transactional
    public AssessmentDetail generate(UUID actorId, UUID id, int requestedCount) {
        int count = Math.max(1, Math.min(100, requestedCount));
        Assessment assessment = require(id);
        if (attemptRepository.countByAssessmentId(id) > 0) {
            throw ApiException.conflict("ASSESSMENT_HAS_ATTEMPTS",
                    "Questions can no longer be regenerated because candidates have attempted this assessment.");
        }
        Skill skill = requireSkill(assessment.getSkillId());
        AiResult<List<AiResult.GeneratedQuestion>> result = aiGateway.generateAssessmentQuestions(
                actorId, skill.getName(), count);
        if (result.value() == null || result.value().isEmpty()) {
            throw ApiException.conflict("AI_UNAVAILABLE",
                    "The question generator returned no questions. Try again.");
        }
        List<QuestionRequest> questions = result.value().stream()
                .map(generated -> new QuestionRequest(
                        generated.text(),
                        generated.options().stream()
                                .map(option -> new QuestionOption(option.id(), option.text()))
                                .toList(),
                        generated.correctOptionId()))
                .toList();
        validateQuestions(questions);
        questionRepository.deleteByAssessmentId(id);
        questionRepository.flush();
        saveQuestions(id, questions);
        assessment.setReviewStatus("pending_review");
        assessment.setActive(false);
        assessmentRepository.save(assessment);
        auditService.record(actorId, "assessment_generated", "assessment", id.toString(),
                "{\"poolSize\":" + questions.size() + "}");
        return toDetail(assessment, Map.of(skill.getId(), skill));
    }

    @Transactional
    public AssessmentDetail approve(UUID actorId, UUID id) {
        Assessment assessment = require(id);
        if (questionRepository.countByAssessmentId(id) == 0) {
            throw ApiException.conflict("ASSESSMENT_EMPTY",
                    "Generate questions before approving this assessment.");
        }
        assessment.setReviewStatus("approved");
        assessment.setActive(true);
        assessmentRepository.save(assessment);
        auditService.record(actorId, "assessment_approved", "assessment", id.toString(), "{}");
        Map<UUID, Skill> skills = skillsFor(List.of(assessment.getSkillId()));
        return toDetail(assessment, skills);
    }

    // --- helpers -----------------------------------------------------------

    private Assessment require(UUID id) {
        return assessmentRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("ASSESSMENT_NOT_FOUND", "Assessment not found."));
    }

    private Skill requireSkill(UUID skillId) {
        return skillRepository.findById(skillId)
                .orElseThrow(() -> ApiException.badRequest("SKILL_NOT_FOUND", "Skill not found."));
    }

    private List<QuestionRequest> validate(AssessmentRequest request) {
        return validateQuestions(request.questions());
    }

    private List<QuestionRequest> validateQuestions(List<QuestionRequest> requested) {
        if (requested == null || requested.isEmpty()) {
            throw ApiException.badRequest("INVALID_ASSESSMENT", "An assessment needs at least one question.");
        }
        for (QuestionRequest question : requested) {
            if (question == null || question.text() == null || question.text().isBlank()) {
                throw ApiException.badRequest("INVALID_QUESTION", "Every question needs text.");
            }
            if (question.options() == null || question.options().size() < 2 || question.options().size() > 6) {
                throw ApiException.badRequest("INVALID_QUESTION",
                        "Every question needs between 2 and 6 options.");
            }
            Set<String> ids = new HashSet<>();
            for (QuestionOption option : question.options()) {
                if (option == null || option.id() == null || option.id().isBlank()
                        || option.text() == null || option.text().isBlank()) {
                    throw ApiException.badRequest("INVALID_QUESTION",
                            "Every option needs an id and text.");
                }
                if (!ids.add(option.id().strip())) {
                    throw ApiException.badRequest("INVALID_QUESTION", "Option ids must be unique.");
                }
            }
            if (question.correctOptionId() == null
                    || !ids.contains(question.correctOptionId().strip())) {
                throw ApiException.badRequest("INVALID_QUESTION",
                        "Exactly one correct option must be chosen.");
            }
        }
        return requested;
    }

    private boolean contentChanged(Assessment assessment, UUID skillId, List<QuestionRequest> questions) {
        if (!assessment.getSkillId().equals(skillId)) {
            return true;
        }
        List<AssessmentQuestion> existing = questionRepository
                .findByAssessmentIdOrderByPositionAsc(assessment.getId());
        if (existing.size() != questions.size()) {
            return true;
        }
        for (int i = 0; i < existing.size(); i++) {
            AssessmentQuestion old = existing.get(i);
            QuestionRequest next = questions.get(i);
            if (!old.getText().strip().equals(next.text().strip())) {
                return true;
            }
            if (!old.getCorrectOptionId().strip().equals(next.correctOptionId().strip())) {
                return true;
            }
            if (old.getOptions().size() != next.options().size()) {
                return true;
            }
            for (int o = 0; o < old.getOptions().size(); o++) {
                QuestionOption before = old.getOptions().get(o);
                QuestionOption after = next.options().get(o);
                if (!before.id().strip().equals(after.id().strip())
                        || !before.text().strip().equals(after.text().strip())) {
                    return true;
                }
            }
        }
        return false;
    }

    private void saveQuestions(UUID assessmentId, List<QuestionRequest> questions) {
        List<AssessmentQuestion> rows = new ArrayList<>();
        for (int i = 0; i < questions.size(); i++) {
            QuestionRequest request = questions.get(i);
            AssessmentQuestion row = new AssessmentQuestion();
            row.setAssessmentId(assessmentId);
            row.setPosition(i);
            row.setText(request.text().strip());
            row.setOptions(request.options().stream()
                    .map(o -> new QuestionOption(o.id().strip(), o.text().strip()))
                    .toList());
            row.setCorrectOptionId(request.correctOptionId().strip());
            rows.add(row);
        }
        questionRepository.saveAll(rows);
    }

    private Map<UUID, Skill> skillsFor(List<UUID> skillIds) {
        Map<UUID, Skill> result = new LinkedHashMap<>();
        skillRepository.findAllById(new HashSet<>(skillIds)).forEach(s -> result.put(s.getId(), s));
        return result;
    }

    private String skillName(Map<UUID, Skill> skills, UUID skillId) {
        Skill skill = skills.get(skillId);
        return skill == null ? null : skill.getName();
    }

    private AssessmentDetail toDetail(Assessment assessment, Map<UUID, Skill> skills) {
        List<AssessmentDetail.QuestionDto> questions = questionRepository
                .findByAssessmentIdOrderByPositionAsc(assessment.getId()).stream()
                .map(q -> new AssessmentDetail.QuestionDto(
                        q.getId(), q.getPosition(), q.getText(), q.getOptions(), q.getCorrectOptionId()))
                .toList();
        return new AssessmentDetail(
                assessment.getId(),
                assessment.getTitle(),
                assessment.getSkillId(),
                skillName(skills, assessment.getSkillId()),
                assessment.getDurationMinutes(),
                assessment.getPassMark(),
                assessment.isActive(),
                attemptRepository.countByAssessmentId(assessment.getId()),
                assessment.getReviewStatus(),
                questions.size(),
                assessment.getQuestionsPerAttempt(),
                demandCount(assessment.getSkillId()),
                questions);
    }

    private long demandCount(UUID skillId) {
        return candidateSkillRepository.countBySkillId(skillId)
                + jobSkillRepository.countBySkillId(skillId);
    }
}
