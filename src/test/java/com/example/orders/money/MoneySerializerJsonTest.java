package com.example.orders.money;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;

@JsonTest
class MoneySerializerJsonTest {

    @Autowired
    private JacksonTester<Map<String, Money>> json;

    @Test
    void writesMoneyAsOneString() throws Exception {
        var total = Map.of("total", Money.of("36.5", "GBP"));
        assertThat(json.write(total))
                .extractingJsonPathStringValue("$.total")
                .isEqualTo("36.50 GBP");
    }

    @Test
    void keepsTrailingZeros() throws Exception {
        var total = Map.of("total", Money.of("10", "EUR"));
        assertThat(json.write(total))
                .extractingJsonPathStringValue("$.total")
                .isEqualTo("10.00 EUR");
    }
}
