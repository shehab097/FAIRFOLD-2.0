package com.fairfold.skill;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SkillAliasRepository extends JpaRepository<SkillAlias, UUID> {

    Optional<SkillAlias> findByAliasIgnoreCase(String alias);

    List<SkillAlias> findTop10ByAliasContainingIgnoreCaseOrderByAliasAsc(String alias);
}
