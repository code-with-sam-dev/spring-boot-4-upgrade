package com.example.orders.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import com.example.orders.money.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.springframework.test.util.ReflectionTestUtils;

class OrderServiceTest {

    private final OrderRepository repository =
            mock(OrderRepository.class);
    private final PaymentGateway payments =
            mock(PaymentGateway.class);
    private final Clock clock = Clock.fixed(
            Instant.parse("2026-10-07T09:00:00Z"),
            ZoneId.of("Europe/London"));
    private final OrderService service =
            new OrderService(repository, payments, clock);

    private final CreateOrderRequest sarah =
            new CreateOrderRequest("Sarah Thompson", "GBP",
                    List.of(line("KETTLE-01", 1, "24.50"),
                            line("MUG-02", 2, "6.00")));

    private static CreateOrderRequest.Line line(
            String sku, int quantity, String price) {
        return new CreateOrderRequest.Line(
                sku, quantity, new BigDecimal(price));
    }

    @BeforeEach
    void saveAssignsAnId() {
        when(repository.save(any(Order.class)))
                .thenAnswer(this::assignId);
        when(payments.authorise(anyLong(), any()))
                .thenReturn(PaymentStatus.AUTHORISED);
    }

    private Order assignId(InvocationOnMock call) {
        Order order = call.getArgument(0);
        ReflectionTestUtils.setField(order, "id", 42L);
        return order;
    }

    @Test
    void stampsTodaysDate() {
        assertThat(service.place(sarah).placedOn())
                .isEqualTo(LocalDate.of(2026, 10, 7));
    }

    @Test
    void asksPaymentsToAuthoriseTheTotal() {
        service.place(sarah);
        Money total = Money.of("36.50", "GBP");
        verify(payments).authorise(42L, total);
    }

    @Test
    void recordsThePaymentOutcome() {
        when(payments.authorise(anyLong(), any()))
                .thenReturn(PaymentStatus.DECLINED);
        assertThat(service.place(sarah).paymentStatus())
                .isEqualTo(PaymentStatus.DECLINED);
    }

    @Test
    void returnsTheStoredOrder() {
        assertThat(service.place(sarah).id()).isEqualTo(42L);
    }

    @Test
    void reportsAMissingOrder() {
        when(repository.findById(7L))
                .thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.find(7L))
                .isInstanceOf(OrderNotFoundException.class)
                .hasMessage("No order with id 7");
    }
}
