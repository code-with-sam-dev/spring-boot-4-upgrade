package com.example.orders.order;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.springframework.http.HttpHeaders.LOCATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.orders.TestOrders;
import com.example.orders.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class OrdersApiIT {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private PaymentGateway payments;

    @BeforeEach
    void paymentsAuthorise() {
        given(payments.authorise(anyLong(), any()))
                .willReturn(PaymentStatus.AUTHORISED);
    }

    @Test
    void placesAnOrder() throws Exception {
        postOrder(TestOrders.SARAH)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customer_name")
                        .value("Sarah Thompson"))
                .andExpect(jsonPath("$.total")
                        .value("36.50 GBP"))
                .andExpect(jsonPath("$.payment_status")
                        .value("AUTHORISED"));
    }

    @Test
    void readsBackWhatWasStored() throws Exception {
        String location = postOrder(TestOrders.SARAH)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getHeader(LOCATION);

        mvc.perform(get(location))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()")
                        .value(2))
                .andExpect(jsonPath("$.items[1].unit_price")
                        .value("6.00 GBP"));
    }

    @Test
    void recordsADecline() throws Exception {
        given(payments.authorise(anyLong(), any()))
                .willReturn(PaymentStatus.DECLINED);

        postOrder(TestOrders.JAMES_LARGE)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.payment_status")
                        .value("DECLINED"));
    }

    @Test
    void returns404ForAnUnknownOrder() throws Exception {
        mvc.perform(get("/orders/999999"))
                .andExpect(status().isNotFound());
    }

    private ResultActions postOrder(String body)
            throws Exception {
        return mvc.perform(post("/orders")
                .contentType(APPLICATION_JSON)
                .content(body));
    }
}
