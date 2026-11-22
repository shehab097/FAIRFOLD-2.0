package com.fairfold.resume;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface StripReportRepository extends JpaRepository<StripReport, UUID> {
}
