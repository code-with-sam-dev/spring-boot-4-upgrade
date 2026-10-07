package com.example.orders.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Map;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

class JacksonConfigTest {

    private static final LocalDate OCT_7 =
            LocalDate.of(2026, 10, 7);

    private final ObjectMapper mapper = new JacksonConfig()
            .objectMapper(JsonMapper.builder());

    record Sample(String customerName, LocalDate placedOn) {
    }

    @Test
    void writesSnakeCaseNames() throws Exception {
        String json = mapper.writeValueAsString(
                new Sample("James", OCT_7));
        assertThat(json)
                .contains("\"customer_name\":\"James\"");
    }

    @Test
    void writesUkDates() throws Exception {
        String json = mapper.writeValueAsString(
                new Sample("James", OCT_7));
        assertThat(json)
                .contains("\"placed_on\":\"07/10/2026\"");
    }

    @Test
    void readsUkDates() throws Exception {
        String json = "{\"customer_name\":\"James\","
                + "\"placed_on\":\"31/12/2026\"}";
        Sample sample = mapper.readValue(json, Sample.class);
        assertThat(sample.placedOn())
                .isEqualTo(LocalDate.of(2026, 12, 31));
    }

    @Test
    void doesNotWriteDatesAsNumbers() throws Exception {
        var dated = Map.of("d", LocalDate.of(2026, 1, 2));
        assertThat(mapper.writeValueAsString(dated))
                .isEqualTo("{\"d\":\"02/01/2026\"}");
    }
}
