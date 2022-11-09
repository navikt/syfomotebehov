package no.nav.syfo.motebehov.historikk

import no.nav.syfo.consumer.pdl.PdlConsumer
import no.nav.syfo.consumer.pdl.fullName
import no.nav.syfo.motebehov.Motebehov
import no.nav.syfo.motebehov.MotebehovService
import org.springframework.stereotype.Service
import java.util.stream.Collectors
import javax.inject.Inject

@Service
class HistorikkService @Inject constructor(
    private val motebehovService: MotebehovService,
    private val pdlConsumer: PdlConsumer
) {
    fun hentHistorikkListe(arbeidstakerFnr: String): List<Historikk> {
        val motebehovListe = motebehovService.hentMotebehovListe(arbeidstakerFnr)

        val historikkListe = hentOpprettetMotebehov(motebehovListe)
        historikkListe.addAll(hentBehandlendeMotebehovHistorikk(motebehovListe))
        return historikkListe
    }

    private fun hentOpprettetMotebehov(motebehovListe: List<Motebehov>): MutableList<Historikk> {
        return motebehovListe.map {
            Historikk(
                opprettetAv = it.opprettetAv,
                tekst = "${getNameOfCreatedBy(it)}$HAR_SVART_PAA_MOTEBEHOV",
                tidspunkt = it.opprettetDato
            )
        }.toMutableList()
    }

    private fun getNameOfCreatedBy(motebehov: Motebehov): String {
        val createdByFnr = pdlConsumer.fnr(motebehov.opprettetAv)
        return pdlConsumer.person(createdByFnr)?.fullName() ?: ""
    }

    private fun hentBehandlendeMotebehovHistorikk(motebehovListe: List<Motebehov>): List<Historikk> {
        return motebehovListe
            .stream()
            .filter { motebehov -> !motebehov.behandletVeilederIdent.isNullOrEmpty() && motebehov.behandletTidspunkt != null }
            .map { motebehov ->
                Historikk(
                    tekst = MOTEBEHOVET_BLE_LEST_AV + motebehov.behandletVeilederIdent,
                    tidspunkt = motebehov.behandletTidspunkt!!
                )
            }
            .collect(Collectors.toList())
    }

    companion object {
        const val HAR_SVART_PAA_MOTEBEHOV = " har svart på møtebehov"
        const val MOTEBEHOVET_BLE_LEST_AV = "Møtebehovet ble behandlet av "
    }
}
