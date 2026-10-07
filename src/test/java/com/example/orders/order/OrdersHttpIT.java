package com.example.orders.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.DEFINED_PORT;

import java.util.Map;

import com.example.orders.TestOrders;
import com.example.orders.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

/**
 * Runs the whole thing over real HTTP, including the call to
 * the payments stub.
 */
@SpringBootTest(webEnvironment = DEFINED_PORT, properties = {
        "server.port=18080",
        "payments.base-url=http://localhost:18080"})
@AutoConfigureTestRestTemplate
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("local-stub")
class OrdersHttpIT {

    @Autowired
    private TestRestTemplate rest;

    @Test
    void placesAnOrderEndToEnd() {
        ResponseEntity<Map> response = place(TestOrders.SARAH);

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody())
                .containsEntry("payment_status", "AUTHORISED")
                .containsEntry("total", "36.50 GBP");
    }

    @Test
    void theStubDeclinesLargeOrders() {
        assertThat(place(TestOrders.JAMES_LARGE).getBody())
                .containsEntry("payment_status", "DECLINED");
    }

    @Test
    void getReturnsTheStoredOrder() {
        ResponseEntity<Map> placed = place(TestOrders.SARAH);
        String location = placed.getHeaders()
                .getLocation().toString();

        ResponseEntity<Map> response =
                rest.getForEntity(location, Map.class);

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("customer_name"))
                .isEqualTo("Sarah Thompson");
    }

    @Test
    void healthIsUp() {
        String health = "/actuator/health";
        ResponseEntity<Map> response =
                rest.getForEntity(health, Map.class);

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .containsEntry("status", "UP");
    }

    private ResponseEntity<Map> place(String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        var request = new HttpEntity<>(body, headers);
        return rest.postForEntity(
                "/orders", request, Map.class);
    }
}
