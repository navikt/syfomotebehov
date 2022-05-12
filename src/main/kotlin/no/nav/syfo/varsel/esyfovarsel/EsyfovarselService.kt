package no.nav.syfo.varsel.esyfovarsel

import no.nav.syfo.varsel.esyfovarsel.domain.EsyfovarselHendelse
import no.nav.syfo.varsel.esyfovarsel.domain.HendelseType
import no.nav.syfo.varsel.esyfovarsel.domain.NarmesteLederVarselData
import org.springframework.stereotype.Service

@Service
class EsyfovarselService(private val producer: EsyfovarselProducer) {

    fun sendSvarMotebehovVarselTilNarmesteLeder(narmestelederFnr: String, ansattFnr: String, orgnummer: String) {
        val esyfovarselHendelse = EsyfovarselHendelse(
            mottakerFnr = narmestelederFnr,
            type = HendelseType.NL_DIALOGMOTE_SVAR_MOTEBEHOV,
            data = NarmesteLederVarselData(ansattFnr, orgnummer)
        )
        producer.sendVarselTilEsyfovarsel(esyfovarselHendelse)
    }

    fun sendSvarMotebehovVarselTilArbeidstaker(arbeidstakerFnr: String) {
        val esyfovarselHendelse = EsyfovarselHendelse(
            mottakerFnr = arbeidstakerFnr,
            type = HendelseType.SM_DIALOGMOTE_SVAR_MOTEBEHOV,
        )
        producer.sendVarselTilEsyfovarsel(esyfovarselHendelse)
    }
}
