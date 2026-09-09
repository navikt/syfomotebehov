# Copilot Instructions for syfomotebehov

---

# Nav Development Standards

These standards apply across Nav projects. Project-specific guidelines follow below.

## Nav Principles

- **Team First**: Autonomous teams with circles of autonomy, supported by Architecture Advice Process
- **Product Development**: Continuous development and product-organized reuse over ad hoc approaches
- **Essential Complexity**: Focus on essential complexity, avoid accidental complexity
- **DORA Metrics**: Measure and improve team performance using DevOps Research and Assessment metrics

## Nav Tech Stack

- **Backend**: Kotlin with Spring Boot 4, PostgreSQL, Apache Kafka
- **Platform**: Nais (Kubernetes on Google Cloud Platform)
- **Auth**: Azure AD, TokenX, ID-porten
- **Observability**: Prometheus, Grafana Loki, Tempo (OpenTelemetry)

## Nav Code Standards

### Kotlin/Spring Boot Patterns

- Spring Boot 4 with `@RestController` and `@ProtectedWithClaims` for auth
- `JdbcTemplate` / `NamedParameterJdbcTemplate` for database access (no ORM)
- Flyway for database migrations
- Kotest DescribeSpec for testing, MockK for mocking
- Spring dependency injection via constructor injection

### Nais Deployment

- Manifests in `nais/` directory
- Required endpoints: `/internal/isAlive`, `/internal/isReady`, `/internal/prometheus`
- OpenTelemetry auto-instrumentation for observability
- Separate dev/prod manifests

---

# Application-Specific Guidelines

## What This Is

A Kotlin/Spring Boot 4 application that stores and serves data about "møtebehov" (dialogue meeting needs) in NAV's sickness follow-up system. Employees (arbeidstaker), employers (arbeidsgiver), and counselors (veileder) each have their own API to view and submit meeting needs. Runs on NAIS (GCP).

## Commands

**Run after all changes:** `./gradlew build`

```bash
./gradlew build              # Build + test
./gradlew test               # Tests only (requires Docker for TestContainers)

# Run a single test class:
./gradlew test --tests "no.nav.syfo.motebehov.api.MotebehovArbeidstakerControllerV4Test"

# Run a single test by description (Kotest):
./gradlew test --tests "no.nav.syfo.motebehov.api.MotebehovArbeidstakerControllerV4Test" -Dkotest.filter.specs="*MotebehovArbeidstakerControllerV4Test"
```

Java 21 required. Uses Gradle with Spring Boot plugin (bootJar produces `app.jar`).

### Local Development

Start via `LocalApplication.kt` in `src/test/kotlin/` — runs on `localhost:8811/syfomotebehov/`. Uses TestContainers PostgreSQL (requires Docker).

## Architecture

### API Layer

Three audience types, each with its own auth mechanism:

| Audience | Path prefix | Auth | Token issuer |
|---|---|---|---|
| Employee (arbeidstaker) | `/api/v4/arbeidstaker` | TokenX (idporten) | `tokenx` |
| Employer (arbeidsgiver) | `/api/v4/arbeidsgiver` | TokenX (idporten) | `tokenx` |
| Counselor (veileder) | `/api/internad/v4/veileder` | Azure AD v2 | `internazureadv2` |

All endpoints are under the context path `/syfomotebehov`. Veileder endpoints identify the person via the `NAV_PERSONIDENT_HEADER` HTTP header.

```kotlin
// ✅ Good - Controller pattern with auth
@RestController
@ProtectedWithClaims(
    issuer = TokenXIssuer.TOKENX,
    claimMap = ["acr=Level4", "acr=idporten-loa-high"],
    combineWithOr = true
)
@RequestMapping(value = ["/api/v4/arbeidstaker"])
class MotebehovArbeidstakerControllerV4 @Inject constructor(
    private val contextHolder: TokenValidationContextHolder,
    // ...
)
```

### Database

- **PostgreSQL** with **Flyway** migrations in `src/main/resources/db/migration/`
- **Raw JDBC** via `JdbcTemplate` / `NamedParameterJdbcTemplate` — no JPA/ORM (JPA auto-config is explicitly excluded)
- DAO classes are annotated `@Service @Transactional @Repository`

```kotlin
// ✅ Good - DAO pattern with JdbcTemplate
@Service
@Transactional
@Repository
class MotebehovDAO(
    private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate,
    private val jdbcTemplate: JdbcTemplate
) {
    fun hentMotebehovListeForAktoer(aktoerId: String): List<PMotebehov> {
        return jdbcTemplate.query(
            "SELECT m.*, f.* FROM motebehov m LEFT JOIN motebehov_form_values f ...",
            motebehovRowMapper,
            aktoerId
        ) ?: emptyList()
    }
}
```

### Kafka

- Aiven Kafka with Avro serialization (Confluent Schema Registry)
- Consumers use manual acknowledgment (`AckMode.MANUAL_IMMEDIATE`)
- Kafka listeners are annotated with `@Profile("remote")` so they don't run in local/test
- Topics: dialogmotekandidat, oppfolgingstilfelle, dialogmote status changes

