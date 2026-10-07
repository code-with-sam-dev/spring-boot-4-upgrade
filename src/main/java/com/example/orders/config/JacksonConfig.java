package com.example.orders.config;

import static com.fasterxml.jackson.databind.PropertyNamingStrategies.SNAKE_CASE;
import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

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
            Jackson2ObjectMapperBuilder builder) {
        var writer = new LocalDateSerializer(UK_DATE);
        var reader = new LocalDateDeserializer(UK_DATE);
        return builder
                .propertyNamingStrategy(SNAKE_CASE)
                .featuresToDisable(WRITE_DATES_AS_TIMESTAMPS)
                .serializerByType(LocalDate.class, writer)
                .deserializerByType(LocalDate.class, reader)
                .build();
    }
}
