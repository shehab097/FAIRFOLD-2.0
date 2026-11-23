package com.fairfold.billing;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Offline/test provider. It is only exposed through the local/test fake-pay
 * endpoint; production uses Stripe.
 */
@Component
public class FakePaymentProvider implements PaymentProvider {

    private final Map<String, BigDecimal> amounts = new ConcurrentHashMap<>();

    @Override
    public String name() {
        return "fake";
    }

    @Override
    public boolean available() {
        return true;
    }

    @Override
    public CreatedPayment createPayment(BigDecimal amountBdt, String merchantInvoice, String callbackUrl) {
        amounts.put(merchantInvoice, amountBdt);
        String redirect = "/api/payments/fake/pay?paymentId="
                + URLEncoder.encode(merchantInvoice, StandardCharsets.UTF_8)
                + "&outcome=success";
        return new CreatedPayment(merchantInvoice, redirect);
    }

    @Override
    public ExecutionResult execute(String providerPaymentId) {
        BigDecimal amount = amounts.remove(providerPaymentId);
        return new ExecutionResult(true, "fake-" + providerPaymentId, amount, "bdt");
    }
}
