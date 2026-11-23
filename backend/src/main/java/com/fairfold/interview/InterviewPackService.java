package com.fairfold.interview;

import com.fairfold.ai.AiGateway;
import com.fairfold.ai.AiResult;
import com.fairfold.common.audit.AuditService;
import com.fairfold.common.errors.ApiException;
import com.fairfold.interview.dto.InterviewPackDto;
import com.fairfold.job.Job;
import com.fairfold.job.JobRepository;
import com.fairfold.job.JobScreeningQuestion;
import com.fairfold.job.JobScreeningQuestionRepository;
import com.fairfold.job.JobSkill;
import com.fairfold.job.JobSkillKind;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class InterviewPackService {

    private final JobRepository jobRepository;
    private final JobScreeningQuestionRepository questionRepository;
    private final InterviewPackRepository packRepository;
    private final AiGateway aiGateway;
    private final AuditService auditService;

    public InterviewPackService(JobRepository jobRepository,
                                JobScreeningQuestionRepository questionRepository,
                                InterviewPackRepository packRepository,
                                AiGateway aiGateway,
                                AuditService auditService) {
        this.jobRepository = jobRepository;
        this.questionRepository = questionRepository;
        this.packRepository = packRepository;
        this.aiGateway = aiGateway;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public InterviewPackDto get(UUID companyId, UUID jobId) {
        Job job = requireJob(companyId, jobId);
        InterviewPack pack = packRepository.findByJobId(job.getId()).orElse(null);
        return toDto(pack, screeningQuestions(job.getId()), null);
    }

    @Transactional(readOnly = true)
    public InterviewPackDto generate(UUID companyId, UUID userId, UUID jobId) {
        Job job = requireJob(companyId, jobId);
        List<String> screening = screeningQuestions(job.getId());
        AiResult<AiResult.InterviewPack> result = aiGateway.generateInterviewPack(
                userId, jobText(job), screening);
        List<InterviewPackDto.Question> questions = new ArrayList<>();
        for (String text : result.value().questions()) {
            if (text != null && !text.isBlank()) {
                questions.add(new InterviewPackDto.Question(UUID.randomUUID().toString(), text.strip()));
            }
        }
        List<InterviewPackDto.RubricItem> rubric = new ArrayList<>();
        for (AiResult.RubricItem item : result.value().rubric()) {
            rubric.add(new InterviewPackDto.RubricItem(
                    UUID.randomUUID().toString(), item.criterion(), item.description()));
        }
        String provider = "rule_based".equals(result.model()) ? "rule_based" : "ai";
        return new InterviewPackDto(questions, rubric, screening, provider);
    }

    @Transactional
    public InterviewPackDto save(UUID companyId, UUID userId, UUID jobId,
                                 InterviewPackDto.SaveRequest request) {
        Job job = requireJob(companyId, jobId);
        List<InterviewPackDto.Question> questions = cleanQuestions(request == null ? null : request.questions());
        List<InterviewPackDto.RubricItem> rubric = cleanRubric(request == null ? null : request.rubric());
        if (questions.isEmpty()) {
            throw ApiException.badRequest("INVALID_PACK", "Add at least one interview question.");
        }
        if (rubric.isEmpty()) {
            throw ApiException.badRequest("INVALID_PACK", "Add at least one rubric item.");
        }

        InterviewPack pack = packRepository.findByJobId(job.getId()).orElseGet(() -> {
            InterviewPack created = new InterviewPack();
            created.setJobId(job.getId());
            return created;
        });
        pack.setQuestions(questions.stream()
                .map(q -> new InterviewPackQuestion(q.id(), q.text())).toList());
        pack.setRubric(rubric.stream()
                .map(r -> new InterviewPackRubric(r.id(), r.criterion(), r.description())).toList());
        pack.setUpdatedBy(userId);
        packRepository.save(pack);
        auditService.record(userId, "interview_pack_saved", "job", job.getId().toString(), "{}");
        return toDto(pack, screeningQuestions(job.getId()), null);
    }

    @Transactional(readOnly = true)
    public InterviewPackDto forJob(UUID jobId) {
        InterviewPack pack = packRepository.findByJobId(jobId).orElse(null);
        return toDto(pack, List.of(), null);
    }

    private List<InterviewPackDto.Question> cleanQuestions(List<InterviewPackDto.Question> input) {
        List<InterviewPackDto.Question> result = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        if (input == null) {
            return result;
        }
        for (InterviewPackDto.Question question : input) {
            if (question == null || question.text() == null || question.text().isBlank()) {
                continue;
            }
            if (result.size() >= 20) {
                throw ApiException.badRequest("INVALID_PACK", "At most 20 questions are allowed.");
            }
            String id = question.id() == null || question.id().isBlank()
                    || !ids.add(question.id()) ? UUID.randomUUID().toString() : question.id();
            ids.add(id);
            result.add(new InterviewPackDto.Question(id,
                    question.text().strip().length() > 500
                            ? question.text().strip().substring(0, 500)
                            : question.text().strip()));
        }
        return result;
    }

    private List<InterviewPackDto.RubricItem> cleanRubric(List<InterviewPackDto.RubricItem> input) {
        List<InterviewPackDto.RubricItem> result = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        if (input == null) {
            return result;
        }
        for (InterviewPackDto.RubricItem item : input) {
            if (item == null || item.criterion() == null || item.criterion().isBlank()) {
                continue;
            }
            if (result.size() >= 10) {
                throw ApiException.badRequest("INVALID_PACK", "At most 10 rubric items are allowed.");
            }
            String id = item.id() == null || item.id().isBlank()
                    || !ids.add(item.id()) ? UUID.randomUUID().toString() : item.id();
            ids.add(id);
            String criterion = item.criterion().strip();
            String description = item.description() == null ? "" : item.description().strip();
            result.add(new InterviewPackDto.RubricItem(
                    id,
                    criterion.length() > 100 ? criterion.substring(0, 100) : criterion,
                    description.length() > 500 ? description.substring(0, 500) : description));
        }
        return result;
    }

    private InterviewPackDto toDto(InterviewPack pack, List<String> screeningQuestions,
                                   String provider) {
        List<InterviewPackDto.Question> questions = pack == null ? List.of()
                : pack.getQuestions().stream()
                .map(q -> new InterviewPackDto.Question(q.id(), q.text())).toList();
        List<InterviewPackDto.RubricItem> rubric = pack == null ? List.of()
                : pack.getRubric().stream()
                .map(r -> new InterviewPackDto.RubricItem(r.id(), r.criterion(), r.description()))
                .toList();
        return new InterviewPackDto(questions, rubric, screeningQuestions, provider);
    }

    private Job requireJob(UUID companyId, UUID jobId) {
        return jobRepository.findByIdAndCompanyId(jobId, companyId)
                .orElseThrow(() -> ApiException.notFound("JOB_NOT_FOUND", "Job not found."));
    }

    private List<String> screeningQuestions(UUID jobId) {
        return questionRepository.findByJobIdOrderByPositionAsc(jobId).stream()
                .map(JobScreeningQuestion::getText).toList();
    }

    private AiResult.JobText jobText(Job job) {
        List<String> skills = job.getSkills().stream()
                .filter(skill -> skill.getKind() == JobSkillKind.required)
                .map(JobSkill::getSkill)
                .map(skill -> skill.getName())
                .toList();
        return new AiResult.JobText(job.getTitle(), job.getDescription(),
                job.getRequirements(), skills);
    }
}
