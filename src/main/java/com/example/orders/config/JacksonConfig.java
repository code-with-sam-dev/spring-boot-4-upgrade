package com.example.orders.config;

import static tools.jackson.databind.PropertyNamingStrategies.SNAKE_CASE;
import static tools.jackson.databind.cfg.DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import tools.jackson.databind.ext.javatime.deser.LocalDateDeserializer;
import tools.jackson.databind.ext.javatime.ser.LocalDateSerializer;
import tools.jackson.databind.module.SimpleModule;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The JSON contract our clients rely on: snake_case names
 * and UK style dates, "07/10/2026".
 *
 * A customizer rather than our own mapper: Spring Boot keeps
 * building the one JsonMapper, with @JacksonComponent
 * serializers and spring.jackson.* properties applied, and
 * adds our rules to it.
 */
@Configuration
public class JacksonConfig {

    public static final DateTimeFormatter UK_DATE =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Bean
    public JsonMapperBuilderCustomizer ordersJsonContract() {
        return builder -> builder
                .propertyNamingStrategy(SNAKE_CASE)
                .disable(WRITE_DATES_AS_TIMESTAMPS)
                .addModule(ukDates());
    }

    private static SimpleModule ukDates() {
        return new SimpleModule("uk-dates")
                .addSerializer(LocalDate.class,
                        new LocalDateSerializer(UK_DATE))
                .addDeserializer(LocalDate.class,
                        new LocalDateDeserializer(UK_DATE));
    }
}
