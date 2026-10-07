package com.example.orders.stub;

import java.math.BigDecimal;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Stands in for the real payments service so the demo runs
 * on one machine. Only active with the local-stub profile;
 * never part of a real deployment. It declines anything
 * over 1000.
 */
@RestController
@Profile("local-stub")
class PaymentsStubController {

    private static final BigDecimal LIMIT =
            new BigDecimal("1000");

    record Authorise(
            long orderId, BigDecimal amount, String currency) {
    }

    record Authorisation(String status, String reference) {
    }

    @PostMapping("/payments/authorisations")
    Authorisation authorise(@RequestBody Authorise request) {
        boolean overLimit =
                request.amount().compareTo(LIMIT) > 0;
        String status = overLimit ? "DECLINED" : "AUTHORISED";
        return new Authorisation(
                status, "pay-" + request.orderId());
    }
}
