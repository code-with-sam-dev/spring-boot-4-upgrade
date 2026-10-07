package com.example.orders.order;

import java.time.LocalDate;

import com.example.orders.money.Money;

/**
 * Version 2 of GET /orders/{id}: a summary with an item
 * count instead of the full list of lines.
 */
public record OrderSummaryV2(
        Long id,
        String customerName,
        int itemCount,
        Money total,
        LocalDate placedOn,
        PaymentStatus paymentStatus) {

    static OrderSummaryV2 of(OrderView order) {
        int count = order.items().stream()
                .mapToInt(OrderView.Item::quantity)
                .sum();
        return new OrderSummaryV2(order.id(),
                order.customerName(), count, order.total(),
                order.placedOn(), order.paymentStatus());
    }
}
