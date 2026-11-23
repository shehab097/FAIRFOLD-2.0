package com.fairfold.interview;

import com.fairfold.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "interview_packs")
public class InterviewPack extends BaseEntity {

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "job_id", columnDefinition = "char(36)", nullable = false)
    private UUID jobId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json", nullable = false)
    private List<InterviewPackQuestion> questions = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json", nullable = false)
    private List<InterviewPackRubric> rubric = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "updated_by", columnDefinition = "char(36)")
    private UUID updatedBy;

    public UUID getJobId() {
        return jobId;
    }

    public void setJobId(UUID jobId) {
        this.jobId = jobId;
    }

    public List<InterviewPackQuestion> getQuestions() {
        return questions;
    }

    public void setQuestions(List<InterviewPackQuestion> questions) {
        this.questions = questions == null ? new ArrayList<>() : questions;
    }

    public List<InterviewPackRubric> getRubric() {
        return rubric;
    }

    public void setRubric(List<InterviewPackRubric> rubric) {
        this.rubric = rubric == null ? new ArrayList<>() : rubric;
    }

    public UUID getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(UUID updatedBy) {
        this.updatedBy = updatedBy;
    }
}
