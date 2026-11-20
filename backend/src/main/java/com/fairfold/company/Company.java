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
