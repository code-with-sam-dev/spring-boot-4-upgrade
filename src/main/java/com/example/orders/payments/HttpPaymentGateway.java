package com.example.orders.payments;

import com.example.orders.money.Money;
import com.example.orders.order.PaymentGateway;
import com.example.orders.order.PaymentStatus;

import org.springframework.stereotype.Component;

/**
 * The payments service over HTTP, behind the PaymentGateway
 * port.
 */
@Component
public class HttpPaymentGateway implements PaymentGateway {

    private final PaymentsApi api;

    HttpPaymentGateway(PaymentsApi api) {
        this.api = api;
    }

    @Override
    public PaymentStatus authorise(long orderId, Money amount) {
        AuthoriseRequest request = new AuthoriseRequest(
                orderId, amount.amount(), amount.currency());
        AuthorisationResponse response =
                api.authorise(request).getBody();
        if (response == null) {
            String message = "Payments returned an empty body"
                    + " for order " + orderId;
            throw new IllegalStateException(message);
        }
        return PaymentStatus.valueOf(response.status());
    }
}
