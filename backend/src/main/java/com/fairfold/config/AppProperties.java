package com.fairfold.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app")
public record AppProperties(
        String storageDir,
        boolean cookieSecure,
        Auth auth
) {
    public record Auth(boolean requireEmailVerification) {}
}
