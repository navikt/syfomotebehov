package no.nav.syfo.consumer.aktorregister.domain

data class AktorId(val value: String) {
    private val thirteenDigits = Regex("\\d{13}")

    init {
        if (!thirteenDigits.matches(value)) {
            throw IllegalArgumentException("$value is not a valid aktorId")
        }
    }
}
