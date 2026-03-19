---
description: REST API-design for Ktor — URL-konvensjoner, StatusPages-basert feilhåndtering, paginering og input-validering
---
<!-- Managed by esyfo-cli. Do not edit manually. Changes will be overwritten.
     For repo-specific customizations, create your own files without this header. -->
# API Design — REST
Standarder for REST API-design i Nav-applikasjoner bygget med Ktor.
## URL-konvensjoner
```
GET    /api/v1/vedtak              → List vedtak
GET    /api/v1/vedtak/{id}         → Hent enkelt vedtak
POST   /api/v1/vedtak              → Opprett vedtak
PUT    /api/v1/vedtak/{id}         → Oppdater vedtak (full)
PATCH  /api/v1/vedtak/{id}         → Oppdater vedtak (delvis)
DELETE /api/v1/vedtak/{id}         → Slett vedtak
```

### Regler
- Bruk **flertall** for ressursnavn: `/vedtak`, `/saker`, `/brukere`
- Bruk **kebab-case** for sammensatte navn: `/sykmeldinger`, `/oppfolgingsplaner`
- Bruk **path params** for identifikatorer: `/vedtak/{id}`
- Bruk **query params** for filtrering: `/vedtak?status=AKTIV&side=2`
- Maks **3 nivåer** nesting: `/saker/{id}/vedtak` (ikke dypere)

## HTTP-statuskoder
| Kode | Bruksområde |
|------|-------------|
| 200 | Vellykket henting/oppdatering |
| 201 | Ressurs opprettet (med `Location`-header) |
| 204 | Vellykket sletting (ingen body) |
| 400 | Ugyldig request (validering) |
| 401 | Ikke autentisert |
| 403 | Ikke autorisert (mangler tilgang) |
| 404 | Ressurs ikke funnet |
| 409 | Konflikt (duplikat, utdatert versjon) |
| 422 | Ugyldig input som er syntaktisk korrekt |
| 500 | Intern feil |

## Feilhåndtering — Ktor StatusPages
```kotlin
open class ApiError(
    val status: HttpStatusCode,
    val type: ErrorType,
    open val message: String,
    open val path: String? = null,
    val timestamp: Instant = Instant.now(),
)

enum class ErrorType { AUTHENTICATION_ERROR, AUTHORIZATION_ERROR, NOT_FOUND, INTERNAL_SERVER_ERROR, BAD_REQUEST, INVALID_FORMAT, CONFLICT }

sealed class ApiErrorException(message: String, val type: ErrorType, cause: Throwable?) : RuntimeException(message, cause) {
    abstract fun toApiError(path: String): ApiError
    class ForbiddenException(val errorMessage: String = "Forbidden", cause: Throwable? = null, type: ErrorType = ErrorType.AUTHORIZATION_ERROR) : ApiErrorException(errorMessage, type, cause) {
        override fun toApiError(path: String) = ApiError(HttpStatusCode.Forbidden, type, errorMessage, path)
    }
    class BadRequestException(val errorMessage: String = "Bad Request", cause: Throwable? = null, type: ErrorType = ErrorType.BAD_REQUEST) : ApiErrorException(errorMessage, type, cause) {
        override fun toApiError(path: String) = ApiError(HttpStatusCode.BadRequest, type, errorMessage, path)
    }
    class NotFoundException(val errorMessage: String = "Not Found", cause: Throwable? = null, type: ErrorType = ErrorType.NOT_FOUND) : ApiErrorException(errorMessage, type, cause) {
        override fun toApiError(path: String) = ApiError(HttpStatusCode.NotFound, type, errorMessage, path)
    }
    class InternalServerErrorException(val errorMessage: String = "Internal Server Error", cause: Throwable? = null, type: ErrorType = ErrorType.INTERNAL_SERVER_ERROR) : ApiErrorException(errorMessage, type, cause) {
        override fun toApiError(path: String) = ApiError(HttpStatusCode.InternalServerError, type, errorMessage, path)
    }
    class UnauthorizedException(val errorMessage: String = "Unauthorized", cause: Throwable? = null, type: ErrorType = ErrorType.AUTHORIZATION_ERROR) : ApiErrorException(errorMessage, type, cause) {
        override fun toApiError(path: String) = ApiError(HttpStatusCode.Unauthorized, type, errorMessage, path)
    }
}

fun Application.installStatusPages() {
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            logException(call, cause)
            val apiError = determineApiError(cause, call.request.path())
            call.respond(apiError.status, apiError)
        }
    }
}

fun determineApiError(cause: Throwable, path: String): ApiError = when (cause) {
    is BadRequestException -> cause.toApiError(path)
    is NotFoundException -> cause.toApiError(path)
    is ApiErrorException -> cause.toApiError(path)
    else -> ApiError(HttpStatusCode.InternalServerError, ErrorType.INTERNAL_SERVER_ERROR, cause.message ?: "Internal server error", path)
}

fun BadRequestException.toApiError(path: String?): ApiError {
    val rootCause = this.rootCause()
    return if (rootCause is MissingFieldException) {
        ApiErrorException.BadRequestException("Invalid request body. Missing required field: ${rootCause.fieldName}", type = ErrorType.INVALID_FORMAT).toApiError(path ?: "")
    } else {
        ApiError(status = HttpStatusCode.BadRequest, type = ErrorType.BAD_REQUEST, message = this.message ?: "Bad request", path = path)
    }
}

fun NotFoundException.toApiError(path: String?): ApiError = ApiError(
    status = HttpStatusCode.NotFound, type = ErrorType.NOT_FOUND, message = this.message ?: "Not found", path = path,
)

fun Throwable.rootCause(): Throwable {
    var root: Throwable = this
    while (root.cause != null && root.cause != root) root = root.cause!!
    return root
}

private fun logException(call: ApplicationCall, cause: Throwable) {
    val callId = call.callId
    val logMessage = "Caught exception, callId=$callId"
    val log = call.application.log
    when (cause) {
        is ApiErrorException -> log.warn(logMessage, cause)
        else -> log.error(logMessage, cause)
    }
}

fun Application.apiModule() {
    installCallId()
    installContentNegotiation()
    installStatusPages()
    // ... routing
}
```

