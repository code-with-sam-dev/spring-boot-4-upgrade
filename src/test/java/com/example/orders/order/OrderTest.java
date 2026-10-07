package com.example.orders.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;

import com.example.orders.money.Money;
import org.junit.jupiter.api.Test;

class OrderTest {

    private static final String SARAH = "Sarah Thompson";
    private static final LocalDate TODAY =
            LocalDate.of(2026, 10, 7);

    private final OrderLine kettle =
            new OrderLine("KETTLE-01", 1, gbp("24.50"));
    private final OrderLine mugs =
            new OrderLine("MUG-02", 2, gbp("6.00"));

    private static Money gbp(String amount) {
        return Money.of(amount, "GBP");
    }

    @Test
    void aLineKnowsItsSubtotal() {
        assertThat(mugs.subtotal()).isEqualTo(gbp("12.00"));
    }

    @Test
    void anOrderTotalsItsLines() {
        List<OrderLine> both = List.of(kettle, mugs);
        Order order = Order.place(SARAH, both, TODAY);
        assertThat(order.total()).isEqualTo(gbp("36.50"));
    }

    @Test
    void anOrderNeedsAtLeastOneItem() {
        List<OrderLine> none = List.of();
        assertThatThrownBy(
                () -> Order.place("James Okafor", none, TODAY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("An order needs at least one item");
    }

    @Test
    void linesMustShareACurrency() {
        Money euros = Money.of("6.00", "EUR");
        OrderLine inEuros = new OrderLine("MUG-02", 1, euros);
        List<OrderLine> mixed = List.of(kettle, inEuros);
        assertThatThrownBy(
                () -> Order.place(SARAH, mixed, TODAY))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aLineNeedsAPositiveQuantity() {
        Money price = gbp("24.50");
        assertThatThrownBy(
                () -> new OrderLine("KETTLE-01", 0, price))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void paymentStartsPendingAndRecordsTheOutcome() {
        Order order =
                Order.place(SARAH, List.of(kettle), TODAY);
        assertThat(order.paymentStatus())
                .isEqualTo(PaymentStatus.PENDING);

        order.recordPayment(PaymentStatus.DECLINED);

        assertThat(order.paymentStatus())
                .isEqualTo(PaymentStatus.DECLINED);
    }
}
