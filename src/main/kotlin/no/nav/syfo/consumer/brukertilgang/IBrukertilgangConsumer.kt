package no.nav.syfo.consumer.brukertilgang

interface IBrukertilgangConsumer {
    fun hasAccessToAnsatt(ansattFnr: String): Boolean
}