```json
{
  "status": 404,
  "type": "NOT_FOUND",
  "message": "Vedtak med id 550e8400 finnes ikke",
  "path": "/api/v1/vedtak/550e8400",
  "timestamp": "2025-01-15T10:30:00Z"
}
```

## Paginering

```kotlin
@Serializable
data class PaginatedResponse<T>(
    val innhold: List<T>,
    val side: Int,
    val antallPerSide: Int,
    val totaltAntall: Long,
    val totaltAntallSider: Int,
)

get("/api/v1/vedtak") {
    val side = call.queryParameters["side"]?.toIntOrNull() ?: 0
    val antall = call.queryParameters["antall"]?.toIntOrNull() ?: 20
    require(antall <= 100) { "Maks 100 per side" }
    val result = vedtakService.findAll(offset = side * antall, limit = antall)
    call.respond(result)
}
```

```json
{
  "innhold": [...],
  "side": 0,
  "antallPerSide": 20,
  "totaltAntall": 142,
  "totaltAntallSider": 8
}
```

## Input-validering

```kotlin
@Serializable
data class CreateVedtakRequest(val brukerId: String, val beskrivelse: String? = null, val type: VedtakType)

post("/api/v1/vedtak") {
    val request = call.receive<CreateVedtakRequest>()
    if (request.brukerId.isBlank()) throw ApiErrorException.BadRequestException("brukerId kan ikke være tom")
    request.beskrivelse?.let { if (it.length > 500) throw ApiErrorException.BadRequestException("beskrivelse maks 500 tegn") }
    val vedtak = vedtakService.create(request)
    call.response.header("Location", "/api/v1/vedtak/${vedtak.id}")
    call.respond(HttpStatusCode.Created, vedtak.toDTO())
}
```

## Versjonering

- Bruk URL-versjonering: `/api/v1/...`
- Bump versjon kun ved breaking changes
- Støtt gammel versjon i overgangsperiode
- Dokumenter endringer i changelog

## Grenser

### ✅ Alltid
- Flertall for ressursnavn
- Strukturert ApiError via StatusPages
- Valider all input
- Location-header ved 201

### ⚠️ Spør først
- Nye API-versjoner (breaking changes)
- Endring av eksisterende kontrakt
- Asynkrone operasjoner (202 Accepted)

### 🚫 Aldri
- Verb i URL-er (`/getVedtak`, `/createSak`)
- PII i URL-er eller query params (FNR, navn)
- 200 med feilmelding i body
- Ukonsistent navngiving mellom endepunkter
- Kaste exceptions som ikke fanges av StatusPages uten bevisst valg
