package com.fairfold.ai;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AiCallRepository extends JpaRepository<AiCall, UUID> {
}
