package no.nav.syfo.exception

data class ApiError(
        private val status: Int,
        private val message: String
)
