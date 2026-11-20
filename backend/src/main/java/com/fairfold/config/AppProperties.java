package com.fairfold.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app")
public record AppProperties(
        String storageDir,
        String baseUrl,
        boolean cookieSecure,
        Auth auth,
        Jwt jwt,
        Mail mail,
        Admin admin
) {
    public record Auth(boolean requireEmailVerification) {}

    public record Jwt(String secret, long accessTokenMinutes) {}

    public record Mail(String mode) {}

    public record Admin(String email, String password) {}
}
