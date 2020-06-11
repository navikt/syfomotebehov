package no.nav.syfo.api.exception

data class ApiError(
    private val status: Int,
    private val message: String
)
