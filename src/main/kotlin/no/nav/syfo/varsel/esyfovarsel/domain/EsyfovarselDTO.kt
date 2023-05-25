package no.nav.syfo.varsel.esyfovarsel.domain

import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.io.Serializable

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
sealed interface EsyfovarselHendelse : Serializable {
    val type: HendelseType
    val ferdigstill: Boolean?
    var data: Any?
}

data class NarmesteLederHendelse(
    override val type: HendelseType,
    override val ferdigstill: Boolean?,
    override var data: Any?,
    val narmesteLederFnr: String,
    val arbeidstakerFnr: String,
    val orgnummer: String
) : EsyfovarselHendelse

data class ArbeidstakerHendelse(
    override val type: HendelseType,
    override val ferdigstill: Boolean?,
    override var data: Any?,
    val arbeidstakerFnr: String,
    val orgnummer: String?
) : EsyfovarselHendelse

enum class HendelseType {
    NL_DIALOGMOTE_SVAR_MOTEBEHOV,
    SM_DIALOGMOTE_SVAR_MOTEBEHOV,
}
