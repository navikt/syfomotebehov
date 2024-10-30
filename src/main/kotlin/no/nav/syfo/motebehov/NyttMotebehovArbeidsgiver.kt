package no.nav.syfo.motebehov

import java.io.Serializable
import javax.validation.constraints.NotEmpty

data class NyttMotebehovArbeidsgiver(
    val arbeidstakerFnr: String,
    val virksomhetsnummer: @NotEmpty String,
    val motebehovSvar: MotebehovSvar,
    val tildeltEnhet: String? = null
) : Serializable

data class NyttMotebehovArbeidsgiverWithOldSvarSubmissionDTO(
    val arbeidstakerFnr: String,
    val virksomhetsnummer: @NotEmpty String,
    val motebehovSvar: MotebehovSvarSubmissionOldDTO,
    val tildeltEnhet: String? = null
) : Serializable

data class NyttMotebehovArbeidsgiverWithDynamicFormSubmissionDTO(
    val arbeidstakerFnr: String,
    val virksomhetsnummer: @NotEmpty String,
    val motebehovSvar: MotebehovDynamicFormSubmissionDTO,
    val tildeltEnhet: String? = null
) : Serializable
