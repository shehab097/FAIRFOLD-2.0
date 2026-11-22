package com.fairfold.screening;

import com.fairfold.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "screening_runs")
public class ScreeningRun extends BaseEntity {

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "job_id", columnDefinition = "char(36)", nullable = false)
    private UUID jobId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "started_by", columnDefinition = "char(36)")
    private UUID startedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ScreeningMode mode;

    @Column(nullable = false)
    private int total;

    @Column(name = "done_count", nullable = false)
    private int doneCount;

    @Column(name = "failed_count", nullable = false)
    private int failedCount;

    @Column(name = "estimated_cost", nullable = false, precision = 10, scale = 4)
    private BigDecimal estimatedCost = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RunStatus status = RunStatus.running;

    @Column(name = "finished_at")
    private Instant finishedAt;

    public UUID getJobId() {
        return jobId;
    }

    public void setJobId(UUID jobId) {
        this.jobId = jobId;
    }

    public UUID getStartedBy() {
        return startedBy;
    }

    public void setStartedBy(UUID startedBy) {
        this.startedBy = startedBy;
    }

    public ScreeningMode getMode() {
        return mode;
    }

    public void setMode(ScreeningMode mode) {
        this.mode = mode;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public int getDoneCount() {
        return doneCount;
    }

    public void setDoneCount(int doneCount) {
        this.doneCount = doneCount;
    }

    public int getFailedCount() {
        return failedCount;
    }

    public void setFailedCount(int failedCount) {
        this.failedCount = failedCount;
    }

    public BigDecimal getEstimatedCost() {
        return estimatedCost;
    }

    public void setEstimatedCost(BigDecimal estimatedCost) {
        this.estimatedCost = estimatedCost == null ? BigDecimal.ZERO : estimatedCost;
    }

    public RunStatus getStatus() {
        return status;
    }

    public void setStatus(RunStatus status) {
        this.status = status;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(Instant finishedAt) {
        this.finishedAt = finishedAt;
    }
}
