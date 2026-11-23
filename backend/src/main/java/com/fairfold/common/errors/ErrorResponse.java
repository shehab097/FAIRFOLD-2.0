package com.fairfold.common.errors;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String code,
        String message,
        List<FieldError> errors,
        @JsonInclude(JsonInclude.Include.NON_NULL) Instant retryAvailableAt
) {
    public ErrorResponse(Instant timestamp, int status, String error, String code,
                         String message, List<FieldError> errors) {
        this(timestamp, status, error, code, message, errors, null);
    }

    public record FieldError(String field, String message) {}
}
