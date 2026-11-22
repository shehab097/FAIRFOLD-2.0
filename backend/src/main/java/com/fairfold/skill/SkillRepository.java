package com.fairfold.skill;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SkillRepository extends JpaRepository<Skill, UUID> {

    List<Skill> findTop10ByNameContainingIgnoreCaseOrderByNameAsc(String name);

    Optional<Skill> findByNameIgnoreCase(String name);
}
