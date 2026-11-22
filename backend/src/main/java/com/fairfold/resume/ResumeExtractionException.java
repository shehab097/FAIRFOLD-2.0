package com.fairfold.resume;

public class ResumeExtractionException extends RuntimeException {

    private final String code;

    public ResumeExtractionException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
