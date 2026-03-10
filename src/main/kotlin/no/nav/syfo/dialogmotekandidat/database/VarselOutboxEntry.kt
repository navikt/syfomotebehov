package no.nav.syfo.dialogmotekandidat.database

import no.nav.syfo.varsel.esyfovarsel.domain.EsyfovarselHendelse
import java.time.LocalDateTime
import java.util.UUID

enum class VarselOutboxStatus { PENDING, PROCESSED, SKIPPED }

enum class VarselOutboxRecipientStatus { PENDING, SENT }

data class VarselOutboxEntry(
    val uuid: UUID,
    val kilde: String,
    val payload: String,
    val status: VarselOutboxStatus,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)

data class VarselOutboxRecipientEntry(
    val uuid: UUID,
    val outboxUuid: UUID?,
    val mottakerFnr: String,
    val hendelse: EsyfovarselHendelse,
    val status: VarselOutboxRecipientStatus,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)
