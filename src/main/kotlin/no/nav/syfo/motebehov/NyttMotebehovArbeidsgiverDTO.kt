package no.nav.syfo.motebehov

import javax.validation.constraints.NotEmpty

data class NyttMotebehovArbeidsgiverInputDTO(
    val arbeidstakerFnr: String,
    val virksomhetsnummer: @NotEmpty String,
    val motebehovSvar: MotebehovSvarInputDTO,
    val tildeltEnhet: String? = null
)

data class NyttMotebehovArbeidsgiverFormValuesInputDTO(
    val arbeidstakerFnr: String,
    val virksomhetsnummer: @NotEmpty String,
    val motebehovFormValues: MotebehovFormValuesInputDTO,
    val tildeltEnhet: String? = null
)

data class NyttMotebehovArbeidsgiverDTO(
    val arbeidstakerFnr: String,
    val virksomhetsnummer: @NotEmpty String,
    val motebehovSvarInputDTO: TemporaryCombinedNyttMotebehovSvar,
    val tildeltEnhet: String? = null
)
