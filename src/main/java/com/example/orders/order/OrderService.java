package com.example.orders.order;

import java.time.Clock;
import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Places orders and reads them back. Pricing lives in Order;
 * payments sit behind the PaymentGateway port.
 */
@Service
public class OrderService {

    private final OrderRepository orders;
    private final PaymentGateway payments;
    private final Clock clock;

    public OrderService(
            OrderRepository orders,
            PaymentGateway payments,
            Clock clock) {
        this.orders = orders;
        this.payments = payments;
        this.clock = clock;
    }

    @Transactional
    public OrderView place(CreateOrderRequest request) {
        Order order = orders.save(Order.place(
                request.customerName(),
                request.toLines(),
                LocalDate.now(clock)));
        PaymentStatus status =
                payments.authorise(order.id(), order.total());
        order.recordPayment(status);
        return OrderView.of(order);
    }

    @Transactional(readOnly = true)
    public OrderView find(long id) {
        return orders.findById(id)
                .map(OrderView::of)
                .orElseThrow(() -> notFound(id));
    }

    private static OrderNotFoundException notFound(long id) {
        return new OrderNotFoundException(id);
    }
}
