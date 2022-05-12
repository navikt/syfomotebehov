package no.nav.syfo.varsel.esyfovarsel.domain

import java.io.Serializable

data class EsyfovarselHendelse(
    val mottakerFnr: String,
    val type: HendelseType,
    val data: EsyfovarselHendelseData? = null
) : Serializable

enum class HendelseType {
    NL_DIALOGMOTE_SVAR_MOTEBEHOV,
    SM_DIALOGMOTE_SVAR_MOTEBEHOV,
}

interface EsyfovarselHendelseData : Serializable

data class NarmesteLederVarselData(
    val ansattFnr: String,
    val orgnummer: String
) : EsyfovarselHendelseData
