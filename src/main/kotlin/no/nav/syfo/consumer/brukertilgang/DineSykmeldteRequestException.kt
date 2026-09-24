package no.nav.syfo.consumer.brukertilgang

import no.nav.syfo.util.DiagnosticFailure
import no.nav.syfo.util.FailureKind
import no.nav.syfo.util.failureKind

enum class SykmeldtFailureStage(
    val value: String,
    val upstream: String,
) {
    TOKEN_EXCHANGE("token_exchange", "tokenx"),
    UPSTREAM_REQUEST("upstream_request", "dinesykmeldte-backend"),
    RESPONSE_DECODE("response_decode", "dinesykmeldte-backend"),
}

class DineSykmeldteRequestException(
    message: String,
    cause: Throwable? = null,
    val stage: SykmeldtFailureStage = SykmeldtFailureStage.UPSTREAM_REQUEST,
    override val upstreamStatus: Int? = null,
) : RuntimeException(message, cause),
    DiagnosticFailure {
    val failureKind: FailureKind
        get() = if (upstreamStatus != null) FailureKind.HTTP else cause?.failureKind() ?: FailureKind.UNKNOWN
}
