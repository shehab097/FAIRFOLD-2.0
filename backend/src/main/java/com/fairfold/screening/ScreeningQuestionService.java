package com.fairfold.screening;

import com.fairfold.ai.AiGateway;
import com.fairfold.ai.AiResult;
import com.fairfold.common.errors.ApiException;
import com.fairfold.job.Job;
import com.fairfold.job.JobRepository;
import com.fairfold.job.JobScreeningQuestion;
import com.fairfold.job.JobScreeningQuestionRepository;
import com.fairfold.job.JobSkill;
import com.fairfold.job.JobSkillKind;
import com.fairfold.job.JobStatus;
import com.fairfold.screening.dto.ScreeningQuestionsRequest;
import com.fairfold.screening.dto.ScreeningQuestionsResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ScreeningQuestionService {

    private static final int MAX_QUESTIONS = 12;
    private static final int MAX_LENGTH = 500;

    private final JobRepository jobRepository;
    private final JobScreeningQuestionRepository questionRepository;
    private final AiGateway aiGateway;

    public ScreeningQuestionService(JobRepository jobRepository,
                                    JobScreeningQuestionRepository questionRepository,
                                    AiGateway aiGateway) {
        this.jobRepository = jobRepository;
        this.questionRepository = questionRepository;
        this.aiGateway = aiGateway;
    }

    @Transactional
    public ScreeningQuestionsResponse suggest(UUID companyId, UUID userId, UUID jobId) {
        Job job = requireJob(companyId, jobId);
        List<String> skills = new ArrayList<>();
        for (JobSkill js : job.getSkills()) {
            if (js.getKind() == JobSkillKind.required) {
                skills.add(js.getSkill().getName());
            }
        }
        AiResult<List<String>> result = aiGateway.suggestQuestions(userId,
                new AiResult.JobText(job.getTitle(), job.getDescription(),
                        job.getRequirements(), skills));
        List<String> questions = result.value().stream()
                .filter(q -> q != null && !q.isBlank())
                .map(String::strip)
                .limit(MAX_QUESTIONS)
                .toList();
        return new ScreeningQuestionsResponse(questions);
    }

    @Transactional
    public ScreeningQuestionsResponse save(UUID companyId, UUID jobId, ScreeningQuestionsRequest request) {
        Job job = requireJob(companyId, jobId);
        if (job.getStatus() != JobStatus.draft) {
            throw ApiException.conflict("JOB_NOT_DRAFT", "Screening questions can only be edited on a draft job.");
        }
        List<String> questions = new ArrayList<>();
        if (request != null && request.questions() != null) {
            for (String question : request.questions()) {
                if (question == null || question.isBlank()) {
                    continue;
                }
                String clean = question.strip();
                if (clean.length() > MAX_LENGTH) {
                    throw ApiException.badRequest("QUESTION_TOO_LONG",
                            "Each screening question must be at most 500 characters.");
                }
                if (questions.size() >= MAX_QUESTIONS) {
                    throw ApiException.badRequest("TOO_MANY_QUESTIONS",
                            "At most 12 screening questions are allowed.");
                }
                questions.add(clean);
            }
        }
        questionRepository.deleteByJobId(jobId);
        for (int i = 0; i < questions.size(); i++) {
            JobScreeningQuestion row = new JobScreeningQuestion();
            row.setJobId(jobId);
            row.setPosition(i);
            row.setText(questions.get(i));
            questionRepository.save(row);
        }
        return new ScreeningQuestionsResponse(questions);
    }

    private Job requireJob(UUID companyId, UUID jobId) {
        return jobRepository.findByIdAndCompanyId(jobId, companyId)
                .orElseThrow(() -> ApiException.notFound("JOB_NOT_FOUND", "Job not found."));
    }
}
