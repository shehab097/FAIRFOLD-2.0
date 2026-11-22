package com.fairfold.screening;

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
@Table(name = "screening_items")
public class ScreeningItem extends BaseEntity {

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "run_id", columnDefinition = "char(36)", nullable = false)
    private UUID runId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "application_id", columnDefinition = "char(36)", nullable = false)
    private UUID applicationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ItemStatus status = ItemStatus.queued;

    @Column(columnDefinition = "text")
    private String error;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "skip_filters", nullable = false)
    private boolean skipFilters;

    public UUID getRunId() {
        return runId;
    }

    public void setRunId(UUID runId) {
        this.runId = runId;
    }

    public UUID getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(UUID applicationId) {
        this.applicationId = applicationId;
    }

    public ItemStatus getStatus() {
        return status;
    }

    public void setStatus(ItemStatus status) {
        this.status = status;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public int getAttempts() {
        return attempts;
    }

    public void setAttempts(int attempts) {
        this.attempts = attempts;
    }

    public boolean isSkipFilters() {
        return skipFilters;
    }

    public void setSkipFilters(boolean skipFilters) {
        this.skipFilters = skipFilters;
    }
}
