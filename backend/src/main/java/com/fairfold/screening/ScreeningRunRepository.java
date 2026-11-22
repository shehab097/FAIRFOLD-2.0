package com.fairfold.screening;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ScreeningRunRepository extends JpaRepository<ScreeningRun, UUID> {

    Optional<ScreeningRun> findFirstByJobIdOrderByCreatedAtDesc(UUID jobId);

    boolean existsByJobIdAndStatus(UUID jobId, RunStatus status);

    List<ScreeningRun> findByStatus(RunStatus status);

    @Transactional
    @Modifying
    @Query("update ScreeningRun r set r.doneCount = r.doneCount + 1 where r.id = :id")
    void incrementDone(@Param("id") UUID id);

    @Transactional
    @Modifying
    @Query("update ScreeningRun r set r.failedCount = r.failedCount + 1 where r.id = :id")
    void incrementFailed(@Param("id") UUID id);

    @Transactional
    @Modifying
    @Query("update ScreeningRun r set r.status = :status, r.finishedAt = :finishedAt where r.id = :id")
    void finish(@Param("id") UUID id, @Param("status") RunStatus status,
                @Param("finishedAt") Instant finishedAt);
}
