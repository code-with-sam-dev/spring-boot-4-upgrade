package com.example.orders.order;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.example.orders.money.Money;

import jakarta.persistence.*;

/**
 * An order prices itself from its lines and refuses to exist
 * without any.
 */
@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String customerName;
    private BigDecimal totalAmount;
    private String currency;
    private LocalDate placedOn;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "order_lines",
            joinColumns = @JoinColumn(name = "order_id"))
    private List<OrderLine> lines = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    protected Order() {
    }

    public static Order place(
            String customerName,
            List<OrderLine> lines,
            LocalDate placedOn) {
        if (lines.isEmpty()) {
            throw new IllegalArgumentException(
                    "An order needs at least one item");
        }
        Money total = lines.stream()
                .map(OrderLine::subtotal)
                .reduce(Money::plus)
                .orElseThrow();
        Order order = new Order();
        order.customerName = customerName;
        order.lines = new ArrayList<>(lines);
        order.totalAmount = total.amount();
        order.currency = total.currency();
        order.placedOn = placedOn;
        return order;
    }

    public void recordPayment(PaymentStatus status) {
        this.paymentStatus = status;
    }

    public Money total() {
        return new Money(totalAmount, currency);
    }

    public Long id() {
        return id;
    }

    public String customerName() {
        return customerName;
    }

    public List<OrderLine> lines() {
        return List.copyOf(lines);
    }

    public LocalDate placedOn() {
        return placedOn;
    }

    public PaymentStatus paymentStatus() {
        return paymentStatus;
    }
}
