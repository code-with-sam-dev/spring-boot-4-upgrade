package com.example.orders.order;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.http.HttpHeaders.LOCATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import com.example.orders.TestOrders;
import com.example.orders.config.JacksonConfig;
import com.example.orders.money.Money;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(OrderController.class)
@Import(JacksonConfig.class)
class OrderControllerWebMvcTest {

    private static final Money PRICE = Money.of("24.50", "GBP");

    @Autowired
    private MockMvc mvc;

    @MockBean
    private OrderService service;

    private final OrderView placed = new OrderView(42L,
            "Sarah Thompson",
            List.of(new OrderView.Item("KETTLE-01", 1, PRICE)),
            PRICE,
            LocalDate.of(2026, 10, 7),
            PaymentStatus.AUTHORISED);

    @Test
    void createsAnOrder() throws Exception {
        given(service.place(any())).willReturn(placed);

        postOrder(TestOrders.SARAH)
                .andExpect(status().isCreated())
                .andExpect(header()
                        .string(LOCATION, "/orders/42"));
    }

    @Test
    void writesTheAgreedJsonContract() throws Exception {
        given(service.find(42L)).willReturn(placed);

        mvc.perform(get("/orders/42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customer_name")
                        .value("Sarah Thompson"))
                .andExpect(jsonPath("$.placed_on")
                        .value("07/10/2026"))
                .andExpect(jsonPath("$.total")
                        .value("24.50 GBP"));
    }

    @Test
    void returns404ForAnUnknownOrder() throws Exception {
        given(service.find(7L))
                .willThrow(new OrderNotFoundException(7L));

        mvc.perform(get("/orders/7"))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsAnOrderWithNoCustomer() throws Exception {
        String noCustomer = """
                {"currency": "GBP", "items": [
                  {"sku": "A", "quantity": 1, "unit_price": 1}]}
                """;

        postOrder(noCustomer)
                .andExpect(status().isBadRequest());
    }

    private ResultActions postOrder(String body)
            throws Exception {
        return mvc.perform(post("/orders")
                .contentType(APPLICATION_JSON)
                .content(body));
    }
}
