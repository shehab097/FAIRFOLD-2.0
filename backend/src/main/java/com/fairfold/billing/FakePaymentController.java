package com.fairfold.billing;

import com.fairfold.common.errors.ApiException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Local/test-only fake payment provider redirect. */
@RestController
@RequestMapping("/api/payments")
@Profile({"local", "test"})
@Tag(name = "Payments (fake)", description = "Local fake payment provider")
public class FakePaymentController {

    private final BillingService billingService;
    private final PaymentController paymentController;

    public FakePaymentController(BillingService billingService, PaymentController paymentController) {
        this.billingService = billingService;
        this.paymentController = paymentController;
    }

    @GetMapping("/fake/pay")
    @Operation(summary = "Complete a fake payment")
    public ResponseEntity<Void> fakePay(
            @RequestParam String paymentId,
            @RequestParam(defaultValue = "success") String outcome) {
        PaymentStatus result;
        try {
            result = billingService.fakePay(paymentId, outcome);
        } catch (ApiException e) {
            result = PaymentStatus.failed;
        }
        return paymentController.redirect(result);
    }
}
