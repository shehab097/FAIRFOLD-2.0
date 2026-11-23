package com.fairfold.auth.dto;

public record MfaChallengeResponse(boolean mfaRequired, String mfaToken) {
}
