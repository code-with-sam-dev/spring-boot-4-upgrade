package com.example.orders.payments;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.service.registry.ImportHttpServices;

/**
 * Registers PaymentsApi as a bean in the "payments" HTTP
 * service group.
 */
@Configuration
@ImportHttpServices(
        group = "payments",
        types = PaymentsApi.class)
public class PaymentsClientConfig {
}
