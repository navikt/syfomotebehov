package no.nav.syfo.consumer.aktorregister.domain

data class Identinfo(
    val ident: String,
    val identgruppe: String,
    val gjeldende: Boolean = false
)
