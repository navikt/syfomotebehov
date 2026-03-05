package no.nav.syfo.dialogmotekandidat.kafka

import no.nav.syfo.util.toNorwegianLocalDateTime
import java.time.OffsetDateTime

data class KafkaDialogmotekandidatEndring(
    val uuid: String,
    val createdAt: OffsetDateTime,
    val personIdentNumber: String,
    val kandidat: Boolean,
    val arsak: String,
)

fun KafkaDialogmotekandidatEndring.localCreatedAt() = this.createdAt.toNorwegianLocalDateTime()