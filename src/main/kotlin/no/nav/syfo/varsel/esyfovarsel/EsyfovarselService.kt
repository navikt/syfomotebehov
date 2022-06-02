package no.nav.syfo.varsel.esyfovarsel

import no.nav.syfo.varsel.esyfovarsel.domain.EsyfovarselHendelse
import no.nav.syfo.varsel.esyfovarsel.domain.HendelseType
import no.nav.syfo.varsel.esyfovarsel.domain.NarmesteLederVarselData
import org.springframework.stereotype.Service

@Service
class EsyfovarselService(private val producer: EsyfovarselProducer) {

    fun sendSvarMotebehovVarselTilNarmesteLeder(narmestelederFnr: String, ansattFnr: String, orgnummer: String) {
        val esyfovarselHendelse = EsyfovarselHendelse(
            narmestelederFnr,
            HendelseType.NL_DIALOGMOTE_SVAR_MOTEBEHOV,
            NarmesteLederVarselData(ansattFnr, orgnummer)
        )
        producer.sendVarselTilEsyfovarsel(esyfovarselHendelse)
    }
}
