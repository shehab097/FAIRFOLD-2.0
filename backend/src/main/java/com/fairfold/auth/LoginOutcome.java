package com.fairfold.auth;

import com.fairfold.auth.dto.UserDto;

/** Successful login, or the MFA challenge when two-step login is enabled. */
public record LoginOutcome(AuthSession session, String mfaToken) {

    public boolean mfaRequired() {
        return mfaToken != null;
    }
}
