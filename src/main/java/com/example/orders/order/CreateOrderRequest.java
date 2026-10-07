package com.example.orders.order;

import java.math.BigDecimal;
import java.util.List;

import com.example.orders.money.Money;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * The body of POST /orders. It knows how to turn itself into
 * order lines.
 */
public record CreateOrderRequest(
        @NotBlank String customerName,
        @NotBlank String currency,
        @NotEmpty List<@Valid Line> items) {

    public record Line(
            @NotBlank String sku,
            @Positive int quantity,
            @NotNull @Positive BigDecimal unitPrice) {
    }

    public List<OrderLine> toLines() {
        return items.stream()
                .map(this::toLine)
                .toList();
    }

    private OrderLine toLine(Line item) {
        Money price = new Money(item.unitPrice(), currency);
        return new OrderLine(
                item.sku(), item.quantity(), price);
    }
}
