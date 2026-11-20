package com.fairfold.common.errors;

import org.springframework.http.HttpStatus;

/**
 * Application exception carrying an HTTP status, a machine-readable code and
 * a human message. Thrown by services and translated by
 * {@link GlobalExceptionHandler} into the standard error body.
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;
    private final int retryAfterSeconds;

    public ApiException(HttpStatus status, String code, String message) {
        this(status, code, message, 0);
    }

    public ApiException(HttpStatus status, String code, String message, int retryAfterSeconds) {
        super(message);
        this.status = status;
        this.code = code;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public int getRetryAfterSeconds() {
        return retryAfterSeconds;
    }

    public static ApiException badRequest(String code, String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, code, message);
    }

    public static ApiException unauthorized(String code, String message) {
        return new ApiException(HttpStatus.UNAUTHORIZED, code, message);
    }

    public static ApiException forbidden(String message) {
        return new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", message);
    }

    public static ApiException notFound(String code, String message) {
        return new ApiException(HttpStatus.NOT_FOUND, code, message);
    }

    public static ApiException conflict(String code, String message) {
        return new ApiException(HttpStatus.CONFLICT, code, message);
    }

    public static ApiException locked(long minutesLeft) {
        return new ApiException(HttpStatus.LOCKED, "ACCOUNT_LOCKED",
                "Account locked. Try again in " + minutesLeft + " minutes.");
    }

    public static ApiException tooManyRequests(int retryAfterSeconds) {
        return new ApiException(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMITED",
                "Too many requests. Try again later.", retryAfterSeconds);
    }
}
