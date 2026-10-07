package com.example.orders.order;

import com.example.orders.money.Money;

/**
 * What the order side needs from payments. The HTTP details
 * live in the payments package, which implements this.
 */
public interface PaymentGateway {

    PaymentStatus authorise(long orderId, Money amount);
}
