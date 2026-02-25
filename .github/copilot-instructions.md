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

- **Backend**: Kotlin with Spring Boot 3, PostgreSQL, Apache Kafka
- **Platform**: Nais (Kubernetes on Google Cloud Platform)
- **Auth**: Azure AD, TokenX, ID-porten
- **Observability**: Prometheus, Grafana Loki, Tempo (OpenTelemetry)

## Nav Code Standards

### Kotlin/Spring Boot Patterns

- Spring Boot 3 with `@RestController` and `@ProtectedWithClaims` for auth
- `JdbcTemplate` / `NamedParameterJdbcTemplate` for database access (no ORM)
- Flyway for database migrations
- Kotest DescribeSpec for testing, MockK for mocking
- Spring dependency injection via constructor injection

### Nais Deployment

- Manifests in `nais/` directory
- Required endpoints: `/internal/isAlive`, `/internal/isReady`, `/internal/prometheus`
- OpenTelemetry auto-instrumentation for observability
- Separate dev/prod manifests

### Writing Effective Agents

Based on [GitHub's analysis of 2,500+ repositories](https://github.blog/ai-and-ml/github-copilot/how-to-write-a-great-agents-md-lessons-from-over-2500-repositories/), follow these patterns when creating or updating agents in `.github/agents/`:

**Structure (in order):**

1. **Frontmatter** - Name and description in YAML
2. **Persona** - One sentence: who you are and what you specialize in
3. **Commands** - Executable commands early, with flags and expected output
4. **Related Agents** - Table of agents to delegate to
5. **Core Content** - Code examples over explanations (show, don't tell)
6. **Boundaries** - Three-tier system at the end

**Three-Tier Boundaries:**

```markdown
## Boundaries

### ✅ Always
- Run `./gradlew build` after changes
- Use parameterized queries

### ⚠️ Ask First
- Modifying production configs
- Changing auth mechanisms

### 🚫 Never
- Commit secrets to git
- Skip input validation
```

**Key Principles:**

- **Commands early**: Put executable commands near the top, not buried at the bottom
- **Code over prose**: Show real code examples, not descriptions of what code should do
- **Specific stack**: Include versions and specifics (Java 21, Kotest DescribeSpec, Spring Boot 3)
- **Actionable boundaries**: "Never commit secrets" not "I cannot access secrets"

---

# Application-Specific Guidelines

## What This Is

A Kotlin/Spring Boot 3 application that stores and serves data about "møtebehov" (dialogue meeting needs) in NAV's sickness follow-up system. Employees (arbeidstaker), employers (arbeidsgiver), and counselors (veileder) each have their own API to view and submit meeting needs. Runs on NAIS (GCP).

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

## Keeping Copilot Config in Sync

When making changes that affect the patterns described in `.github/` config files (agents, instructions, skills), **suggest** updating the relevant files — but do not update them automatically.

Examples of changes that should trigger a suggestion:
- Upgrading or replacing frameworks (e.g., Spring Boot version bump, switching to Exposed/Ktor)
- Changing test framework or patterns
- Adding/removing authentication mechanisms
- Changing database access patterns
- Adding new Kafka topics or changing consumer setup
- Modifying build tooling or commands

Format the suggestion as: *"This change affects patterns documented in `.github/instructions/kotlin-spring.instructions.md` — want me to update it?"*
