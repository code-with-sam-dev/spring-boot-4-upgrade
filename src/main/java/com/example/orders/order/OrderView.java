package com.example.orders.order;

import java.time.LocalDate;
import java.util.List;

import com.example.orders.money.Money;

/**
 * What GET /orders/{id} and POST /orders return.
 */
public record OrderView(
        Long id,
        String customerName,
        List<Item> items,
        Money total,
        LocalDate placedOn,
        PaymentStatus paymentStatus) {

    public record Item(
            String sku, int quantity, Money unitPrice) {
    }

    static OrderView of(Order order) {
        List<Item> items = order.lines().stream()
                .map(OrderView::item)
                .toList();
        return new OrderView(order.id(), order.customerName(),
                items, order.total(), order.placedOn(),
                order.paymentStatus());
    }

    private static Item item(OrderLine line) {
        return new Item(
                line.sku(), line.quantity(), line.unitPrice());
    }
}
