package com.example.orders.payments;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.example.orders.money.Money;
import com.example.orders.order.PaymentStatus;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.response.DefaultResponseCreator;

@RestClientTest(HttpPaymentGateway.class)
@TestPropertySource(properties =
        "payments.base-url=http://payments.test")
class HttpPaymentGatewayTest {

    private static final String AUTHORISED = """
            {"status":"AUTHORISED","reference":"pay-42"}
            """;
    private static final String DECLINED = """
            {"status":"DECLINED","reference":"pay-9"}
            """;
    private static final String PATH =
            "/payments/authorisations";

    @Autowired
    private HttpPaymentGateway gateway;

    @Autowired
    private MockRestServiceServer server;

    @Test
    void postsTheAmountAndReadsTheStatus() {
        server.expect(requestTo(PATH))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.amount").value(36.5))
                .andRespond(json(AUTHORISED));

        Money total = Money.of("36.50", "GBP");
        assertThat(gateway.authorise(42L, total))
                .isEqualTo(PaymentStatus.AUTHORISED);
    }

    @Test
    void passesADeclineThrough() {
        server.expect(requestTo(PATH))
                .andRespond(json(DECLINED));

        Money total = Money.of("1499.00", "GBP");
        assertThat(gateway.authorise(9L, total))
                .isEqualTo(PaymentStatus.DECLINED);
    }

    private static DefaultResponseCreator json(String body) {
        return withSuccess(body, APPLICATION_JSON);
    }
}
