package com.fairfold.assessment;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface LearningResourceRepository extends JpaRepository<LearningResource, UUID> {

    Page<LearningResource> findBySkillId(UUID skillId, Pageable pageable);
}
