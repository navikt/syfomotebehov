package no.nav.syfo.varsel.esyfovarsel

import no.nav.syfo.varsel.esyfovarsel.domain.*
import org.springframework.stereotype.Service

@Service
class EsyfovarselService(private val producer: EsyfovarselProducer) {

    fun sendSvarMotebehovVarselTilNarmesteLeder(narmestelederFnr: String, ansattFnr: String, orgnummer: String) {
        val esyfovarselHendelse = NarmesteLederHendelse(
            narmestelederFnr,
            HendelseType.NL_DIALOGMOTE_SVAR_MOTEBEHOV,
            NarmesteLederVarselData(ansattFnr, orgnummer),
            narmestelederFnr,
            ansattFnr,
            orgnummer
        )
        producer.sendVarselTilEsyfovarsel(esyfovarselHendelse)
    }

    fun sendSvarMotebehovVarselTilArbeidstaker(ansattFnr: String) {
        val esyfovarselHendelse = ArbeidstakerHendelse(
            ansattFnr,
            HendelseType.SM_DIALOGMOTE_SVAR_MOTEBEHOV,
            null,
            ansattFnr,
            null
        )
        producer.sendVarselTilEsyfovarsel(esyfovarselHendelse)
    }
}
