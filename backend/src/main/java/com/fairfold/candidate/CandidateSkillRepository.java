package com.fairfold.candidate;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CandidateSkillRepository extends JpaRepository<CandidateSkill, UUID> {

    List<CandidateSkill> findByUserIdOrderByCreatedAtAsc(UUID userId);

    Optional<CandidateSkill> findByIdAndUserId(UUID id, UUID userId);

    boolean existsByUserIdAndSkillId(UUID userId, UUID skillId);

    long countByUserId(UUID userId);
}
