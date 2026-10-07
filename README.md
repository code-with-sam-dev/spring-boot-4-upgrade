# Spring Boot 3.5 to 4.1, one real upgrade

A small orders service, written the way a lot of Spring Boot 3 services are
written, upgraded from **Spring Boot 3.5.16** to **Spring Boot 4.1.1** one
commit at a time, following Spring's own migration guide. Every step is a
branch (and the steps inside a step are tags), so you can check out any
point and see exactly what the build, the tests and the running app do.

Versions are as of October 2026. Spring Boot 4.1.1 was the current release
at the time of recording.

Every error message, test count, log line and HTTP response quoted in this
repository comes from a real run. `scripts/verify.sh` reproduces all of them
and writes them to `evidence/`.

## The app

Package `com.example.orders`:

- `POST /orders` places an order (customer name, items, currency) and asks the
  payments service to authorise the total. `GET /orders/{id}` reads it back.
- Spring MVC, JPA on PostgreSQL 17, Flyway migration `V1__orders.sql`, Actuator.
- A JSON contract: snake_case field names, dates as `dd/MM/yyyy`, and money
  written as one string, `"36.50 GBP"`, by a custom serializer.
- A payments gateway. `OrderService` depends on a one-method `PaymentGateway`
  port in the `order` package; `HttpPaymentGateway` in the `payments` package
  implements it over HTTP. The order side never imports the payments package.
- A rich domain: `OrderLine` knows its subtotal, `Order.place(...)` totals its
  own lines and refuses an order with no items, `Money` is the value object,
  and `CreateOrderRequest.toLines()` maps the request. The service only
  coordinates: build, save, authorise, record the outcome.
- **The payments stub.** So the demo runs on one machine, a stand-in payments
  service lives in `com.example.orders.stub` and is only active with the
  `local-stub` profile (it declines anything over 1000). Without that profile
  it does not exist and `payments.base-url` must point at a real service. We
  chose a profile over a separate container because it is the simplest honest
  option: one jar, clearly fenced off, never on in a real deployment.
- On 3.5 it runs on Undertow, the gateway uses a `RestTemplate`, and the tests use
  `@MockBean`, `@WebMvcTest`, `@AutoConfigureMockMvc`, `@RestClientTest` and
  `TestRestTemplate`.
- Unit and slice tests run with surefire (`./mvnw package`). Integration tests
  against a real PostgreSQL in Testcontainers are named `*IT` and run with
  failsafe (`./mvnw verify`).

## The steps

| Branch or tag | What changes | What happens |
|---|---|---|
| `step-0-boot-3.5` | The app on 3.5.16 | Green. Deprecation warnings for `setConnectTimeout`, `setReadTimeout` and `@MockBean` |
| `step-1-latest-3.5` | Confirm 3.5.16 is the latest 3.5, remove the deprecated calls, add `spring-boot-properties-migrator` | Green, no deprecation warnings |
| `step-2-boot-4.1-parent` | Only the parent version, 3.5.16 to 4.1.1 | Maven cannot even read the POM: no version for the Undertow starter or the Testcontainers 1.x modules |
| `step-3a-tomcat` | Tomcat instead of Undertow, Testcontainers 2 module names | Main code no longer compiles: Jackson 2 imports, `@JsonComponent`, `RestTemplateBuilder` |
| `step-3b-jackson-imports` | `tools.jackson` imports, `@JacksonComponent`, `spring-boot-starter-webmvc` and `-restclient` | Main compiles. Test code does not: test annotations moved |
| `step-3c-test-starters` | `spring-boot-starter-webmvc-test`, `-restclient-test`, new test imports, `@AutoConfigureTestRestTemplate`, and a second end-to-end test written with the new `RestTestClient` | Compiles. Two JSON contract tests fail: the custom `ObjectMapper` bean is ignored |
| `step-3d-jsonmapper` = `step-3-loud-fixed` | Declare the custom mapper as a `JsonMapper` | `./mvnw package` is green and the app starts. The first `POST /orders` returns 500: no migration ran. The integration tests (`./mvnw verify`) do catch it |
| `step-4-flyway-starter` | `spring-boot-starter-flyway` | Migrations run, everything green |
| `step-5a-objectmapper-probe` | The Jackson 3 port of the old `ObjectMapper` bean, on a working database | Probe only: shows what the old bean does on 4.1 |
| `step-5b-customizer` = `step-5-jackson` | `JsonMapperBuilderCustomizer` instead of a mapper bean | Green, and the JSON matches 3.5 |
| `step-6a-api-versioning` | `@GetMapping(version = ...)`, `spring.mvc.apiversion.*`, a deprecation handler | Opt-in |
| `step-6b-http-service-client` | `@HttpExchange` interface, `@ImportHttpServices`, `spring.http.serviceclient.payments.base-url` | Opt-in, `HttpPaymentGateway` uses it instead of a `RestTemplate` |
| `step-6c-nullaway-fails` | JSpecify `@NullMarked` on the payments package, NullAway in the build | Fails to compile on purpose: an unchecked `@Nullable` Spring return |
| `step-6d-nullaway-fixed` | Check the nullable body | Green |
| `step-6e-migrator-removed` = `step-6-opt-in` = `main` | Rename the property the migrator reported, remove the migrator | Green, the finished upgrade |

