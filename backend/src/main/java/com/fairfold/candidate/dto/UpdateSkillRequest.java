package com.fairfold.candidate.dto;

import java.math.BigDecimal;

public record UpdateSkillRequest(String level, BigDecimal years) {
}
