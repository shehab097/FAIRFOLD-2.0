package com.fairfold.job;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface JobSkillRepository extends JpaRepository<JobSkill, UUID> {

    List<JobSkill> findBySkillId(UUID skillId);

    boolean existsByJobIdAndSkillId(UUID jobId, UUID skillId);

    long countBySkillId(UUID skillId);
}
