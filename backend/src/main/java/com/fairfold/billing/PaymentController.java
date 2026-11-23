package com.fairfold.billing;

import com.fairfold.config.AppProperties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/** Public browser redirects from the payment provider. */
@RestController
@RequestMapping("/api/payments")
@Tag(name = "Payments", description = "Stripe and local fake payment callbacks")
public class PaymentController {

    private final BillingService billingService;
    private final AppProperties properties;

    public PaymentController(BillingService billingService, AppProperties properties) {
        this.billingService = billingService;
        this.properties = properties;
    }

    @GetMapping("/stripe/callback")
    @Operation(summary = "Stripe Checkout return URL")
    public ResponseEntity<Void> stripeCallback(
            @RequestParam(name = "session_id", required = false) String sessionId,
            @RequestParam(required = false) String status) {
        PaymentStatus result;
        if ("cancelled".equalsIgnoreCase(status)) {
            result = sessionId == null ? PaymentStatus.cancelled
                    : safelyComplete(sessionId, true);
        } else if (sessionId == null || sessionId.isBlank()) {
            result = PaymentStatus.failed;
        } else {
            result = safelyComplete(sessionId, false);
        }
        return redirect(result);
    }

    PaymentStatus safelyComplete(String sessionId, boolean cancelled) {
        try {
            return billingService.complete(sessionId, cancelled);
        } catch (RuntimeException e) {
            return cancelled ? PaymentStatus.cancelled : PaymentStatus.failed;
        }
    }

    ResponseEntity<Void> redirect(PaymentStatus status) {
        String suffix = status == PaymentStatus.completed ? "success"
                : status == PaymentStatus.cancelled ? "cancelled" : "failed";
        String base = properties.baseUrl() == null || properties.baseUrl().isBlank()
                ? "http://localhost:5173" : properties.baseUrl();
        String target = (base.endsWith("/") ? base.substring(0, base.length() - 1) : base)
                + "/employer/billing?payment=" + suffix;
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(target)).build();
    }
}
