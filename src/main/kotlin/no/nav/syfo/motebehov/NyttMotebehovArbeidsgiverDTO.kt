package no.nav.syfo.motebehov

import java.util.UUID
import javax.validation.constraints.NotEmpty

data class NyttMotebehovArbeidsgiverDTO(
    val arbeidstakerFnr: String,
    val virksomhetsnummer: @NotEmpty String,
    val narmesteLederId: UUID,
    val formSubmission: MotebehovFormSubmissionDTO,
    val tildeltEnhet: String? = null,
)
