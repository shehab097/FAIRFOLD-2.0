package com.fairfold.screening;

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

import java.util.UUID;

@Entity
@Table(name = "application_claims")
public class ApplicationClaim {

    @Id
    @GeneratedValue
    @UuidGenerator
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "id", columnDefinition = "char(36)", nullable = false, updatable = false)
    private UUID id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "application_id", columnDefinition = "char(36)", nullable = false)
    private UUID applicationId;

    @Column(name = "requirement_key", nullable = false, length = 100)
    private String requirementKey;

    @Column(name = "requirement_text", columnDefinition = "text", nullable = false)
    private String requirementText;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ClaimKind kind;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Verdict verdict;

    @Column(columnDefinition = "text")
    private String quote;

    @Column(nullable = false)
    private int weight;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ClaimSource source;

    public UUID getId() {
        return id;
    }

    public UUID getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(UUID applicationId) {
        this.applicationId = applicationId;
    }

    public String getRequirementKey() {
        return requirementKey;
    }

    public void setRequirementKey(String requirementKey) {
        this.requirementKey = requirementKey;
    }

    public String getRequirementText() {
        return requirementText;
    }

    public void setRequirementText(String requirementText) {
        this.requirementText = requirementText;
    }

    public ClaimKind getKind() {
        return kind;
    }

    public void setKind(ClaimKind kind) {
        this.kind = kind;
    }

    public Verdict getVerdict() {
        return verdict;
    }

    public void setVerdict(Verdict verdict) {
        this.verdict = verdict;
    }

    public String getQuote() {
        return quote;
    }

    public void setQuote(String quote) {
        this.quote = quote;
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    public ClaimSource getSource() {
        return source;
    }

    public void setSource(ClaimSource source) {
        this.source = source;
    }
}
