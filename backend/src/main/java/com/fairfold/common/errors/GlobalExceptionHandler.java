package com.fairfold.common.errors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.Instant;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApi(ApiException ex) {
        HttpStatus status = ex.getStatus();
        var body = new ErrorResponse(
                Instant.now(), status.value(), status.getReasonPhrase(),
                ex.getCode(), ex.getMessage(), null, ex.getRetryAvailableAt());
        ResponseEntity.BodyBuilder builder = ResponseEntity.status(status);
        if (status == HttpStatus.TOO_MANY_REQUESTS && ex.getRetryAfterSeconds() > 0) {
            builder.header("Retry-After", String.valueOf(ex.getRetryAfterSeconds()));
        }
        return builder.body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        List<ErrorResponse.FieldError> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ErrorResponse.FieldError(fe.getField(), fe.getDefaultMessage()))
                .toList();
        var body = new ErrorResponse(Instant.now(), 400, "Bad Request", "VALIDATION",
                "Validation failed", errors);
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSize(MaxUploadSizeExceededException ex) {
        var body = new ErrorResponse(Instant.now(), 413, "Payload Too Large", "FILE_TOO_LARGE",
                "File is too large.", null);
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(body);
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            org.springframework.security.access.AccessDeniedException ex) {
        var body = new ErrorResponse(Instant.now(), 403, "Forbidden", "FORBIDDEN",
                "You do not have permission.", null);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    @ExceptionHandler(org.springframework.dao.ConcurrencyFailureException.class)
    public ResponseEntity<ErrorResponse> handleConcurrency(org.springframework.dao.ConcurrencyFailureException ex) {
        log.warn("Concurrency conflict: {}", ex.getMessage());
        var body = new ErrorResponse(Instant.now(), 409, "Conflict", "CONCURRENCY_CONFLICT",
                "A concurrent request was detected. Please retry.", null);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        log.error("Unhandled exception", ex);
        var body = new ErrorResponse(Instant.now(), 500, "Internal Server Error", "INTERNAL",
                "Something went wrong.", null);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
