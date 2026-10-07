package com.example.orders.payments;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

/**
 * The payments service, described as an interface. Spring
 * generates the client; the base URL comes from
 * spring.http.serviceclient.payments.base-url.
 */
@HttpExchange("/payments")
interface PaymentsApi {

    @PostExchange("/authorisations")
    ResponseEntity<AuthorisationResponse> authorise(
            @RequestBody AuthoriseRequest request);
}
