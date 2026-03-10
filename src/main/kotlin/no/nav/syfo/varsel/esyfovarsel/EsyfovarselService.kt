package no.nav.syfo.varsel.esyfovarsel

import no.nav.syfo.motebehov.Motebehov
import no.nav.syfo.motebehov.MotebehovTilbakemelding
import no.nav.syfo.varsel.esyfovarsel.domain.*
import org.springframework.stereotype.Service

@Service
class EsyfovarselService(private val producer: EsyfovarselProducer) {

    fun sendTilbakemeldingsvarsel(tilbakemelding: MotebehovTilbakemelding, motebehov: Motebehov) {
        if (motebehov.opprettetAvFnr == motebehov.arbeidstakerFnr) {
            val sykmeldtHendelse = ArbeidstakerHendelse(
                type = HendelseType.SM_DIALOGMOTE_MOTEBEHOV_TILBAKEMELDING,
                ferdigstill = false,
                data = VarselDataMotebehovTilbakemelding(tilbakemelding.varseltekst),
                arbeidstakerFnr = motebehov.arbeidstakerFnr,
                orgnummer = motebehov.virksomhetsnummer,
            )

            producer.sendVarselTilEsyfovarsel(sykmeldtHendelse)
        } else {
            val narmestelederHendelse = NarmesteLederHendelse(
                type = HendelseType.NL_DIALOGMOTE_MOTEBEHOV_TILBAKEMELDING,
                ferdigstill = false,
                data = VarselDataMotebehovTilbakemelding(tilbakemelding.varseltekst),
                narmesteLederFnr = motebehov.opprettetAvFnr,
                arbeidstakerFnr = motebehov.arbeidstakerFnr,
                orgnummer = motebehov.virksomhetsnummer,
            )

            producer.sendVarselTilEsyfovarsel(narmestelederHendelse)
        }
    }

    fun ferdigstillSvarMotebehovForArbeidsgiver(narmestelederFnr: String, ansattFnr: String, orgnummer: String) {
        val esyfovarselHendelse = NarmesteLederHendelse(
            type = HendelseType.NL_DIALOGMOTE_SVAR_MOTEBEHOV,
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
            type = HendelseType.SM_DIALOGMOTE_SVAR_MOTEBEHOV,
            ferdigstill = true,
            data = null,
            arbeidstakerFnr = ansattFnr,
            orgnummer = null,
        )
        producer.sendVarselTilEsyfovarsel(esyfovarselHendelse)
    }
}
