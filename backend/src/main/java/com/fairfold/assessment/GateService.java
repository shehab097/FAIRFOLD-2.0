package com.fairfold.assessment;

import com.fairfold.application.Application;
import com.fairfold.application.ApplicationRepository;
import com.fairfold.screening.GateStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Applies the assessment gate to applications. No requirement means
 * {@code not_required}; all requirements met means {@code passed}; otherwise a
 * submitted attempt makes it {@code failed}; no attempt at all keeps it
 * {@code pending}.
 */
@Service
public class GateService {

    private final ApplicationRepository applicationRepository;
    private final JobRequiredAssessmentRepository requiredRepository;
    private final AssessmentAttemptRepository attemptRepository;

    public GateService(ApplicationRepository applicationRepository,
                       JobRequiredAssessmentRepository requiredRepository,
                       AssessmentAttemptRepository attemptRepository) {
        this.applicationRepository = applicationRepository;
        this.requiredRepository = requiredRepository;
        this.attemptRepository = attemptRepository;
    }

    @Transactional
    public void recompute(UUID applicationId) {
        applicationRepository.findById(applicationId).ifPresent(this::apply);
    }

    @Transactional
    public void recomputeForCandidateAssessment(UUID userId, UUID assessmentId) {
        for (Application application : applicationRepository.findByCandidateId(userId)) {
            if (requiredRepository.existsByJobIdAndAssessmentId(application.getJobId(), assessmentId)) {
                apply(application);
            }
        }
    }

    /** Used after a job is duplicated; a fresh draft normally has no applications. */
    @Transactional
    public void recomputeForJob(UUID jobId) {
        for (Application application : applicationRepository
                .findByJobIdOrderByCreatedAtAscIdAsc(jobId)) {
            apply(application);
        }
    }

    private void apply(Application application) {
        List<JobRequiredAssessment> required = requiredRepository.findByJobId(application.getJobId());
        GateStatus status = statusFor(application, required);
        if (application.getGateStatus() != status) {
            application.setGateStatus(status);
            applicationRepository.save(application);
        }
    }

    private GateStatus statusFor(Application application, List<JobRequiredAssessment> required) {
        if (required.isEmpty()) {
            return GateStatus.not_required;
        }
        Set<UUID> assessmentIds = required.stream()
                .map(JobRequiredAssessment::getAssessmentId)
                .collect(Collectors.toSet());
        List<AssessmentAttempt> attempts = attemptRepository
                .findByUserIdAndAssessmentIdInAndStatusNot(
                        application.getCandidateId(), assessmentIds, AttemptStatus.in_progress);
        boolean allMet = required.stream().allMatch(requirement -> attempts.stream()
                .anyMatch(attempt -> attempt.getAssessmentId().equals(requirement.getAssessmentId())
                        && attempt.getScore() != null
                        && attempt.getScore() >= requirement.getMinScore()));
        if (allMet) {
            return GateStatus.passed;
        }
        boolean anyAttempt = attempts.stream()
                .anyMatch(attempt -> assessmentIds.contains(attempt.getAssessmentId()));
        return anyAttempt ? GateStatus.failed : GateStatus.pending;
    }
}
