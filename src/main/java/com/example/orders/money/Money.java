package com.example.orders.money;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * An amount in one currency, always held to two decimal
 * places.
 */
public record Money(BigDecimal amount, String currency) {

    public Money {
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(currency, "currency");
        amount = amount.setScale(2, RoundingMode.HALF_UP);
    }

    public static Money of(String amount, String currency) {
        return new Money(new BigDecimal(amount), currency);
    }

    public Money plus(Money other) {
        if (!currency.equals(other.currency)) {
            String message = "Cannot add " + other.currency
                    + " to " + currency;
            throw new IllegalArgumentException(message);
        }
        return new Money(amount.add(other.amount), currency);
    }

    public Money times(int quantity) {
        BigDecimal factor = BigDecimal.valueOf(quantity);
        return new Money(amount.multiply(factor), currency);
    }

    /** The wire and log form, for example "24.50 GBP". */
    public String display() {
        return amount.toPlainString() + " " + currency;
    }
}
