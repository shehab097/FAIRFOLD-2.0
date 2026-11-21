package com.fairfold.application;

import com.fairfold.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Entity
@Table(name = "applications")
public class Application extends BaseEntity {

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "job_id", columnDefinition = "char(36)", nullable = false)
    private UUID jobId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "candidate_id", columnDefinition = "char(36)", nullable = false)
    private UUID candidateId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "resume_id", columnDefinition = "char(36)", nullable = false)
    private UUID resumeId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ApplicationStatus status = ApplicationStatus.applied;

    public UUID getJobId() {
        return jobId;
    }

    public void setJobId(UUID jobId) {
        this.jobId = jobId;
    }

    public UUID getCandidateId() {
        return candidateId;
    }

    public void setCandidateId(UUID candidateId) {
        this.candidateId = candidateId;
    }

    public UUID getResumeId() {
        return resumeId;
    }

    public void setResumeId(UUID resumeId) {
        this.resumeId = resumeId;
    }

    public ApplicationStatus getStatus() {
        return status;
    }

    public void setStatus(ApplicationStatus status) {
        this.status = status;
    }
}
