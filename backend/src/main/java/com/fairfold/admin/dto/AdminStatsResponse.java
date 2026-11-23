package com.fairfold.admin.dto;

import java.math.BigDecimal;
import java.util.Map;

public record AdminStatsResponse(
        Map<String, Long> usersByRole,
        Map<String, Long> jobsByStatus,
        long applicationsTotal,
        long aiCallsThisMonth,
        BigDecimal aiCostThisMonthUsd
) {
}
