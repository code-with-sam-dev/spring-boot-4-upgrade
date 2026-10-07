package com.example.orders.money;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import org.springframework.boot.jackson.JsonComponent;

/**
 * Writes Money as one string, "24.50 GBP", not an object.
 */
@JsonComponent
public class MoneySerializer extends JsonSerializer<Money> {

    @Override
    public void serialize(
            Money value,
            JsonGenerator gen,
            SerializerProvider serializers) throws IOException {
        gen.writeString(value.display());
    }
}
