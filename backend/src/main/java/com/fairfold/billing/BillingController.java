package com.fairfold.billing;

import com.fairfold.billing.dto.BillingResponse;
import com.fairfold.billing.dto.CheckoutRequest;
import com.fairfold.billing.dto.PaymentItem;
import com.fairfold.common.paging.PageResponse;
import com.fairfold.common.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/employer/billing")
@Tag(name = "Billing", description = "Employer plans, quota and Stripe Checkout")
@PreAuthorize("hasRole('EMPLOYER_HR')")
public class BillingController {

    private final BillingService billingService;

    public BillingController(BillingService billingService) {
        this.billingService = billingService;
    }

    @GetMapping
    @Operation(summary = "Current plan, monthly usage and available plans")
    public BillingResponse billing(@AuthenticationPrincipal CurrentUser user) {
        return billingService.billing(user.companyId());
    }

    @GetMapping("/payments")
    @Operation(summary = "Payment history")
    public PageResponse<PaymentItem> payments(@AuthenticationPrincipal CurrentUser user,
                                              @RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "20") int size) {
        return billingService.payments(user.companyId(), page, size);
    }

    @PostMapping("/checkout")
    @Operation(summary = "Create a payment and return the provider redirect URL")
    public BillingService.CheckoutResponse checkout(@AuthenticationPrincipal CurrentUser user,
                                                    @RequestBody CheckoutRequest request) {
        return billingService.checkout(user.companyId(), request.planId());
    }
}
