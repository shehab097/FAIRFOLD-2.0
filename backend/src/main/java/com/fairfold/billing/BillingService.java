package com.fairfold.billing;

import com.fairfold.billing.dto.BillingResponse;
import com.fairfold.billing.dto.PaymentItem;
import com.fairfold.common.audit.AuditService;
import com.fairfold.common.errors.ApiException;
import com.fairfold.common.paging.PageResponse;
import com.fairfold.config.AppProperties;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/** FR-048/FR-056: plans, checkout, callbacks, subscriptions and payment history. */
@Service
public class BillingService {

    private final PlanRepository planRepository;
    private final CompanySubscriptionRepository subscriptionRepository;
    private final PaymentRepository paymentRepository;
    private final PlanService planService;
    private final QuotaService quotaService;
    private final List<PaymentProvider> providers;
    private final AuditService auditService;
    private final AppProperties properties;
    private final Clock clock;

    public BillingService(PlanRepository planRepository,
                          CompanySubscriptionRepository subscriptionRepository,
                          PaymentRepository paymentRepository,
                          PlanService planService,
                          QuotaService quotaService,
                          List<PaymentProvider> providers,
                          AuditService auditService,
                          AppProperties properties,
                          Clock clock) {
        this.planRepository = planRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.paymentRepository = paymentRepository;
        this.planService = planService;
        this.quotaService = quotaService;
        this.providers = providers;
        this.auditService = auditService;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public BillingResponse billing(UUID companyId) {
        Plan plan = planService.effectivePlan(companyId);
        CompanySubscription subscription = planService.activeSubscription(companyId);
        QuotaService.QuotaInfo quota = quotaService.forCompany(companyId);
        List<BillingResponse.PlanView> plans = planRepository.findByActiveTrueOrderByPriceBdtAsc()
                .stream()
                .map(p -> new BillingResponse.PlanView(p.getId(), p.getName(),
                        p.getMonthlyScreenings(), p.getPriceBdt(), p.getDurationDays()))
                .toList();
        return new BillingResponse(
                new BillingResponse.CurrentPlan(plan.getName(), plan.getMonthlyScreenings(),
                        subscription == null ? null : subscription.getEndsAt()),
                quota.used(), quota.quota(), plans);
    }

    @Transactional(readOnly = true)
    public PageResponse<PaymentItem> payments(UUID companyId, int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(100, size));
        Page<Payment> result = paymentRepository.findByCompanyIdOrderByCreatedAtDesc(
                companyId, PageRequest.of(safePage, safeSize));
        Map<UUID, String> planNames = planRepository.findAllById(
                        result.getContent().stream().map(Payment::getPlanId).toList())
                .stream().collect(Collectors.toMap(Plan::getId, Plan::getName));
        List<PaymentItem> items = result.getContent().stream()
                .map(payment -> new PaymentItem(payment.getId(),
                        planNames.getOrDefault(payment.getPlanId(), "Plan"),
                        payment.getAmountBdt(), payment.getStatus().name(), payment.getCreatedAt()))
                .toList();
        return PageResponse.of(items, safePage, safeSize, result.getTotalElements());
    }

    public record CheckoutResponse(String redirectUrl) {}

    @Transactional
    public CheckoutResponse checkout(UUID companyId, UUID planId) {
        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> ApiException.notFound("PLAN_NOT_FOUND", "Plan not found."));
        if (!plan.isActive() || plan.getDurationDays() == null
                || plan.getPriceBdt() == null || plan.getPriceBdt().signum() <= 0) {
            throw ApiException.badRequest("PLAN_NOT_PURCHASABLE", "This plan cannot be purchased.");
        }
        PaymentProvider provider = provider();
        Payment payment = new Payment();
        payment.setCompanyId(companyId);
        payment.setPlanId(plan.getId());
        payment.setAmountBdt(plan.getPriceBdt());
        payment.setProvider(provider.name());
        payment.setMerchantInvoice("FF-" + UUID.randomUUID().toString().replace("-", ""));
        payment.setStatus(PaymentStatus.initiated);
        paymentRepository.save(payment);

