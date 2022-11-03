package no.nav.syfo.consumer.pdl

data class PdlIdenterResponse(
    val errors: List<PdlError>?,
    val data: PdlHentIdenter?
)

data class PdlHentIdenter(
    val hentIdenter: PdlIdenter
)

data class PdlIdenter(
    val identer: List<PdlIdent>
)

data class PdlIdent(
    val ident: String
)
