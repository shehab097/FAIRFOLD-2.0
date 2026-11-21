package com.fairfold.job;

import com.fairfold.common.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "jobs")
public class Job extends BaseEntity {

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "company_id", columnDefinition = "char(36)", nullable = false)
    private UUID companyId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "created_by", columnDefinition = "char(36)")
    private UUID createdBy;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String description;

    @Column(nullable = false, columnDefinition = "text")
    private String requirements = "";

    @Enumerated(EnumType.STRING)
    @Column(name = "experience_level", nullable = false, length = 20)
    private ExperienceLevel experienceLevel;

    @Column(length = 255)
    private String city;

    @Column(length = 255)
    private String address;

    @Column(nullable = false, length = 255)
    private String country;

    @Column(nullable = false)
    private boolean remote;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private JobStatus status = JobStatus.draft;

    @Column(name = "hf_min_years")
    private Integer hfMinYears;

    @Column(name = "hf_country", length = 255)
    private String hfCountry;

    @Column(name = "cutoff_score")
    private Integer cutoffScore;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @OneToMany(mappedBy = "job", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<JobSkill> skills = new ArrayList<>();

    public UUID getCompanyId() {
        return companyId;
    }

    public void setCompanyId(UUID companyId) {
        this.companyId = companyId;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getRequirements() {
        return requirements;
    }

    public void setRequirements(String requirements) {
        this.requirements = requirements;
    }

    public ExperienceLevel getExperienceLevel() {
        return experienceLevel;
    }

    public void setExperienceLevel(ExperienceLevel experienceLevel) {
        this.experienceLevel = experienceLevel;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public boolean isRemote() {
        return remote;
    }

    public void setRemote(boolean remote) {
        this.remote = remote;
    }

    public JobStatus getStatus() {
        return status;
    }

    public void setStatus(JobStatus status) {
        this.status = status;
    }

    public Integer getHfMinYears() {
        return hfMinYears;
    }

    public void setHfMinYears(Integer hfMinYears) {
        this.hfMinYears = hfMinYears;
    }

    public String getHfCountry() {
        return hfCountry;
    }

    public void setHfCountry(String hfCountry) {
        this.hfCountry = hfCountry;
    }

    public Integer getCutoffScore() {
        return cutoffScore;
    }

    public void setCutoffScore(Integer cutoffScore) {
        this.cutoffScore = cutoffScore;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(Instant publishedAt) {
        this.publishedAt = publishedAt;
    }

    public Instant getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(Instant closedAt) {
        this.closedAt = closedAt;
    }

    public List<JobSkill> getSkills() {
        return skills;
    }

    public void setSkills(List<JobSkill> skills) {
        this.skills = skills;
    }
}
