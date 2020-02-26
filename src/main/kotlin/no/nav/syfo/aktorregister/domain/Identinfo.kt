package no.nav.syfo.aktorregister.domain

data class Identinfo(
        val ident: String,
        val identgruppe: String,
        val gjeldende: Boolean = false
)
