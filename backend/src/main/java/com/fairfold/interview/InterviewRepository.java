package com.fairfold.interview;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface InterviewRepository extends JpaRepository<Interview, UUID> {

    List<Interview> findByApplicationIdOrderByScheduledAtAsc(UUID applicationId);

    @Query("select i from Interview i, com.fairfold.application.Application a, com.fairfold.job.Job j "
            + "where i.applicationId = a.id and a.jobId = j.id and j.companyId = :companyId "
            + "and i.scheduledAt >= :now order by i.scheduledAt asc")
    Page<Interview> findUpcomingForCompany(@Param("companyId") UUID companyId,
                                           @Param("now") Instant now, Pageable pageable);

    @Query("select i from Interview i, com.fairfold.application.Application a, com.fairfold.job.Job j "
            + "where i.applicationId = a.id and a.jobId = j.id and j.companyId = :companyId "
            + "and i.scheduledAt < :now order by i.scheduledAt desc")
    Page<Interview> findPastForCompany(@Param("companyId") UUID companyId,
                                       @Param("now") Instant now, Pageable pageable);

    @Query("select i from Interview i where i.id in "
            + "(select ii.id.interviewId from InterviewInterviewer ii where ii.id.userId = :userId) "
            + "and i.scheduledAt >= :now order by i.scheduledAt asc")
    Page<Interview> findUpcomingForInterviewer(@Param("userId") UUID userId,
                                               @Param("now") Instant now, Pageable pageable);

    @Query("select i from Interview i where i.id in "
            + "(select ii.id.interviewId from InterviewInterviewer ii where ii.id.userId = :userId) "
            + "and i.scheduledAt < :now order by i.scheduledAt desc")
    Page<Interview> findPastForInterviewer(@Param("userId") UUID userId,
                                           @Param("now") Instant now, Pageable pageable);

    @Query("select count(i) from Interview i, com.fairfold.application.Application a, com.fairfold.job.Job j "
            + "where i.applicationId = a.id and a.jobId = j.id and j.companyId = :companyId "
            + "and i.scheduledAt >= :from and i.scheduledAt < :to")
    long countForCompanyBetween(@Param("companyId") UUID companyId,
                                @Param("from") Instant from, @Param("to") Instant to);
}
