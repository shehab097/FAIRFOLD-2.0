package com.fairfold.screening.dto;

import java.math.BigDecimal;

public record ScreeningEstimateResponse(
        int count,
        String mode,
        BigDecimal estimatedCostUsd
) {
}
