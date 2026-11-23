package com.fairfold.interview;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "interview_interviewers")
public class InterviewInterviewer {

    @EmbeddedId
    private InterviewInterviewerId id;

    public InterviewInterviewer() {
    }

    public InterviewInterviewer(InterviewInterviewerId id) {
        this.id = id;
    }

    public InterviewInterviewerId getId() {
        return id;
    }

    public void setId(InterviewInterviewerId id) {
        this.id = id;
    }
}
