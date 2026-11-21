package com.fairfold.application;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ApplicationRepository extends JpaRepository<Application, UUID> {

    boolean existsByJobIdAndCandidateId(UUID jobId, UUID candidateId);

    Optional<Application> findByIdAndCandidateId(UUID id, UUID candidateId);

    @Query("select a from Application a where a.candidateId = :candidateId and (:status is null or a.status = :status) order by a.createdAt desc")
    Page<Application> findByCandidate(@Param("candidateId") UUID candidateId,
                                      @Param("status") ApplicationStatus status,
                                      Pageable pageable);

    List<Application> findTop5ByCandidateIdOrderByCreatedAtDesc(UUID candidateId);

    List<Application> findByJobIdOrderByCreatedAtAscIdAsc(UUID jobId);
}
