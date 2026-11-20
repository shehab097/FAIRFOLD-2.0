package com.fairfold.common.errors;

import java.time.Instant;
import java.util.List;

public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String code,
        String message,
        List<FieldError> errors
) {
    public record FieldError(String field, String message) {}
}
