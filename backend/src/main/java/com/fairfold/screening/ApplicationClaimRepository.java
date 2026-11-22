package com.fairfold.screening;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

public interface ApplicationClaimRepository extends JpaRepository<ApplicationClaim, UUID> {

    List<ApplicationClaim> findByApplicationIdOrderByIdAsc(UUID applicationId);

    @Transactional
    void deleteByApplicationId(UUID applicationId);
}
