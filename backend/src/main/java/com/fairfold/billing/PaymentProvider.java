package com.fairfold.billing;

import java.math.BigDecimal;

public interface PaymentProvider {

    String name();

    boolean available();

    CreatedPayment createPayment(BigDecimal amountBdt, String merchantInvoice, String callbackUrl);

    ExecutionResult execute(String providerPaymentId);

    record CreatedPayment(String providerPaymentId, String redirectUrl) {}

    record ExecutionResult(boolean success, String transactionId, BigDecimal amountBdt, String currency) {}
}
