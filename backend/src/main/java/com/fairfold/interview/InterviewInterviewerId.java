package com.fairfold.interview;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class InterviewInterviewerId implements Serializable {

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "interview_id", columnDefinition = "char(36)", nullable = false)
    private UUID interviewId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "user_id", columnDefinition = "char(36)", nullable = false)
    private UUID userId;

    public InterviewInterviewerId() {
    }

    public InterviewInterviewerId(UUID interviewId, UUID userId) {
        this.interviewId = interviewId;
        this.userId = userId;
    }

    public UUID getInterviewId() {
        return interviewId;
    }

    public void setInterviewId(UUID interviewId) {
        this.interviewId = interviewId;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof InterviewInterviewerId that)) return false;
        return Objects.equals(interviewId, that.interviewId) && Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(interviewId, userId);
    }
}
