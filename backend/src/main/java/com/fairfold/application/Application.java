package com.fairfold.application;

import com.fairfold.common.BaseEntity;
import com.fairfold.screening.BiasStatus;
import com.fairfold.screening.GateStatus;
import com.fairfold.screening.OverrideDirection;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
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

    @Column
    private Integer score;

    @Enumerated(EnumType.STRING)
    @Column(name = "scoring_mode", length = 20)
    private com.fairfold.screening.ScreeningMode scoringMode;

    @Enumerated(EnumType.STRING)
    @Column(name = "bias_status", length = 20)
    private BiasStatus biasStatus;

    @Column(name = "bias_note", columnDefinition = "text")
    private String biasNote;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "missing_skills", columnDefinition = "json", nullable = false)
    private List<String> missingSkills = new ArrayList<>();

    @Column(name = "not_matched_rule", columnDefinition = "text")
    private String notMatchedRule;

    @Column(name = "pulled_back_at")
    private Instant pulledBackAt;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "pulled_back_by", columnDefinition = "char(36)")
    private UUID pulledBackBy;

    @Column(name = "shortlisted_at")
    private Instant shortlistedAt;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "decided_by", columnDefinition = "char(36)")
    private UUID decidedBy;

    @Column(name = "decision_reason", columnDefinition = "text")
    private String decisionReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "override_direction", nullable = false, length = 30)
    private OverrideDirection overrideDirection = OverrideDirection.none;

    @Enumerated(EnumType.STRING)
    @Column(name = "gate_status", nullable = false, length = 20)
    private GateStatus gateStatus = GateStatus.not_required;

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

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public com.fairfold.screening.ScreeningMode getScoringMode() {
        return scoringMode;
    }

    public void setScoringMode(com.fairfold.screening.ScreeningMode scoringMode) {
        this.scoringMode = scoringMode;
    }

    public BiasStatus getBiasStatus() {
        return biasStatus;
    }

    public void setBiasStatus(BiasStatus biasStatus) {
        this.biasStatus = biasStatus;
    }

    public String getBiasNote() {
        return biasNote;
    }

    public void setBiasNote(String biasNote) {
        this.biasNote = biasNote;
    }

    public List<String> getMissingSkills() {
        return missingSkills;
    }

    public void setMissingSkills(List<String> missingSkills) {
        this.missingSkills = missingSkills == null ? new ArrayList<>() : missingSkills;
    }

    public String getNotMatchedRule() {
        return notMatchedRule;
    }

    public void setNotMatchedRule(String notMatchedRule) {
        this.notMatchedRule = notMatchedRule;
    }

    public Instant getPulledBackAt() {
        return pulledBackAt;
    }

    public void setPulledBackAt(Instant pulledBackAt) {
        this.pulledBackAt = pulledBackAt;
    }

    public UUID getPulledBackBy() {
        return pulledBackBy;
    }

    public void setPulledBackBy(UUID pulledBackBy) {
        this.pulledBackBy = pulledBackBy;
    }

    public Instant getShortlistedAt() {
        return shortlistedAt;
    }

    public void setShortlistedAt(Instant shortlistedAt) {
        this.shortlistedAt = shortlistedAt;
    }

    public UUID getDecidedBy() {
        return decidedBy;
    }

    public void setDecidedBy(UUID decidedBy) {
        this.decidedBy = decidedBy;
    }

    public String getDecisionReason() {
        return decisionReason;
    }

    public void setDecisionReason(String decisionReason) {
        this.decisionReason = decisionReason;
    }

    public OverrideDirection getOverrideDirection() {
        return overrideDirection;
    }

    public void setOverrideDirection(OverrideDirection overrideDirection) {
        this.overrideDirection = overrideDirection == null ? OverrideDirection.none : overrideDirection;
    }

    public GateStatus getGateStatus() {
        return gateStatus;
    }

    public void setGateStatus(GateStatus gateStatus) {
        this.gateStatus = gateStatus == null ? GateStatus.not_required : gateStatus;
    }
}