### External Service Consumers

The `consumer/` package contains REST clients for other NAV microservices (PDL, narmesteleder, behandlendeenhet, brukertilgang, veiledertilgang). These use `RestTemplate`/`WebClient` with Azure AD v2 or TokenX token exchange.

### Runtime and observability

- Application namespace: `team-esyfo`, as declared in `nais/nais-dev.yaml` and
  `nais/nais-prod.yaml`.
- Probes: `/syfomotebehov/internal/isAlive` and
  `/syfomotebehov/internal/isReady`; metrics:
  `/syfomotebehov/internal/prometheus`.
- Alert definitions are in `nais/alerts-gcp.yaml`. Check the maintained team
  routing configuration for alert recipients.
- Business and endpoint metrics are centralized in
  `src/main/kotlin/no/nav/syfo/metric/Metric.kt`. Keep labels bounded and exclude
  person identifiers and other sensitive data.
- Logging is selected by `src/main/resources/logback-spring.xml`, with
  `logback-remote.xml` and `logback-local.xml` for the environment-specific
  appenders. Preserve structured JSON logs in deployed environments.
- Start code exploration in `motebehov/api/`, `motebehov/database/`,
  `consumer/`, and `api/auth/` under `src/main/kotlin/no/nav/syfo/`.

## Testing

- **Framework**: Kotest `DescribeSpec` with JUnit 5 runner + `@ApplyExtension(SpringExtension::class)`
- **Base class**: `IntegrationTest` (extends `DescribeSpec`, clears mocks after each test via `afterTest`)
- **Database**: TestContainers PostgreSQL (auto-started via `LocalApplication` with `@ServiceConnection`)
- **Mocking**: MockK + springmockk (`@MockkBean`), WireMock for HTTP
- **Test data**: Generators in `testhelper/generator/`, constants in `testhelper/UserConstants`
- **DB cleanup**: Use `@Sql(statements = ["DELETE FROM table_name"])` annotations on test classes
- **Assertions**: Mix of Kotest matchers (`shouldBe`) and AssertJ (`assertThat`)

```kotlin
// ✅ Good - Integration test pattern
@TestConfiguration
@SpringBootTest(classes = [LocalApplication::class])
@ApplyExtension(SpringExtension::class)
class MotebehovArbeidstakerControllerV4Test : IntegrationTest() {
    @MockkBean
    lateinit var pdlConsumer: PdlConsumer

    @Autowired
    lateinit var motebehovDAO: MotebehovDAO

    init {
        describe("MotebehovArbeidstakerControllerV4") {
            it("should return motebehov status") {
                // Arrange, Act, Assert
            }
        }
    }
}
```

All integration tests require Docker running for TestContainers.

## Key Conventions

- Domain language is **Norwegian** in code (e.g., `motebehov`, `arbeidstaker`, `veileder`, `oppfolgingstilfelle`)
- Auth is handled via `no.nav.security:token-validation-spring` with `@ProtectedWithClaims` annotations
- The `formSnapshot` pattern stores complete form submissions as JSON blobs for auditing
- Spring profiles: `remote` for production/deployed environments, no profile for local/test
- V3 and V4 API versions coexist (gradual migration)

## Boundaries

### ✅ Always

- Run `./gradlew build` after changes to verify build + tests
- Use parameterized queries for all database access (JdbcTemplate with `?` placeholders)
- Write Kotest DescribeSpec tests for new functionality
- Use Flyway migrations for schema changes (never modify existing migrations)
- Follow existing controller pattern (`@ProtectedWithClaims` + `@RestController`)
- Use constructor injection for all dependencies

### ⚠️ Ask First

- Modifying Kafka consumer configuration or listener setup
- Changing authentication or access control logic
- Modifying production NAIS manifests (`nais/nais-prod.yaml`)
- Adding new API versions or endpoints

### 🚫 Never

- Commit secrets or credentials to git
- Log personal data (fødselsnummer, tokens)
- Modify existing Flyway migration files
- Use string concatenation in SQL queries
- Bypass authentication or access control checks
- Use `!!` operator without proper null checks

## Documentation

Keep temporary working notes in `.local-notes/`, which is ignored by Git.
Maintain durable service documentation in `README.md` and the existing `docs/` layout as
part of the authorized change. Record an ADR for a lasting architectural
tradeoff or a change to an earlier architectural decision, following existing
ADR paths and numbering when present. The task scope determines which docs
need updating; ask only when a material decision or authority is missing.

## Repository guidance

This repository owns `.github/copilot-instructions.md`, applicable files under
`.github/instructions/`, and retained local agents and skills. Update affected
repository guidance together with an authorized change, preserving service
facts, build commands, data rules, and operational constraints.

Portable agents and task workflows come from the selected nav-pilot package.
Use the exact component identities offered by the active session. Check local
and user components for name collisions when a skill is missing or resolves to
unexpected content.
