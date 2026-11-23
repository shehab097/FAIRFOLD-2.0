package com.fairfold.billing;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface CompanySubscriptionRepository extends JpaRepository<CompanySubscription, UUID> {

    Optional<CompanySubscription> findFirstByCompanyIdAndStartsAtLessThanEqualAndEndsAtAfterOrderByEndsAtDesc(
            UUID companyId, Instant startsAt, Instant endsAt);

    @Modifying
    @Query("update CompanySubscription s set s.endsAt = :now "
            + "where s.companyId = :companyId and s.startsAt <= :now and s.endsAt > :now")
    int endActive(@Param("companyId") UUID companyId, @Param("now") Instant now);
}
