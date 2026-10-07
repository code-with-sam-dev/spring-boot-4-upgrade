package com.example.orders.config;

import static tools.jackson.databind.PropertyNamingStrategies.SNAKE_CASE;
import static tools.jackson.databind.cfg.DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ext.javatime.deser.LocalDateDeserializer;
import tools.jackson.databind.ext.javatime.ser.LocalDateSerializer;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The JSON contract our clients rely on: snake_case names
 * and UK style dates, "07/10/2026".
 */
@Configuration
public class JacksonConfig {

    public static final DateTimeFormatter UK_DATE =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Bean
    public ObjectMapper objectMapper(
            JsonMapper.Builder builder) {
        return builder
                .propertyNamingStrategy(SNAKE_CASE)
                .disable(WRITE_DATES_AS_TIMESTAMPS)
                .addModule(ukDates())
                .build();
    }

    private static SimpleModule ukDates() {
        return new SimpleModule("uk-dates")
                .addSerializer(LocalDate.class,
                        new LocalDateSerializer(UK_DATE))
                .addDeserializer(LocalDate.class,
                        new LocalDateDeserializer(UK_DATE));
    }
}
