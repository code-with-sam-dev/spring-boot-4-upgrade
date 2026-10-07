package com.example.orders.config;

import static java.time.ZoneOffset.UTC;

import java.net.URI;
import java.time.ZonedDateTime;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.accept.ApiVersionDeprecationHandler;
import org.springframework.web.accept.StandardApiVersionDeprecationHandler;

/**
 * Version 1 of the orders API still works, but every
 * response says it is deprecated and when it goes away.
 */
@Configuration
public class ApiVersionConfig {

    private static final ZonedDateTime DEPRECATED_ON =
            ZonedDateTime.of(2026, 10, 1, 0, 0, 0, 0, UTC);
    private static final ZonedDateTime SUNSET_ON =
            ZonedDateTime.of(2027, 4, 1, 0, 0, 0, 0, UTC);
    private static final URI MIGRATION_GUIDE = URI.create(
            "https://example.com/orders-api/v2-migration");

    @Bean
    public ApiVersionDeprecationHandler deprecationHandler() {
        StandardApiVersionDeprecationHandler handler =
                new StandardApiVersionDeprecationHandler();
        handler.configureVersion("1")
                .setDeprecationDate(DEPRECATED_ON)
                .setSunsetDate(SUNSET_ON)
                .setDeprecationLink(MIGRATION_GUIDE);
        return handler;
    }
}
