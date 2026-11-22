package com.fairfold.screening;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CandidateAliasRepository extends JpaRepository<CandidateAlias, UUID> {

    Optional<CandidateAlias> findByCompanyIdAndCandidateId(UUID companyId, UUID candidateId);

    boolean existsByCompanyIdAndAlias(UUID companyId, String alias);
}
