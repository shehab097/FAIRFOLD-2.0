package com.fairfold.resume;

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
@Table(name = "resumes")
public class Resume extends BaseEntity {

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "user_id", columnDefinition = "char(36)", nullable = false)
    private UUID userId;

    @Column(name = "original_filename", length = 255)
    private String originalFilename;

    @Column(name = "storage_key", nullable = false, length = 255)
    private String storageKey;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ResumeStatus status = ResumeStatus.uploaded;

    @Column(name = "failure_reason", columnDefinition = "text")
    private String failureReason;

    @Column(name = "stripped_text", columnDefinition = "text")
    private String strippedText;

    @Column(name = "is_current", nullable = false)
    private boolean current = true;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "current_user_key", columnDefinition = "char(36)")
    private UUID currentUserKey;

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public void setStorageKey(String storageKey) {
        this.storageKey = storageKey;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }

    public ResumeStatus getStatus() {
        return status;
    }

    public void setStatus(ResumeStatus status) {
        this.status = status;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public String getStrippedText() {
        return strippedText;
    }

    public void setStrippedText(String strippedText) {
        this.strippedText = strippedText;
    }

    public boolean isCurrent() {
        return current;
    }

    public void setCurrent(boolean current) {
        this.current = current;
    }

    public UUID getCurrentUserKey() {
        return currentUserKey;
    }

    public void setCurrentUserKey(UUID currentUserKey) {
        this.currentUserKey = currentUserKey;
    }
}
