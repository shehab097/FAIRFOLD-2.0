package com.fairfold.billing.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record BillingResponse(
        CurrentPlan plan,
        long used,
        int quota,
        List<PlanView> plans
) {
    public record CurrentPlan(String name, int monthlyScreenings, java.time.Instant endsAt) {}

    public record PlanView(UUID id, String name, int monthlyScreenings,
                           BigDecimal priceBdt, Integer durationDays) {}
}
