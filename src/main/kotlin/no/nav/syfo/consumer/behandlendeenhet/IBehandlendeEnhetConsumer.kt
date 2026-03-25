package no.nav.syfo.consumer.behandlendeenhet

interface IBehandlendeEnhetConsumer {
    fun getBehandlendeEnhet(
        fnr: String,
        callId: String?,
    ): BehandlendeEnhet
}
