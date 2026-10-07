package com.example.orders;

/**
 * Request bodies shared by the tests. The JSON contract is
 * snake_case.
 */
public final class TestOrders {

    public static final String SARAH = """
            {
              "customer_name": "Sarah Thompson",
              "currency": "GBP",
              "items": [
                { "sku": "KETTLE-01", "quantity": 1,
                  "unit_price": 24.50 },
                { "sku": "MUG-02", "quantity": 2,
                  "unit_price": 6.00 }
              ]
            }
            """;

    public static final String JAMES_LARGE = """
            {
              "customer_name": "James Okafor",
              "currency": "GBP",
              "items": [
                { "sku": "SOFA-09", "quantity": 1,
                  "unit_price": 1499.00 }
              ]
            }
            """;

    private TestOrders() {
    }
}
