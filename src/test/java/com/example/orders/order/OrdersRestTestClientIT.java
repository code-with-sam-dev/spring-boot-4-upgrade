package com.example.orders.order;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.DEFINED_PORT;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import com.example.orders.TestOrders;
import com.example.orders.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.client.RestTestClient;

/**
 * The same end-to-end check as OrdersHttpIT, written with
 * the fluent RestTestClient instead of TestRestTemplate.
 */
@SpringBootTest(webEnvironment = DEFINED_PORT, properties = {
        "server.port=18081",
        "spring.http.serviceclient.payments.base-url="
                + "http://localhost:18081"})
@AutoConfigureRestTestClient
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("local-stub")
class OrdersRestTestClientIT {

    @Autowired
    private RestTestClient client;

    @Test
    void placesAnOrderEndToEnd() {
        client.post().uri("/orders")
                .contentType(APPLICATION_JSON)
                .body(TestOrders.SARAH)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.payment_status")
                .isEqualTo("AUTHORISED")
                .jsonPath("$.total")
                .isEqualTo("36.50 GBP");
    }
}
