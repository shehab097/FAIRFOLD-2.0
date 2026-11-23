package com.fairfold.billing;

import com.fairfold.config.AppProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/** Stripe hosted Checkout (test mode) using the REST API, no SDK dependency. */
@Component
public class StripePaymentProvider implements PaymentProvider {

    private final AppProperties.Payment config;
    private final ObjectMapper objectMapper;
    private final HttpClient http;

    @org.springframework.beans.factory.annotation.Autowired
    public StripePaymentProvider(AppProperties properties, ObjectMapper objectMapper) {
        this(properties, objectMapper, HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20)).build());
    }

    StripePaymentProvider(AppProperties properties, ObjectMapper objectMapper, HttpClient http) {
        this.config = properties.payment();
        this.objectMapper = objectMapper;
        this.http = http;
    }

    @Override
    public String name() {
        return "stripe";
    }

    @Override
    public boolean available() {
        return config != null && config.stripe() != null
                && config.stripe().secretKey() != null
                && !config.stripe().secretKey().isBlank();
    }

    @Override
    public CreatedPayment createPayment(BigDecimal amountBdt, String merchantInvoice, String callbackUrl) {
        if (!available()) {
            throw new PaymentProviderException("Stripe is not configured.");
        }
        String currency = currency();
        Map<String, String> form = new LinkedHashMap<>();
        form.put("mode", "payment");
        form.put("success_url", callbackUrl + "?session_id={CHECKOUT_SESSION_ID}");
        form.put("cancel_url", callbackUrl + "?status=cancelled&session_id={CHECKOUT_SESSION_ID}");
        form.put("client_reference_id", merchantInvoice);
        form.put("line_items[0][quantity]", "1");
        form.put("line_items[0][price_data][currency]", currency);
        form.put("line_items[0][price_data][unit_amount]", minorUnits(amountBdt));
        form.put("line_items[0][price_data][product_data][name]", "FairFold plan");
        form.put("metadata[merchant_invoice]", merchantInvoice);
        form.put("payment_intent_data[metadata][merchant_invoice]", merchantInvoice);
        JsonNode root = request("POST", "/v1/checkout/sessions", form);
        String id = root.path("id").asText("");
        String url = root.path("url").asText("");
        if (id.isBlank() || url.isBlank()) {
            throw new PaymentProviderException("Stripe returned an invalid Checkout Session.");
        }
        return new CreatedPayment(id, url);
    }

    @Override
    public ExecutionResult execute(String providerPaymentId) {
        if (!available()) {
            throw new PaymentProviderException("Stripe is not configured.");
        }
        JsonNode root = request("GET", "/v1/checkout/sessions/" + providerPaymentId, null);
        String paymentStatus = root.path("payment_status").asText("");
        boolean success = "paid".equals(paymentStatus);
        BigDecimal amount = root.has("amount_total")
                ? BigDecimal.valueOf(root.path("amount_total").asLong()).movePointLeft(2)
                : null;
        String transactionId = root.path("payment_intent").isTextual()
                ? root.path("payment_intent").asText() : providerPaymentId;
        return new ExecutionResult(success, transactionId, amount,
                root.path("currency").asText(currency()).toLowerCase());
    }

    private JsonNode request(String method, String path, Map<String, String> form) {
        String base = config.stripe().baseUrl() == null || config.stripe().baseUrl().isBlank()
                ? "https://api.stripe.com" : config.stripe().baseUrl();
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(base + path))
                .timeout(Duration.ofSeconds(30))
                .header("Authorization", "Bearer " + config.stripe().secretKey());
        if (form == null) {
            builder.GET();
        } else {
            StringBuilder body = new StringBuilder();
            for (Map.Entry<String, String> entry : form.entrySet()) {
                if (!body.isEmpty()) {
                    body.append('&');
                }
                body.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8))
                        .append('=')
                        .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
            }
            builder.header("Content-Type", "application/x-www-form-urlencoded")
                    .method(method, HttpRequest.BodyPublishers.ofString(body.toString()));
        }
        try {
            HttpResponse<String> response = http.send(builder.build(),
                    HttpResponse.BodyHandlers.ofString());
            JsonNode root = objectMapper.readTree(response.body());
            if (response.statusCode() >= 400) {
                String message = root.path("error").path("message").asText("Stripe request failed.");
                throw new PaymentProviderException(message);
            }
            return root;
        } catch (PaymentProviderException e) {
            throw e;
        } catch (Exception e) {
            throw new PaymentProviderException("Could not reach Stripe: " + e.getMessage(), e);
        }
    }

    private String currency() {
        return config.currency() == null || config.currency().isBlank()
                ? "bdt" : config.currency().trim().toLowerCase();
    }

    private String minorUnits(BigDecimal amount) {
        return amount.movePointRight(2).setScale(0, RoundingMode.HALF_UP).toPlainString();
    }
}
