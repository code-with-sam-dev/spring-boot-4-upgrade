package com.example.orders.payments;

import java.math.BigDecimal;

/**
 * The request body the payments service expects.
 */
record AuthoriseRequest(
        long orderId, BigDecimal amount, String currency) {
}
