package no.nav.syfo.consumer.pdl

data class PdlRequest(
    val query: String,
    val variables: Variables
)

data class Variables(
    val ident: String,
    val grupper: String = IdentType.FOLKEREGISTERIDENT.name,
    val navnHistorikk: Boolean = false
)

enum class IdentType {
    FOLKEREGISTERIDENT,
    AKTORID
}
