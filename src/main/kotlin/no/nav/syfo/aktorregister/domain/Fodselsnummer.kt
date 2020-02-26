package no.nav.syfo.aktorregister.domain

data class Fodselsnummer(val value: String) {
    private val elevenDigits = Regex("\\d{11}")

    init {
        if (!elevenDigits.matches(value)) {
            throw IllegalArgumentException("$value is not a valid fnr")
        }
    }
}
