package no.nav.syfo.consumer.pdl

import no.nav.syfo.util.DiagnosticFailure
import no.nav.syfo.util.FailureKind
import no.nav.syfo.util.withFailureDiagnostics
import org.slf4j.spi.LoggingEventBuilder
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(code = HttpStatus.INTERNAL_SERVER_ERROR)
class PdlRequestFailedException(
    message: String = "Request to get Person from PDL Failed",
    cause: Throwable? = null,
    val operation: String = "person_fetch",
    val stage: String = "graphql_response",
    val pdlErrors: List<PdlError>? = null,
    override val upstreamStatus: Int? = null,
) : RuntimeException(message, cause),
    DiagnosticFailure {
    override fun addDiagnosticFields(event: LoggingEventBuilder) {
        event.addKeyValue("pdl_operation", operation)
        pdlErrors?.takeIf { it.isNotEmpty() }?.let { event.addKeyValue("pdl_errors", it) }
    }
}

fun LoggingEventBuilder.withPdlDiagnostics(error: PdlRequestFailedException): LoggingEventBuilder {
    val httpFailure = error.upstreamStatus != null
    addKeyValue("event_type", "pdl_lookup_failed")
        .addKeyValue("operation", error.operation)
        .addKeyValue("upstream", "pdl")
        .addKeyValue("outcome", "failed")
        .addKeyValue("failure_stage", error.stage)
        .addKeyValue("failure_kind", if (httpFailure) FailureKind.HTTP.value else FailureKind.INVALID_RESPONSE.value)
        .addKeyValue(
            "error_code",
            when {
                httpFailure -> "UPSTREAM_HTTP_ERROR"
                !error.pdlErrors.isNullOrEmpty() -> "PDL_GRAPHQL_ERROR"
                else -> "PDL_RESPONSE_MISSING_DATA"
            },
        )
    return withFailureDiagnostics(error)
}
