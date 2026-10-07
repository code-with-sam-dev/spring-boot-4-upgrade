package com.example.orders;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.orders.order.OrderService;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class ApplicationIT {

    @Autowired
    private ApplicationContext context;

    @Test
    void contextLoads() {
        assertThat(context.getBeansOfType(OrderService.class))
                .hasSize(1);
    }
}
