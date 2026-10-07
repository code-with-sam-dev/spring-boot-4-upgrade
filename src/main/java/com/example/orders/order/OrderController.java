package com.example.orders.order;

import java.net.URI;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService service;

    public OrderController(OrderService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<OrderView> place(
            @Valid @RequestBody CreateOrderRequest request) {
        OrderView order = service.place(request);
        URI location = URI.create("/orders/" + order.id());
        return ResponseEntity.created(location).body(order);
    }

    @GetMapping(path = "/{id}", version = "1")
    public OrderView find(@PathVariable long id) {
        return service.find(id);
    }

    @GetMapping(path = "/{id}", version = "2")
    public OrderSummaryV2 findV2(@PathVariable long id) {
        return OrderSummaryV2.of(service.find(id));
    }
}
