package no.nav.syfo.varsel.esyfovarsel

import no.nav.syfo.varsel.esyfovarsel.domain.*
import no.nav.syfo.varsel.esyfovarsel.domain.HendelseType.NL_DIALOGMOTE_SVAR_MOTEBEHOV
import no.nav.syfo.varsel.esyfovarsel.domain.HendelseType.SM_DIALOGMOTE_SVAR_MOTEBEHOV
import org.springframework.stereotype.Service

@Service
class EsyfovarselService(private val producer: EsyfovarselProducer) {

    fun sendSvarMotebehovVarselTilNarmesteLeder(narmestelederFnr: String, ansattFnr: String, orgnummer: String) {
        val esyfovarselHendelse = NarmesteLederHendelse(
            type = NL_DIALOGMOTE_SVAR_MOTEBEHOV,
            ferdigstill = false,
            data = null,
            narmesteLederFnr = narmestelederFnr,
            arbeidstakerFnr = ansattFnr,
            orgnummer = orgnummer,
        )
        producer.sendVarselTilEsyfovarsel(esyfovarselHendelse)
    }

    fun sendSvarMotebehovVarselTilArbeidstaker(ansattFnr: String) {
        val esyfovarselHendelse = ArbeidstakerHendelse(
            type = SM_DIALOGMOTE_SVAR_MOTEBEHOV,
            ferdigstill = false,
            data = null,
            arbeidstakerFnr = ansattFnr,
            orgnummer = null,
        )
        producer.sendVarselTilEsyfovarsel(esyfovarselHendelse)
    }

    fun ferdigstillSvarMotebehovForArbeidsgiver(narmestelederFnr: String, ansattFnr: String, orgnummer: String) {
        val esyfovarselHendelse = NarmesteLederHendelse(
            type = NL_DIALOGMOTE_SVAR_MOTEBEHOV,
            ferdigstill = true,
            data = null,
            narmesteLederFnr = narmestelederFnr,
            arbeidstakerFnr = ansattFnr,
            orgnummer = orgnummer,
        )
        producer.sendVarselTilEsyfovarsel(esyfovarselHendelse)
    }

    fun ferdigstillSvarMotebehovForArbeidstaker(ansattFnr: String) {
        val esyfovarselHendelse = ArbeidstakerHendelse(
            type = SM_DIALOGMOTE_SVAR_MOTEBEHOV,
            ferdigstill = true,
            data = null,
            arbeidstakerFnr = ansattFnr,
            orgnummer = null,
        )
        producer.sendVarselTilEsyfovarsel(esyfovarselHendelse)
    }
}
