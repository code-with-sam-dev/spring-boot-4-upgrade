package com.example.orders.order;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import com.example.orders.money.Money;
import org.junit.jupiter.api.Test;

class CreateOrderRequestTest {

    @Test
    void turnsItemsIntoLinesInTheOrderCurrency() {
        CreateOrderRequest request = new CreateOrderRequest(
                "Sarah Thompson", "GBP", List.of(
                        line("KETTLE-01", 1, "24.50"),
                        line("MUG-02", 2, "6")));

        List<OrderLine> lines = request.toLines();

        assertThat(lines).extracting(OrderLine::sku)
                .containsExactly("KETTLE-01", "MUG-02");
        assertThat(lines.get(1).unitPrice())
                .isEqualTo(Money.of("6.00", "GBP"));
        assertThat(lines.get(1).quantity()).isEqualTo(2);
    }

    private static CreateOrderRequest.Line line(
            String sku, int quantity, String price) {
        return new CreateOrderRequest.Line(
                sku, quantity, new BigDecimal(price));
    }
}
