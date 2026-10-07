package com.example.orders.order;

import java.math.BigDecimal;

import com.example.orders.money.Money;

import jakarta.persistence.Embeddable;

/**
 * One product on an order: what was bought, how many, and
 * at what price.
 */
@Embeddable
public class OrderLine {

    private String sku;
    private int quantity;
    private BigDecimal unitPrice;
    private String currency;

    protected OrderLine() {
    }

    public OrderLine(
            String sku,
            int quantity,
            Money unitPrice) {
        if (quantity < 1) {
            throw new IllegalArgumentException(
                    "Quantity below 1: " + quantity);
        }
        this.sku = sku;
        this.quantity = quantity;
        this.unitPrice = unitPrice.amount();
        this.currency = unitPrice.currency();
    }

    public Money subtotal() {
        return unitPrice().times(quantity);
    }

    public Money unitPrice() {
        return new Money(unitPrice, currency);
    }

    public String sku() {
        return sku;
    }

    public int quantity() {
        return quantity;
    }
}
