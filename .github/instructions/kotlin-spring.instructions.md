---
applyTo: "**/*.kt"
---

# Kotlin/Spring Boot Development Standards

## Application Structure

Spring Boot 3 application with `@RestController` endpoints and Spring dependency injection. The main entry point is `Application.kt`, with `LocalApplication.kt` in tests for local development.

## Controller Pattern

Endpoints use `@RestController` with `@ProtectedWithClaims` for authentication:

```kotlin
@RestController
@ProtectedWithClaims(
    issuer = TokenXIssuer.TOKENX,
    claimMap = ["acr=Level4", "acr=idporten-loa-high"],
    combineWithOr = true
)
@RequestMapping(value = ["/api/v4/arbeidstaker"])
class MotebehovArbeidstakerControllerV4 @Inject constructor(
    private val contextHolder: TokenValidationContextHolder,
    private val metric: Metric,
    private val motebehovStatusServiceV2: MotebehovStatusServiceV2,
) {
    @GetMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
    fun motebehovStatusArbeidstaker(): MotebehovStatusWithFormValuesDTO {
        val fnr = TokenXUtil.fnrFromIdportenTokenX(contextHolder, clientIds)
        metric.countEndpointRequest("motebehovStatusArbeidstaker")
        return motebehovStatusServiceV2.getStatus(fnr)
    }
}
```

## Database Access (DAO Pattern)

Raw JDBC with `JdbcTemplate` / `NamedParameterJdbcTemplate` — no JPA/ORM:

```kotlin
@Service
@Transactional
@Repository
class MotebehovDAO(
    private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate,
    private val jdbcTemplate: JdbcTemplate
) {
    fun hentMotebehovListeForAktoer(aktoerId: String): List<PMotebehov> {
        return jdbcTemplate.query(
            """
                SELECT m.*, f.* FROM motebehov m
                LEFT JOIN motebehov_form_values f ON m.id = f.motebehov_row_id
                WHERE m.aktoer_id = ?
                ORDER BY m.opprettet_dato ASC
            """,
            motebehovRowMapper,
            aktoerId
        ) ?: emptyList()
    }

    fun lagreMotebehov(motebehov: PMotebehov): Long {
        val params = MapSqlParameterSource()
            .addValue("uuid", motebehov.uuid.toString())
            .addValue("aktoerId", motebehov.aktoerId)
            // ...
        val keyHolder = GeneratedKeyHolder()
        namedParameterJdbcTemplate.update(INSERT_SQL, params, keyHolder)
        return keyHolder.key!!.toLong()
    }
}
```

## Kafka Listeners

Listeners use `@KafkaListener` with manual acknowledgment, active only in `remote` profile:

```kotlin
@Profile("remote")
@KafkaListener(
    topics = [TOPIC_NAME],
    containerFactory = "customListenerContainerFactory"
)
fun listen(record: ConsumerRecord<String, EventType>, ack: Acknowledgment) {
    try {
        service.process(record.value())
        ack.acknowledge()
    } catch (e: Exception) {
        log.error("Error processing message", e)
    }
}
```

## External Service Consumers

REST clients in `consumer/` package use `RestTemplate`/`WebClient` with token exchange:

```kotlin
class BehandlendeEnhetConsumer(
    private val azureAdV2TokenConsumer: AzureAdV2TokenConsumer,
    @Qualifier("restTemplateWithProxy") private val restTemplate: RestTemplate,
    @Value("\${syfobehandlendeenhet.url}") private val baseUrl: String,
    @Value("\${syfobehandlendeenhet.client.id}") private val clientId: String,
) {
    fun getBehandlendeEnhet(fnr: String): BehandlendeEnhet {
        val token = azureAdV2TokenConsumer.getSystemToken(clientId)
        val headers = HttpHeaders().apply {
            setBearerAuth(token)
            set("Nav-Personident", fnr)
        }
        return restTemplate.exchange(url, HttpMethod.GET, HttpEntity<Any>(headers), BehandlendeEnhet::class.java).body!!
    }
}
```

## Configuration

- `application.yaml` — Production config with NAIS env vars
- `src/test/resources/application.yaml` — Test config with local defaults
- Context path: `/syfomotebehov`
- Production profile: `remote` (set via `SPRING_PROFILES_ACTIVE` / Dockerfile JVM opts)

## Boundaries

### ✅ Always

- Use `@ProtectedWithClaims` for endpoint authentication
- Use constructor injection (`@Inject` or Kotlin constructor)
- Use parameterized queries with `JdbcTemplate` (never string concatenation)
- Follow existing DAO pattern (`@Service @Transactional @Repository`)
- Use Flyway migrations for schema changes

### ⚠️ Ask First

- Adding new external service consumers
- Changing authentication configuration
- Adding new Kafka listeners or topics

### 🚫 Never

- Use JPA/Hibernate (project explicitly excludes it)
- Bypass authentication checks
- Use `!!` operator without proper null checks
- Log personal data (fødselsnummer, tokens)
- Commit secrets or credentials
