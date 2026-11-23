package com.fairfold.billing.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentItem(
        UUID id,
        String planName,
        BigDecimal amountBdt,
        String status,
        Instant createdAt
) {
}
