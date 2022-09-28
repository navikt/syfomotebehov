package no.nav.syfo.oppfolgingstilfelle.kafka.domain

import no.nav.syfo.util.toNorwegianLocalDateTime
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime

data class KafkaOppfolgingstilfellePerson(
    val uuid: String,
    val createdAt: OffsetDateTime,
    val personIdentNumber: String,
    val oppfolgingstilfelleList: List<KafkaOppfolgingstilfelle>,
    val referanseTilfelleBitUuid: String,
    val referanseTilfelleBitInntruffet: OffsetDateTime
)

data class KafkaOppfolgingstilfelle(
    val arbeidstakerAtTilfelleEnd: Boolean,
    val start: LocalDate,
    val end: LocalDate,
    val virksomhetsnummerList: List<String>
)

fun KafkaOppfolgingstilfellePerson.previouslyProcessed(
    lastUpdatedAt: LocalDateTime?
) = lastUpdatedAt?.let { it ->
    this.createdAt.toNorwegianLocalDateTime().isBefore(it)
} ?: false
