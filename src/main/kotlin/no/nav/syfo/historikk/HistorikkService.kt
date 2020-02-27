package no.nav.syfo.historikk

import no.nav.syfo.aktorregister.AktorregisterConsumer
import no.nav.syfo.aktorregister.domain.AktorId
import no.nav.syfo.aktorregister.domain.Fodselsnummer
import no.nav.syfo.domain.rest.*
import no.nav.syfo.pdl.PdlConsumer
import no.nav.syfo.pdl.fullName
import no.nav.syfo.service.MotebehovService
import no.nav.syfo.service.VeilederOppgaverService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClientException
import java.util.*
import java.util.stream.Collectors
import javax.inject.Inject

@Service
class HistorikkService @Inject constructor(
        private val aktorregisterConsumer: AktorregisterConsumer,
        private val motebehovService: MotebehovService,
        private val pdlConsumer: PdlConsumer,
        private val veilederOppgaverService: VeilederOppgaverService
) {
    fun hentHistorikkListe(arbeidstakerFnr: String): List<Historikk> {
        val historikkListe = hentOpprettetMotebehov(arbeidstakerFnr)
        historikkListe.addAll(hentLesteMotebehovHistorikk(arbeidstakerFnr))
        return historikkListe
    }

    private fun hentOpprettetMotebehov(arbeidstakerFnr: String): MutableList<Historikk> {
        val motebehovListe = motebehovService.hentMotebehovListe(Fodselsnummer(arbeidstakerFnr))

        return motebehovListe.map {
            Historikk(
                    opprettetAv = it.opprettetAv,
                    tekst = "${getNameOfCreatedBy(it)}$HAR_SVART_PAA_MOTEBEHOV",
                    tidspunkt = it.opprettetDato
            )
        }.toMutableList()
    }

    private fun getNameOfCreatedBy(motebehov: Motebehov): String {
        val createdByFnr = Fodselsnummer(aktorregisterConsumer.getFnrForAktorId(AktorId(motebehov.opprettetAv)))
        return pdlConsumer.person(createdByFnr)?.fullName() ?: ""
    }

    private fun hentLesteMotebehovHistorikk(sykmeldtFnr: String): List<Historikk> {
        return try {
            veilederOppgaverService.getVeilederoppgave(sykmeldtFnr)
                    .stream()
                    .filter { veilederOppgave: VeilederOppgave -> veilederOppgave.type == "MOTEBEHOV_MOTTATT" && veilederOppgave.status == "FERDIG" }
                    .map { veilederOppgave: VeilederOppgave ->
                        Historikk(
                                tekst = MOTEBEHOVET_BLE_LEST_AV + veilederOppgave.sistEndretAv,
                                tidspunkt = veilederOppgave.sistEndretAsLocalDateTime
                        )
                    }
                    .collect(Collectors.toList())
        } catch (e: RestClientException) {
            LOG.error("Klarte ikke hente ut varselhistorikk på leste møtebehov")
            ArrayList()
        }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(HistorikkService::class.java)
        const val HAR_SVART_PAA_MOTEBEHOV = " har svart på møtebehov"
        const val MOTEBEHOVET_BLE_LEST_AV = "Møtebehovet ble lest av "
    }

}
