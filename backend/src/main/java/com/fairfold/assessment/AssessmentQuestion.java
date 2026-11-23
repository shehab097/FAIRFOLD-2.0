package com.fairfold.assessment;

import com.fairfold.assessment.dto.QuestionOption;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "assessment_questions")
public class AssessmentQuestion {

    @Id
    @GeneratedValue
    @UuidGenerator
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "id", columnDefinition = "char(36)", nullable = false, updatable = false)
    private UUID id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "assessment_id", columnDefinition = "char(36)", nullable = false)
    private UUID assessmentId;

    @Column(nullable = false)
    private int position;

    @Column(columnDefinition = "text", nullable = false)
    private String text;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json", nullable = false)
    private List<QuestionOption> options = new ArrayList<>();

    @Column(name = "correct_option_id", nullable = false, length = 20)
    private String correctOptionId;

    public UUID getId() {
        return id;
    }

    public UUID getAssessmentId() {
        return assessmentId;
    }

    public void setAssessmentId(UUID assessmentId) {
        this.assessmentId = assessmentId;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public List<QuestionOption> getOptions() {
        return options;
    }

    public void setOptions(List<QuestionOption> options) {
        this.options = options == null ? new ArrayList<>() : options;
    }

    public String getCorrectOptionId() {
        return correctOptionId;
    }

    public void setCorrectOptionId(String correctOptionId) {
        this.correctOptionId = correctOptionId;
    }
}
