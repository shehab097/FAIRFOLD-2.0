package com.fairfold.assessment;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AssessmentAttemptRepository extends JpaRepository<AssessmentAttempt, UUID> {

    long countByAssessmentId(UUID assessmentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from AssessmentAttempt a where a.id = :id")
    Optional<AssessmentAttempt> findWithLockById(@Param("id") UUID id);

    Optional<AssessmentAttempt> findFirstByUserIdAndAssessmentIdAndStatusOrderByStartedAtDesc(
            UUID userId, UUID assessmentId, AttemptStatus status);

    Optional<AssessmentAttempt> findFirstByUserIdAndAssessmentIdAndStatusNotOrderBySubmittedAtDesc(
            UUID userId, UUID assessmentId, AttemptStatus status);

    List<AssessmentAttempt> findByUserIdAndAssessmentIdOrderByStartedAtDesc(UUID userId, UUID assessmentId);

    Page<AssessmentAttempt> findByUserIdAndStatusNotOrderBySubmittedAtDesc(
            UUID userId, AttemptStatus status, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<AssessmentAttempt> findByStatusAndEndsAtBefore(AttemptStatus status, Instant cutoff);

    long countByUserIdAndAssessmentId(UUID userId, UUID assessmentId);
}
