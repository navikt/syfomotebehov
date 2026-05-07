package no.nav.syfo.dialogmotekandidat.database

import java.time.LocalDateTime
import java.util.UUID

data class SvarMotebehovVarselUtsending(
    val id: UUID,
    val kafkaMeldingUuid: String,
    val fnr: String,
    val type: DialogmotekandidatVarselType,
    val status: SvarMotebehovVarselStatus,
    val retryCount: Int,
    val nextRetryAt: LocalDateTime,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)

enum class SvarMotebehovVarselStatus {
    PENDING,
    SENT,
    FAILED,
    SKIPPED,
}
