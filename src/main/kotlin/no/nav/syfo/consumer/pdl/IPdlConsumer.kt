package no.nav.syfo.consumer.pdl

interface IPdlConsumer {
    fun person(ident: String): PdlHentPerson?

    fun aktorid(fnr: String): String

    fun fnr(aktorid: String): String

    fun isKode6(fnr: String): Boolean
}