`evidence/CLAIMS.md` is the ledger: each claim, what 3.5 did, what 4.1 did,
the Spring page that documents it, and whether it was loud, silent or opt-in.

## Code layout for the screen

Every Java file, `pom.xml` and `application.properties` keeps
lines to 64 columns, apart from `package` and `import` lines,
so two files fit side by side in the video. The one exception
is `<artifactId>spring-boot-starter-restclient-test</artifactId>`
(66 columns) in steps 3c to 6a, which cannot be broken.

## Requirements

- Docker (Testcontainers and `compose.yaml` both use `postgres:17-alpine`)
- A JDK 21. For the NullAway steps (6c onwards) it must be **21.0.8 or newer**,
  or any JDK 22+, because NullAway's JSpecify mode needs
  `-XDaddTypeAnnotationsToSymbol=true`. On 21.0.5 the compiler stops with
  "The flag -XDaddTypeAnnotationsToSymbol=true was passed, but it is not
  supported by the running JDK". The recordings used Zulu 21.0.10.
- Ports 8080 (the app) and 5442 (PostgreSQL from `compose.yaml`) free
- Nothing else: the Maven wrapper downloads Maven 3.9.11

## Replay a step by hand

```bash
git checkout step-3-loud-fixed

./mvnw clean package            # unit and slice tests
./mvnw verify                   # plus the Testcontainers integration tests

docker compose up -d --wait     # PostgreSQL on localhost:5442
java -jar target/orders-0.0.1-SNAPSHOT.jar --spring.profiles.active=local-stub

curl -i -X POST localhost:8080/orders -H 'Content-Type: application/json' \
  -d '{"customer_name":"Sarah Thompson","currency":"GBP","items":[{"sku":"KETTLE-01","quantity":1,"unit_price":24.50}]}'

docker compose down -v
```

To see the diff a step introduces: `git diff step-3b-jackson-imports step-3c-test-starters`.

## Replay everything with verify.sh

```bash
scripts/verify.sh            # every step, under 10 minutes with a warm Maven cache
scripts/verify.sh step-3     # one step: step-0 ... step-6, or jars
```

Each step is checked out into a temporary git worktree, so your working copy
is left alone. For each step the script builds, runs the tests, starts the app
against a fresh database, sends the requests with curl and stops it again. It
writes:

- `evidence/<step>/build*.txt`: the build output trimmed to the lines that
  matter (errors, warnings that name a file, test counts, the verdict)
- `evidence/<step>/startup.txt`: the startup lines that matter, and the
  properties migrator report when there is one
- `evidence/<step>/curl.txt`: every request and the full response
- `evidence/step-5/json-compare.txt`: the same JSON from 3.5 and 4.1, byte for byte
- `evidence/jars/`: `BOOT-INF/lib` for 3.5 and 4.1 with names and sizes
- `evidence/.raw/`: the complete Maven and application logs (not committed)

Set `VERIFY_JAVA_HOME` to choose the JDK. The script removes its database
container and worktrees when it finishes.

## Not covered in this migration

This app does not use them, so this upgrade does not exercise them. Read
the matching release notes if yours does.

- Spring Security 7
- Hibernate 7 behaviour changes beyond what this app touches
- gRPC support, new in Spring Boot 4.1
- `@Retryable`, `@ConcurrencyLimit` and `@EnableResilientMethods` (Spring Retry
  dependency management is gone; resilience now lives in Spring Framework 7)
- Virtual threads (`spring.threads.virtual.enabled`, unchanged)
- Liveness and readiness probes now on by default (visible in the curl
  transcripts as `/actuator/health/liveness`, but not discussed)
- Spring Batch, Kafka, AMQP, MongoDB, Redis and Session property moves

## Sources

- Spring Boot 4.0 Migration Guide:
  https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide
- Spring Boot 4.0 and 4.1 Release Notes, same wiki
- Modularizing Spring Boot: https://spring.io/blog/2025/10/28/modularizing-spring-boot/
- API versioning: https://docs.spring.io/spring-framework/reference/web/webmvc-versioning.html
- HTTP service clients: https://docs.spring.io/spring-boot/reference/io/rest-client.html
- Null safety: https://docs.spring.io/spring-framework/reference/core/null-safety.html
