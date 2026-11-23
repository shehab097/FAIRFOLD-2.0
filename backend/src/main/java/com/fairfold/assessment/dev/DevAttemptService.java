package com.fairfold.assessment.dev;

import com.fairfold.assessment.AssessmentAttempt;
import com.fairfold.assessment.AssessmentAttemptRepository;
import com.fairfold.assessment.AssessmentQuestion;
import com.fairfold.assessment.AssessmentQuestionRepository;
import com.fairfold.assessment.AttemptService;
import com.fairfold.assessment.AttemptStatus;
import com.fairfold.assessment.dto.AttemptView;
import com.fairfold.common.errors.ApiException;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Development-only helper: fill an in-progress attempt with a chosen number of
 * correct answers and submit it. Never active outside local/test profiles.
 */
@Service
@Profile({"local", "test"})
public class DevAttemptService {

    private final AssessmentAttemptRepository attemptRepository;
    private final AssessmentQuestionRepository questionRepository;
    private final AttemptService attemptService;

    public DevAttemptService(AssessmentAttemptRepository attemptRepository,
                             AssessmentQuestionRepository questionRepository,
                             AttemptService attemptService) {
        this.attemptRepository = attemptRepository;
        this.questionRepository = questionRepository;
        this.attemptService = attemptService;
    }

    @Transactional
    public AttemptView complete(UUID userId, UUID attemptId, Integer correctCount) {
        AssessmentAttempt attempt = attemptRepository.findWithLockById(attemptId)
                .orElseThrow(() -> ApiException.notFound("ATTEMPT_NOT_FOUND", "Attempt not found."));
        if (!attempt.getUserId().equals(userId)) {
            throw ApiException.notFound("ATTEMPT_NOT_FOUND", "Attempt not found.");
        }
        if (attempt.getStatus() != AttemptStatus.in_progress) {
            throw ApiException.conflict("ATTEMPT_CLOSED", "This attempt is already submitted.");
        }
        List<AssessmentQuestion> questions = questionRepository
                .findByAssessmentIdOrderByPositionAsc(attempt.getAssessmentId());
        if (!attempt.getQuestionIds().isEmpty()) {
            java.util.Map<String, AssessmentQuestion> byId = questions.stream()
                    .collect(java.util.stream.Collectors.toMap(
                            q -> q.getId().toString(), q -> q));
            questions = attempt.getQuestionIds().stream()
                    .map(byId::get).filter(java.util.Objects::nonNull).toList();
        }
        int correct = correctCount == null ? questions.size()
                : Math.max(0, Math.min(correctCount, questions.size()));
        Map<String, String> answers = new LinkedHashMap<>();
        for (int i = 0; i < questions.size(); i++) {
            AssessmentQuestion question = questions.get(i);
            String option;
            if (i < correct) {
                option = question.getCorrectOptionId();
            } else {
                option = question.getOptions().stream()
                        .map(o -> o.id())
                        .filter(id -> !id.equals(question.getCorrectOptionId()))
                        .findFirst()
                        .orElse(question.getCorrectOptionId());
            }
            answers.put(question.getId().toString(), option);
        }
        attempt.setAnswers(answers);
        attemptRepository.save(attempt);
        return attemptService.submit(userId, attemptId);
    }
}
