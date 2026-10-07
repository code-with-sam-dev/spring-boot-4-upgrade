package com.example.orders.payments;

/**
 * The response body the payments service sends back.
 */
record AuthorisationResponse(String status, String reference) {
}
