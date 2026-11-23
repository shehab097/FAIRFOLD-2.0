package com.fairfold.team;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TeamInviteRepository extends JpaRepository<TeamInvite, UUID> {

    List<TeamInvite> findByCompanyIdAndAcceptedAtIsNullOrderByEmailAsc(UUID companyId);

    Optional<TeamInvite> findByIdAndCompanyId(UUID id, UUID companyId);

    List<TeamInvite> findByCompanyIdAndEmailAndAcceptedAtIsNull(UUID companyId, String email);

    Optional<TeamInvite> findByTokenHash(String tokenHash);

    Optional<TeamInvite> findByTokenHashAndAcceptedAtIsNullAndExpiresAtAfter(String tokenHash, Instant now);
}
