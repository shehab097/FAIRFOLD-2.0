package com.fairfold.assessment;

import com.fairfold.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Entity
@Table(name = "assessments")
public class Assessment extends BaseEntity {

    @Column(nullable = false, columnDefinition = "text")
    private String title;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "skill_id", columnDefinition = "char(36)", nullable = false)
    private UUID skillId;

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes;

    @Column(name = "pass_mark", nullable = false)
    private int passMark;

    @Column(nullable = false)
    private boolean active = true;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public UUID getSkillId() {
        return skillId;
    }

    public void setSkillId(UUID skillId) {
        this.skillId = skillId;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(int durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public int getPassMark() {
        return passMark;
    }

    public void setPassMark(int passMark) {
        this.passMark = passMark;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
