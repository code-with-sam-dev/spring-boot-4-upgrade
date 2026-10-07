# Claim ledger

Recorded 7 October 2026 (regenerated after the domain refactor and the 64 column reformat) with Spring Boot 3.5.16 and 4.1.1, Zulu JDK 21.0.10,
PostgreSQL 17 (alpine). Every result below is quoted from a transcript in this
folder; the file is named in the last column. Re-run with `scripts/verify.sh`.

Kinds: **loud** means the build, the tests or startup stop you. **silent** means
compile, the unit tests and startup all pass but behaviour changed. **opt-in**
means nothing breaks and Boot 4 offers something new.

| # | Claim | 3.5 result | 4.1 result | Spring source | Kind | Transcript |
|---|---|---|---|---|---|---|
| 1 | The 3.5 compiler already warns about the APIs 4.0 deletes | `setConnectTimeout`, `setReadTimeout` and `@MockBean` reported "deprecated and marked for removal" | n/a | MG, "Upgrade to the latest 3.5.x" | warning | step-0/deprecation.txt |
| 2 | 3.5.16 is the latest 3.5 | Maven Central lists 3.5.14, 3.5.15, 3.5.16 | n/a | spring.io/projects/spring-boot | fact | step-1/build.txt |
| 3 | There is no Undertow starter in Boot 4 | `spring-boot-starter-undertow` 3.5.16: HTTP 200 | 4.0.0 and 4.1.1: HTTP 404. Maven: "'dependencies.dependency.version' for org.springframework.boot:spring-boot-starter-undertow:jar is missing" | MG, "Undertow" | loud | step-2/build.txt |
| 4 | Boot 4.1.1 manages Testcontainers 2, which renamed its modules | `org.testcontainers:postgresql` and `junit-jupiter` resolve | "version ... is missing" for both; `testcontainers-postgresql` and `testcontainers-junit-jupiter` work | Boot 4.1.1 BOM (testcontainers 2.0.5); Testcontainers 2.0.0 release notes, https://github.com/testcontainers/testcontainers-java/releases/tag/2.0.0 (not Spring) | loud | step-2/build.txt |
| 5 | Jackson 3 moves to `tools.jackson` and `@JsonComponent` becomes `@JacksonComponent` | compiles | "package com.fasterxml.jackson.databind does not exist", "cannot find symbol ... JsonComponent" | MG, "Jackson 3" | loud | step-3/3a-tomcat-build.txt |
| 6 | `RestTemplateBuilder` moved to its own module | `org.springframework.boot.web.client` | "package org.springframework.boot.web.client does not exist"; fixed with `spring-boot-starter-restclient` and `org.springframework.boot.restclient` | MG and https://spring.io/blog/2025/10/28/modularizing-spring-boot/ | loud | step-3/3a-tomcat-build.txt |
| 7 | `RestTemplateBuilder.rootUri` is deprecated in 4.1 | not deprecated | "rootUri(java.lang.String) ... has been deprecated and marked for removal"; `baseUri` replaces it | Boot 4.1 API docs (seen as a compiler warning) | warning | step-3/3b-jackson-build.txt |
| 8 | `@MockBean` is gone in 4.0 | deprecated, works | "package org.springframework.boot.test.mock.mockito does not exist", "symbol: class MockBean" (only when step 1 is skipped) | MG, "Testing"; https://docs.spring.io/spring-boot/reference/testing/spring-boot-applications.html | loud | step-2/mockbean-if-step-1-skipped.txt |
| 9 | Test slices and helpers moved to modules | `org.springframework.boot.test.autoconfigure.web.servlet`, `...web.client`, `org.springframework.boot.test.web.client` | "does not exist" for all three; `WebMvcTest`, `AutoConfigureMockMvc` now in `org.springframework.boot.webmvc.test.autoconfigure`, `RestClientTest` in `org.springframework.boot.restclient.test.autoconfigure`, `TestRestTemplate` in `org.springframework.boot.resttestclient` | MG, "Testing" | loud | step-3/3b-jackson-build.txt |
| 10 | `TestRestTemplate` is not provided unless asked for | autowired by `@SpringBootTest(webEnvironment=...)` | needs `@AutoConfigureTestRestTemplate` (added in step 3c, the IT then runs) | MG, "Testing" | loud | step-3/build-verify.txt |
| 10b | Boot 4 adds `RestTestClient` as a fluent alternative | n/a | `OrdersRestTestClientIT` makes the same end-to-end assertion as `OrdersHttpIT.placesAnOrderEndToEnd` with `@AutoConfigureRestTestClient` and `client.post().uri("/orders").contentType(APPLICATION_JSON).body(...).exchange().expectStatus().isCreated().expectBody().jsonPath("$.payment_status").isEqualTo("AUTHORISED")`; passes from step 4 on | MG, "Testing"; https://docs.spring.io/spring-boot/reference/testing/spring-boot-applications.html | opt-in | step-4/build.txt, step-6/build.txt |
| 11 | A user `ObjectMapper` bean no longer shapes HTTP JSON | snake_case, `"placed_on":"07/10/2026"` | camelCase, `"placedOn":"2026-10-07"`; the snake_case `POST /orders` gets 400. Caught here by two `@WebMvcTest` contract tests ("Status expected:<201> but was:<400>", "No value at JSON path \"$.customer_name\""). Without such tests it would ship | MG, "Jackson 3": define a `JsonMapper` | silent in general, loud in this suite | step-3/3c-test-starters-build.txt, step-5/json-compare.txt |
| 12 | `JsonMapperBuilderCustomizer` restores the exact wire format | baseline body | byte for byte IDENTICAL to 3.5 for `GET /orders/1` | MG, "Jackson 3" | migration | step-5/json-compare.txt |
| 13 | `flyway-core` alone no longer runs migrations | "Successfully applied 1 migration" at startup | no Flyway line at all; `./mvnw package` BUILD SUCCESS with 29 tests; app starts; `POST /orders` returns 500, log: `relation "orders" does not exist` | MG, "Modules" / modularisation blog | silent (unit tests and startup); the Testcontainers ITs catch it: 10 run, 3 failures, 5 errors | step-3/build-package.txt, step-3/curl.txt, step-3/build-verify.txt |
| 14 | `spring-boot-starter-flyway` fixes it | n/a | "Successfully applied 1 migration", `POST /orders` 201, 39 tests green (29 unit and slice, 10 integration) | MG | migration | step-4/startup.txt, step-4/curl.txt |
| 15 | The properties migrator reports renamed keys | nothing reported on 3.5 | "Key: server.error.include-message ... Reason: Replacement key 'spring.web.error.include-message' uses an incompatible target type". It did NOT remap it: error bodies lost `message` until the key was renamed in step 6e | MG, "properties migrator" | silent until renamed | step-3/startup.txt, step-4/curl.txt, step-6/curl.txt |
| 16 | Boot's single autoconfigure jar is split by technology | 7 `spring-boot*` jars, `spring-boot-autoconfigure` 2,088,460 bytes | 26 `spring-boot*` jars, `spring-boot-autoconfigure` 372,740 bytes; 84 jars total on 3.5, 85 on 4.1 | https://spring.io/blog/2025/10/28/modularizing-spring-boot/ | fact | jars/summary.txt |
| 17 | API versioning from one attribute and two properties | n/a | no header: v1; `X-Version: 2`: the v2 summary; `X-Version: 3`: 400 "Invalid API version: '3.0.0'."; `X-Version: abc`: 400 "Invalid API version: 'abc'." | https://docs.spring.io/spring-framework/reference/web/webmvc-versioning.html, https://docs.spring.io/spring-boot/reference/web/servlet.html | opt-in | step-6/curl.txt |
| 18 | A deprecated version is announced in headers | n/a | `Deprecation: @1790812800`, `Sunset: Thu, 1 Apr 2027 00:00:00 GMT`, `Link: <...>; rel="deprecation"`. Needs an `ApiVersionDeprecationHandler` bean, there is no property for it | Framework versioning docs | opt-in | step-6/curl.txt |
| 19 | `@HttpExchange` plus `@ImportHttpServices` plus `spring.http.serviceclient.<group>.base-url` replaces a hand-built client | `RestTemplate` inside `HttpPaymentGateway` | `HttpPaymentGateway` calls the generated `PaymentsApi`, `POST /orders` 201 AUTHORISED, all ITs green | https://docs.spring.io/spring-boot/reference/io/rest-client.html | opt-in | step-6/curl.txt, step-6/build.txt |
| 20 | NullAway rejects an unchecked nullable Spring return in a `@NullMarked` package | n/a | "[NullAway] dereferenced expression 'response' is @Nullable" for `response.status()` where `response = api.authorise(request).getBody()` | https://docs.spring.io/spring-framework/reference/core/null-safety.html | opt-in | step-6/6c-nullaway-build.txt |
| 21 | Liveness group | `/actuator/health/liveness` 404 on 3.5 (see curl) | 200 `{"status":"UP"}`, health lists `"groups":["liveness","readiness"]` | MG | silent default (not covered on camera) | step-0/curl.txt, step-3/curl.txt |

MG = https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide

## Things that did not behave the way the docs led us to expect

- **The properties migrator did not migrate `server.error.include-message`.** The
  guide says it temporarily migrates properties at runtime. For this key it
  reported "uses an incompatible target type" and left it unapplied, so error
  responses silently lost their `message` field until the key was renamed.
- **Version deprecation headers land on unversioned endpoints too.** `POST /orders`
  has no `version` attribute, resolves to the default version 1, and so carries
  the `Deprecation`, `Sunset` and `Link` headers meant for `GET` version 1. So
  does the 404 for an unknown order.
- **The error `message` text is not the same as 3.5 after the rename.** On 3.5
  the 404 body said `"message":"Not Found"`; on 4.1 with
  `spring.web.error.include-message=always` it says `"message":"No order with id 999"`.
  Recorded, not investigated.
- **NullAway's JSpecify mode needs JDK 21.0.8+ or 22+.** Corretto 21.0.5 failed
  with "The flag -XDaddTypeAnnotationsToSymbol=true was passed, but it is not
  supported by the running JDK".
