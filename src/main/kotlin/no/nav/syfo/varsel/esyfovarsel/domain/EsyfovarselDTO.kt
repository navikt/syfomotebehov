package no.nav.syfo.varsel.esyfovarsel.domain

import java.io.Serializable

data class EsyfovarselPlanlagtVarsel(
    val varselDato: String,
    val type: HendelseType,
    val arbeidstakerFnr: String,
    val orgnummer: String,
) : Serializable

enum class HendelseType {
    NL_DIALOGMOTE_SVAR_MOTEBEHOV,
    SM_DIALOGMOTE_SVAR_MOTEBEHOV,
}