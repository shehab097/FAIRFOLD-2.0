package com.fairfold.interview;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "interview_feedback")
public class InterviewFeedback {

    @Id
    @GeneratedValue
    @UuidGenerator
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "id", columnDefinition = "char(36)", nullable = false, updatable = false)
    private UUID id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "interview_id", columnDefinition = "char(36)", nullable = false)
    private UUID interviewId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "interviewer_id", columnDefinition = "char(36)")
    private UUID interviewerId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json", nullable = false)
    private Map<String, Integer> scores = new LinkedHashMap<>();

    @Column(columnDefinition = "text")
    private String comments;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FeedbackRecommendation recommendation;

    public UUID getId() {
        return id;
    }

    public UUID getInterviewId() {
        return interviewId;
    }

    public void setInterviewId(UUID interviewId) {
        this.interviewId = interviewId;
    }

    public UUID getInterviewerId() {
        return interviewerId;
    }

    public void setInterviewerId(UUID interviewerId) {
        this.interviewerId = interviewerId;
    }

    public Map<String, Integer> getScores() {
        return scores;
    }

    public void setScores(Map<String, Integer> scores) {
        this.scores = scores == null ? new LinkedHashMap<>() : scores;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public FeedbackRecommendation getRecommendation() {
        return recommendation;
    }

    public void setRecommendation(FeedbackRecommendation recommendation) {
        this.recommendation = recommendation;
    }
}
