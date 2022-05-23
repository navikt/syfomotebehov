package no.nav.syfo.varsel.esyfovarsel

import no.nav.syfo.varsel.esyfovarsel.domain.EsyfovarselPlanlagtVarsel
import no.nav.syfo.varsel.esyfovarsel.domain.HendelseType
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class EsyfovarselPlanleggingService(private val producer: EsyfovarselPlanleggingProducer) {

    fun planleggSvarMotebehovVarselTilNarmesteLeder(varselDato: LocalDate, arbeidstakerFnr: String, ansattFnr: String, orgnummer: String) {
        planleggVarsel(varselDato, arbeidstakerFnr, orgnummer, HendelseType.NL_DIALOGMOTE_SVAR_MOTEBEHOV)
    }

    fun planleggSvarMotebehovVarselTilArbeidstaker(varselDato: LocalDate, arbeidstakerFnr: String, ansattFnr: String, orgnummer: String) {
        planleggVarsel(varselDato, arbeidstakerFnr, orgnummer, HendelseType.SM_DIALOGMOTE_SVAR_MOTEBEHOV)
    }

    private fun planleggVarsel(varselDato: LocalDate, arbeidstakerFnr: String, orgnummer: String, hendelseType: HendelseType) {
        val esyfovarselPlanlagtVarsel = EsyfovarselPlanlagtVarsel(
            varselDato.toString(),
            hendelseType,
            arbeidstakerFnr,
            orgnummer
        )
        producer.sendVarselTilEsyfovarselPlanlegging(esyfovarselPlanlagtVarsel)
    }

}
