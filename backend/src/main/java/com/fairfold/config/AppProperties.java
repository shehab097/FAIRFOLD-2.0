package com.fairfold.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

@ConfigurationProperties("app")
public record AppProperties(
        String storageDir,
        String baseUrl,
        boolean cookieSecure,
        Auth auth,
        Jwt jwt,
        Mail mail,
        Admin admin,
        Ai ai,
        Resources resources,
        Assessment assessment
) {
    public record Auth(boolean requireEmailVerification) {}

    public record Resources(String linkCheckCron) {}

    public record Assessment(int cooldownHours) {}

    public record Jwt(String secret, long accessTokenMinutes) {}

    public record Mail(String mode) {}

    public record Admin(String email, String password) {}

    public record Ai(
            String provider,
            String apiKey,
            String baseUrl,
            String model,
            BigDecimal priceInputPerM,
            BigDecimal priceOutputPerM,
            int timeoutSeconds,
            int maxTokens
    ) {
        public BigDecimal inputPrice() {
            return priceInputPerM == null ? BigDecimal.ZERO : priceInputPerM;
        }

        public BigDecimal outputPrice() {
            return priceOutputPerM == null ? BigDecimal.ZERO : priceOutputPerM;
        }

        public boolean deepSeekConfigured() {
            return provider != null
                    && "deepseek".equalsIgnoreCase(provider.trim())
                    && apiKey != null
                    && !apiKey.isBlank();
        }
    }
}
