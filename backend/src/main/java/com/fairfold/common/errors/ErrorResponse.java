package com.fairfold.common.errors;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String code,
        String message,
        List<FieldError> errors,
        @JsonInclude(JsonInclude.Include.NON_NULL) Instant retryAvailableAt,
        @JsonInclude(JsonInclude.Include.NON_NULL) Map<String, Object> details
) {
    public ErrorResponse(Instant timestamp, int status, String error, String code,
                         String message, List<FieldError> errors) {
        this(timestamp, status, error, code, message, errors, null, null);
    }

    public ErrorResponse(Instant timestamp, int status, String error, String code,
                         String message, List<FieldError> errors, Instant retryAvailableAt) {
        this(timestamp, status, error, code, message, errors, retryAvailableAt, null);
    }

    public record FieldError(String field, String message) {}
}
