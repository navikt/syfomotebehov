package no.nav.syfo.varsel.esyfovarsel.domain

import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.io.Serializable

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
sealed interface EsyfovarselHendelse : Serializable {
    val mottakerFnr: String
    val type: HendelseType
    var data: Any?
}

data class NarmesteLederHendelse(
    override val mottakerFnr: String, // To be removed
    override val type: HendelseType,
    override var data: Any?,
    val narmesteLederFnr: String,
    val arbeidstakerFnr: String,
    val orgnummer: String
) : EsyfovarselHendelse

data class ArbeidstakerHendelse(
    override val mottakerFnr: String, // To be removed
    override val type: HendelseType,
    override var data: Any?,
    val arbeidstakerFnr: String,
    val orgnummer: String?
) : EsyfovarselHendelse

enum class HendelseType {
    NL_DIALOGMOTE_SVAR_MOTEBEHOV,
    SM_DIALOGMOTE_SVAR_MOTEBEHOV,
}

interface EsyfovarselHendelseData : Serializable // To be removed

data class NarmesteLederVarselData( // To be removed
    val ansattFnr: String,
    val orgnummer: String
) : EsyfovarselHendelseData
