package com.example.orders.money;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class MoneyTest {

    @Test
    void holdsTwoDecimalPlaces() {
        Money money = new Money(new BigDecimal("24.5"), "GBP");
        assertThat(money.amount())
                .isEqualByComparingTo("24.50");
        assertThat(Money.of("3", "GBP").display())
                .isEqualTo("3.00 GBP");
    }

    @Test
    void addsTheSameCurrency() {
        Money sum = Money.of("24.50", "GBP")
                .plus(Money.of("12.00", "GBP"));
        assertThat(sum).isEqualTo(Money.of("36.50", "GBP"));
    }

    @Test
    void refusesToAddDifferentCurrencies() {
        Money pounds = Money.of("1.00", "GBP");
        Money euros = Money.of("1.00", "EUR");
        assertThatThrownBy(() -> pounds.plus(euros))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cannot add EUR to GBP");
    }

    @Test
    void multipliesByQuantity() {
        assertThat(Money.of("6.00", "GBP").times(2))
                .isEqualTo(Money.of("12.00", "GBP"));
    }

    @Test
    void displaysAmountThenCurrency() {
        assertThat(Money.of("1499", "USD").display())
                .isEqualTo("1499.00 USD");
    }
}
