package no.nav.syfo.motebehov

import javax.validation.constraints.NotEmpty

data class NyttMotebehovArbeidsgiverFormSubmissionDTO(
    val arbeidstakerFnr: String,
    val virksomhetsnummer: @NotEmpty String,
    val formSubmission: MotebehovFormSubmissionDTO,
    val tildeltEnhet: String? = null
)


// TODO: Er det en tenkt forskjell på disse?
//
//data class NyttMotebehovArbeidsgiverDTO(
//    val arbeidstakerFnr: String,
//    val virksomhetsnummer: @NotEmpty String,
//    val formSubmission: MotebehovFormSubmissionCombinedDTO,
//    val tildeltEnhet: String? = null
//)
