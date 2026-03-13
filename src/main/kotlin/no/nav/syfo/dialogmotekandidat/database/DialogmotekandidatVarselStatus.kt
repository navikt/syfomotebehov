package no.nav.syfo.dialogmotekandidat.database

import java.time.LocalDateTime
import java.util.UUID

data class DialogmotekandidatVarselStatus(
    val id: UUID,
    val kafkaMeldingUuid: String,
    val fnr: String,
    val type: DialogmotekandidatVarselType,
    val status: String,
    val retryCount: Int,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)
