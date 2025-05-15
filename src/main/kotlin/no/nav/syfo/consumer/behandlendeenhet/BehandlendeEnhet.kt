package no.nav.syfo.consumer.behandlendeenhet

import java.time.LocalDateTime

data class BehandlendeEnhet(
    val geografiskEnhet: EnhetDTO,
    val oppfolgingsenhetDTO: OppfolgingsenhetDTO?,
)

data class OppfolgingsenhetDTO(
    val enhet: EnhetDTO,
    val createdAt: LocalDateTime,
    val veilederident: String,
)

data class EnhetDTO(
    val enhetId: String,
    val navn: String,
)

fun BehandlendeEnhet.getEnhetId() = oppfolgingsenhetDTO?.enhet?.enhetId ?: geografiskEnhet.enhetId
