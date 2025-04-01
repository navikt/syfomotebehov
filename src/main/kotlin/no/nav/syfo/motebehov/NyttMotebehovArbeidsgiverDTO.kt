package no.nav.syfo.motebehov

import javax.validation.constraints.NotEmpty

data class NyttMotebehovArbeidsgiverLegacyInputDTO(
    val arbeidstakerFnr: String,
    val virksomhetsnummer: @NotEmpty String,
    val motebehovSvar: MotebehovSvarLegacyDTO,
    val tildeltEnhet: String? = null
)

data class NyttMotebehovArbeidsgiverFormSubmissionInputDTO(
    val arbeidstakerFnr: String,
    val virksomhetsnummer: @NotEmpty String,
    val formSubmission: MotebehovFormSubmissionDTO,
    val tildeltEnhet: String? = null
)

data class NyttMotebehovArbeidsgiverDTO(
    val arbeidstakerFnr: String,
    val virksomhetsnummer: @NotEmpty String,
    val formSubmission: MotebehovFormSubmissionCombinedDTO,
    val tildeltEnhet: String? = null
)
