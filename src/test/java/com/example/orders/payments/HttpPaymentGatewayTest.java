package com.example.orders.payments;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.example.orders.money.Money;
import com.example.orders.order.PaymentStatus;
import org.junit.jupiter.api.Test;

import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.response.DefaultResponseCreator;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

class HttpPaymentGatewayTest {

    private static final String AUTHORISED = """
            {"status":"AUTHORISED","reference":"pay-42"}
            """;
    private static final String DECLINED = """
            {"status":"DECLINED","reference":"pay-9"}
            """;
    private static final String BASE = "http://payments.test";
    private static final String URL =
            BASE + "/payments/authorisations";

    private final RestClient.Builder builder =
            RestClient.builder().baseUrl(BASE);
    private final MockRestServiceServer server =
            MockRestServiceServer.bindTo(builder).build();
    private final HttpPaymentGateway gateway =
            new HttpPaymentGateway(paymentsApi(builder));

    private static PaymentsApi paymentsApi(
            RestClient.Builder builder) {
        RestClient client = builder.build();
        return HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(client))
                .build()
                .createClient(PaymentsApi.class);
    }

    @Test
    void postsTheAmountAndReadsTheStatus() {
        server.expect(requestTo(URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.amount").value(36.5))
                .andRespond(json(AUTHORISED));

        Money total = Money.of("36.50", "GBP");
        assertThat(gateway.authorise(42L, total))
                .isEqualTo(PaymentStatus.AUTHORISED);
    }

    @Test
    void passesADeclineThrough() {
        server.expect(requestTo(URL))
                .andRespond(json(DECLINED));

        Money total = Money.of("1499.00", "GBP");
        assertThat(gateway.authorise(9L, total))
                .isEqualTo(PaymentStatus.DECLINED);
    }

    @Test
    void refusesAnEmptyBody() {
        server.expect(requestTo(URL)).andRespond(withSuccess());
        Money total = Money.of("1.00", "GBP");

        assertThatThrownBy(() -> gateway.authorise(5L, total))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Payments returned an empty body"
                        + " for order 5");
    }

    private static DefaultResponseCreator json(String body) {
        return withSuccess(body, APPLICATION_JSON);
    }
}
