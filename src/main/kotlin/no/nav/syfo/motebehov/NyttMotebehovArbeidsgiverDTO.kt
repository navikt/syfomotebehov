package no.nav.syfo.motebehov

import jakarta.validation.constraints.NotEmpty
import java.util.UUID

data class NyttMotebehovArbeidsgiverDTO(
    val arbeidstakerFnr: String,
    val virksomhetsnummer: @NotEmpty String,
    val formSubmission: MotebehovFormSubmissionDTO,
    val tildeltEnhet: String? = null,
)

data class NyttMotebehovArbeidsgiverV5DTO(
    val narmesteLederId: UUID,
    val formSubmission: MotebehovFormSubmissionDTO,
)
