package com.fairfold.application;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ApplicationRepository extends JpaRepository<Application, UUID> {

    boolean existsByJobIdAndCandidateId(UUID jobId, UUID candidateId);

    Optional<Application> findByIdAndCandidateId(UUID id, UUID candidateId);

    Optional<Application> findByJobIdAndCandidateId(UUID jobId, UUID candidateId);

    @Query("select a from Application a where a.candidateId = :candidateId and (:status is null or a.status = :status) order by a.createdAt desc")
    Page<Application> findByCandidate(@Param("candidateId") UUID candidateId,
                                      @Param("status") ApplicationStatus status,
                                      Pageable pageable);

    List<Application> findTop5ByCandidateIdOrderByCreatedAtDesc(UUID candidateId);

    List<Application> findByCandidateId(UUID candidateId);

    List<Application> findByJobIdOrderByCreatedAtAscIdAsc(UUID jobId);

    List<Application> findByJobIdAndStatusOrderByCreatedAtAscIdAsc(UUID jobId, ApplicationStatus status);

    @Query("select count(a) from Application a, Job j where a.jobId = j.id "
            + "and j.companyId = :companyId and a.createdAt >= :since")
    long countNewApplications(@Param("companyId") UUID companyId, @Param("since") Instant since);

    @Query("select count(a) from Application a, Job j where a.jobId = j.id "
            + "and j.companyId = :companyId and a.status = :status")
    long countByCompanyAndStatus(@Param("companyId") UUID companyId,
                                 @Param("status") ApplicationStatus status);

    @Query("select count(a) from Application a, Job j where a.jobId = j.id "
            + "and j.companyId = :companyId and a.status = :status and a.biasStatus = :bias")
    long countByCompanyStatusAndBias(@Param("companyId") UUID companyId,
                                     @Param("status") ApplicationStatus status,
                                     @Param("bias") com.fairfold.screening.BiasStatus bias);

    @Query("select a.status, count(a) from Application a, Job j "
            + "where a.jobId = j.id and j.companyId = :companyId group by a.status")
    List<Object[]> countByStatusForCompany(@Param("companyId") UUID companyId);

    @Query("select a from Application a, Job j where a.jobId = j.id "
            + "and j.companyId = :companyId order by a.createdAt desc, a.id desc")
    List<Application> findRecentForCompany(@Param("companyId") UUID companyId, Pageable pageable);

    long countByJobId(UUID jobId);

    long countByJobIdAndStatus(UUID jobId, ApplicationStatus status);
}
