package com.fairfold.billing;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByProviderPaymentId(String providerPaymentId);

    Optional<Payment> findByIdAndCompanyId(UUID id, UUID companyId);

    Page<Payment> findByCompanyIdOrderByCreatedAtDesc(UUID companyId, Pageable pageable);
}
