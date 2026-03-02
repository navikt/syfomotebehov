package no.nav.syfo.consumer.veiledertilgang

interface IVeilederTilgangConsumer {
    fun sjekkVeiledersTilgangTilPersonMedOBO(fnr: String): Boolean
}
