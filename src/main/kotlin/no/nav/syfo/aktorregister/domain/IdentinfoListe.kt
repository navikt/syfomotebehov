package no.nav.syfo.aktorregister.domain

data class IdentinfoListe(
        val identer: List<Identinfo>,
        val feilmelding: String? = null
)
