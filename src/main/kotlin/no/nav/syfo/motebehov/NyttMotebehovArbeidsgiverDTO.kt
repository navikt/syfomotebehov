package no.nav.syfo.motebehov

import jakarta.validation.constraints.NotEmpty

data class NyttMotebehovArbeidsgiverDTO(
    val arbeidstakerFnr: String,
    val virksomhetsnummer: @NotEmpty String,
    val formSubmission: MotebehovFormSubmissionDTO,
    val tildeltEnhet: String? = null,
)
