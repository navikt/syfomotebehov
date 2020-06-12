package no.nav.syfo.consumer.aktorregister.domain

data class IdentinfoListe(
    val identer: List<Identinfo>,
    val feilmelding: String? = null
)
