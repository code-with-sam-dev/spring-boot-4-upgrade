package com.example.orders.money;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

import org.springframework.boot.jackson.JacksonComponent;

/**
 * Writes Money as one string, "24.50 GBP", not an object.
 */
@JacksonComponent
public class MoneySerializer extends ValueSerializer<Money> {

    @Override
    public void serialize(
            Money value,
            JsonGenerator gen,
            SerializationContext context) {
        gen.writeString(value.display());
    }
}