        try {
            PaymentProvider.CreatedPayment created = provider.createPayment(
                    plan.getPriceBdt(), payment.getMerchantInvoice(),
                    trimSlash(properties.baseUrl()) + "/api/payments/stripe/callback");
            payment.setProviderPaymentId(created.providerPaymentId());
            paymentRepository.save(payment);
            return new CheckoutResponse(created.redirectUrl());
        } catch (PaymentProviderException e) {
            payment.setStatus(PaymentStatus.failed);
            paymentRepository.save(payment);
            throw new ApiException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,
                    "PAYMENT_UNAVAILABLE", "The payment provider is unavailable right now.");
        }
    }

    /**
     * Completes a payment from a provider callback or the fake pay endpoint.
     * Idempotent: a completed payment is returned unchanged.
     */
    @Transactional
    public PaymentStatus complete(String providerPaymentId, boolean cancelled) {
        Payment payment = paymentRepository.findByProviderPaymentId(providerPaymentId)
                .orElseThrow(() -> ApiException.notFound("PAYMENT_NOT_FOUND", "Payment not found."));
        if (payment.getStatus() == PaymentStatus.completed) {
            return PaymentStatus.completed;
        }
        if (cancelled) {
            return finish(payment, PaymentStatus.cancelled, "cancelled");
        }
        PaymentProvider provider = provider(payment.getProvider());
        PaymentProvider.ExecutionResult result;
        try {
            result = provider.execute(providerPaymentId);
        } catch (PaymentProviderException e) {
            return finish(payment, PaymentStatus.failed, "provider_error");
        }
        if (!result.success()) {
            return finish(payment, PaymentStatus.failed, "declined");
        }
        if (result.amountBdt() != null
                && result.amountBdt().compareTo(payment.getAmountBdt()) != 0) {
            return finish(payment, PaymentStatus.failed, "amount_mismatch");
        }
        if (result.currency() != null && !result.currency().isBlank()
                && !result.currency().equalsIgnoreCase(configuredCurrency())) {
            return finish(payment, PaymentStatus.failed, "currency_mismatch");
        }
        Plan plan = planRepository.findById(payment.getPlanId()).orElse(null);
        if (plan == null || plan.getDurationDays() == null) {
            return finish(payment, PaymentStatus.failed, "plan_unavailable");
        }

        var now = clock.instant();
        subscriptionRepository.endActive(payment.getCompanyId(), now);
        CompanySubscription subscription = new CompanySubscription();
        subscription.setCompanyId(payment.getCompanyId());
        subscription.setPlanId(plan.getId());
        subscription.setStartsAt(now);
        subscription.setEndsAt(now.plus(plan.getDurationDays(), ChronoUnit.DAYS));
        subscriptionRepository.save(subscription);

        payment.setStatus(PaymentStatus.completed);
        payment.setCompletedAt(now);
        paymentRepository.save(payment);
        auditService.record(null, "payment_completed", "payment", payment.getId().toString(),
                "{\"plan\":\"" + plan.getName() + "\",\"amountBdt\":" + payment.getAmountBdt() + "}");
        return PaymentStatus.completed;
    }

    @Transactional
    public PaymentStatus fakePay(String providerPaymentId, String outcome) {
        if ("failed".equalsIgnoreCase(outcome)) {
            Payment payment = paymentRepository.findByProviderPaymentId(providerPaymentId)
                    .orElseThrow(() -> ApiException.notFound("PAYMENT_NOT_FOUND", "Payment not found."));
            if (payment.getStatus() == PaymentStatus.completed) {
                return PaymentStatus.completed;
            }
            return finish(payment, PaymentStatus.failed, "declined");
        }
        return complete(providerPaymentId, "cancelled".equalsIgnoreCase(outcome));
    }

    private PaymentStatus finish(Payment payment, PaymentStatus status, String reason) {
        payment.setStatus(status);
        paymentRepository.save(payment);
        auditService.record(null, "payment_failed", "payment", payment.getId().toString(),
                "{\"reason\":\"" + reason + "\"}");
        return status;
    }

    private PaymentProvider provider() {
        String configured = properties.payment() == null
                || properties.payment().provider() == null
                || properties.payment().provider().isBlank()
                ? "fake" : properties.payment().provider().trim().toLowerCase(Locale.ROOT);
        return providers.stream()
                .filter(p -> p.name().equals(configured))
                .filter(PaymentProvider::available)
                .findFirst()
                .orElseThrow(() -> new ApiException(
                        org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,
                        "PAYMENT_UNAVAILABLE", "The payment provider is not configured."));
    }

    private PaymentProvider provider(String name) {
        return providers.stream()
                .filter(p -> p.name().equalsIgnoreCase(name))
                .findFirst()
                .orElseThrow(() -> new PaymentProviderException("Unknown payment provider"));
    }

    private String configuredCurrency() {
        return properties.payment() == null || properties.payment().currency() == null
                || properties.payment().currency().isBlank()
                ? "bdt" : properties.payment().currency().trim().toLowerCase(Locale.ROOT);
    }

    private String trimSlash(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
