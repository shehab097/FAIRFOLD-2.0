package com.fairfold.company;

import com.fairfold.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "companies")
public class Company extends BaseEntity {

    @Column(length = 255)
    private String name;

    @Column(length = 255)
    private String industry;

    @Column(length = 20)
    private String size;

    @Column(name = "show_name_publicly", nullable = false)
    private boolean showNamePublicly;

    @Column(length = 160)
    private String tagline;

    @Column(columnDefinition = "text")
    private String about;

    @Column(length = 2048)
    private String website;

    @Column(name = "logo_url", length = 2048)
    private String logoUrl;

    @Column(name = "linkedin_url", length = 2048)
    private String linkedinUrl;

    @Column(name = "founded_year")
    private Integer foundedYear;

    @Column(length = 255)
    private String headquarters;

    @Column(name = "contact_email", length = 254)
    private String contactEmail;

    @Column(length = 40)
    private String phone;

    @Column(columnDefinition = "text")
    private String benefits;

    public String getTagline() { return tagline; }
    public void setTagline(String tagline) { this.tagline = tagline; }
    public String getAbout() { return about; }
    public void setAbout(String about) { this.about = about; }
    public String getWebsite() { return website; }
    public void setWebsite(String website) { this.website = website; }
    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }
    public String getLinkedinUrl() { return linkedinUrl; }
    public void setLinkedinUrl(String linkedinUrl) { this.linkedinUrl = linkedinUrl; }
    public Integer getFoundedYear() { return foundedYear; }
    public void setFoundedYear(Integer foundedYear) { this.foundedYear = foundedYear; }
    public String getHeadquarters() { return headquarters; }
    public void setHeadquarters(String headquarters) { this.headquarters = headquarters; }
    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getBenefits() { return benefits; }
    public void setBenefits(String benefits) { this.benefits = benefits; }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIndustry() {
        return industry;
    }

    public void setIndustry(String industry) {
        this.industry = industry;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public boolean isShowNamePublicly() {
        return showNamePublicly;
    }

    public void setShowNamePublicly(boolean showNamePublicly) {
        this.showNamePublicly = showNamePublicly;
    }
}
