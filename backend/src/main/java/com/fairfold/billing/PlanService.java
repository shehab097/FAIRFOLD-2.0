package com.fairfold.billing;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/** Effective plan lookup shared by billing and quota. */
@Service
public class PlanService {

    private final PlanRepository planRepository;
    private final CompanySubscriptionRepository subscriptionRepository;
    private final Clock clock;

    public PlanService(PlanRepository planRepository,
                       CompanySubscriptionRepository subscriptionRepository, Clock clock) {
        this.planRepository = planRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public CompanySubscription activeSubscription(UUID companyId) {
        Instant now = clock.instant();
        return subscriptionRepository
                .findFirstByCompanyIdAndStartsAtLessThanEqualAndEndsAtAfterOrderByEndsAtDesc(
                        companyId, now, now)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public Plan effectivePlan(UUID companyId) {
        CompanySubscription subscription = activeSubscription(companyId);
        if (subscription == null) {
            return freePlan();
        }
        return planRepository.findById(subscription.getPlanId()).orElseGet(this::freePlan);
    }

    public Plan freePlan() {
        return planRepository.findByName("Free")
                .orElseThrow(() -> new IllegalStateException("Free plan is missing"));
    }
}
