package com.example.orders.payments;

import java.time.Duration;

import com.example.orders.money.Money;
import com.example.orders.order.PaymentGateway;
import com.example.orders.order.PaymentStatus;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * The payments service over HTTP, behind the PaymentGateway
 * port.
 */
@Component
public class HttpPaymentGateway implements PaymentGateway {

    private static final String PATH =
            "/payments/authorisations";

    private final RestTemplate restTemplate;

    public HttpPaymentGateway(
            RestTemplateBuilder builder,
            @Value("${payments.base-url}") String baseUrl) {
        this.restTemplate = builder
                .rootUri(baseUrl)
                .connectTimeout(Duration.ofSeconds(2))
                .readTimeout(Duration.ofSeconds(5))
                .build();
    }

    @Override
    public PaymentStatus authorise(long orderId, Money amount) {
        AuthoriseRequest request = new AuthoriseRequest(
                orderId, amount.amount(), amount.currency());
        AuthorisationResponse response = restTemplate
                .postForObject(PATH, request,
                        AuthorisationResponse.class);
        if (response == null) {
            String message = "Payments returned an empty body"
                    + " for order " + orderId;
            throw new IllegalStateException(message);
        }
        return PaymentStatus.valueOf(response.status());
    }
}
