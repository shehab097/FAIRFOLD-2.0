package com.fairfold.billing;

import com.fairfold.config.AppProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Part 5.2: Stripe Checkout session mapping without a network call. */
class StripeProviderTest {

    @SuppressWarnings("unchecked")
    private StripePaymentProvider provider(HttpClient http, HttpResponse<String> response) throws Exception {
        AppProperties properties = mock(AppProperties.class);
        AppProperties.Payment payment = mock(AppProperties.Payment.class);
        AppProperties.Stripe stripe = mock(AppProperties.Stripe.class);
        when(properties.payment()).thenReturn(payment);
        when(payment.currency()).thenReturn("bdt");
        when(payment.stripe()).thenReturn(stripe);
        when(stripe.secretKey()).thenReturn("sk_test_dummy");
        when(stripe.baseUrl()).thenReturn("https://api.stripe.com");
        doReturn(response).when(http).send(any(HttpRequest.class),
                any(HttpResponse.BodyHandler.class));
        return new StripePaymentProvider(properties, new ObjectMapper(), http);
    }

    @Test
    void create_and_execute_map_checkout_session() throws Exception {
        HttpClient http = mock(HttpClient.class);
        HttpResponse<String> create = mock(HttpResponse.class);
        when(create.statusCode()).thenReturn(200);
        when(create.body()).thenReturn(
                "{\"id\":\"cs_test_1\",\"url\":\"https://checkout.stripe.com/c/pay/1\"}");
        StripePaymentProvider provider = provider(http, create);
        assertThat(provider.available()).isTrue();

        PaymentProvider.CreatedPayment created = provider.createPayment(
                new BigDecimal("1500.00"), "FF-1", "http://localhost:5173/api/payments/stripe/callback");
        assertThat(created.providerPaymentId()).isEqualTo("cs_test_1");
        assertThat(created.redirectUrl()).startsWith("https://checkout.stripe.com/");

        HttpResponse<String> retrieve = mock(HttpResponse.class);
        when(retrieve.statusCode()).thenReturn(200);
        when(retrieve.body()).thenReturn(
                "{\"payment_status\":\"paid\",\"amount_total\":150000,\"currency\":\"bdt\","
                        + "\"payment_intent\":\"pi_1\"}");
        StripePaymentProvider second = provider(http, retrieve);
        PaymentProvider.ExecutionResult result = second.execute("cs_test_1");
        assertThat(result.success()).isTrue();
        assertThat(result.transactionId()).isEqualTo("pi_1");
        assertThat(result.amountBdt()).isEqualByComparingTo("1500.00");
        assertThat(result.currency()).isEqualTo("bdt");
    }

    @Test
    void unpaid_session_is_not_successful() throws Exception {
        HttpClient http = mock(HttpClient.class);
        HttpResponse<String> retrieve = mock(HttpResponse.class);
        when(retrieve.statusCode()).thenReturn(200);
        when(retrieve.body()).thenReturn(
                "{\"payment_status\":\"unpaid\",\"amount_total\":150000,\"currency\":\"bdt\"}");
        StripePaymentProvider provider = provider(http, retrieve);
        assertThat(provider.execute("cs_test_2").success()).isFalse();
    }
}
