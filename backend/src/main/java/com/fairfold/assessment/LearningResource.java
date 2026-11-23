package com.fairfold.assessment;

import com.fairfold.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "resources")
public class LearningResource extends BaseEntity {

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "skill_id", columnDefinition = "char(36)", nullable = false)
    private UUID skillId;

    @Column(nullable = false, columnDefinition = "text")
    private String title;

    @Column(nullable = false, length = 2048)
    private String url;

    @Column(nullable = false, length = 10)
    private String cost = "free";

    @Column(nullable = false, length = 50)
    private String language = "English";

    @Column(nullable = false, length = 20)
    private String level;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private boolean broken;

    @Column(name = "last_checked_at")
    private Instant lastCheckedAt;

    public UUID getSkillId() {
        return skillId;
    }

    public void setSkillId(UUID skillId) {
        this.skillId = skillId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getCost() {
        return cost;
    }

    public void setCost(String cost) {
        this.cost = cost;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isBroken() {
        return broken;
    }

    public void setBroken(boolean broken) {
        this.broken = broken;
    }

    public Instant getLastCheckedAt() {
        return lastCheckedAt;
    }

    public void setLastCheckedAt(Instant lastCheckedAt) {
        this.lastCheckedAt = lastCheckedAt;
    }
}
