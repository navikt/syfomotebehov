package no.nav.syfo.motebehov

import javax.validation.constraints.NotEmpty

data class NyttMotebehovArbeidsgiverInputDTO(
    val arbeidstakerFnr: String,
    val virksomhetsnummer: @NotEmpty String,
    val motebehovSvarInput: MotebehovSvarInputDTO,
    val tildeltEnhet: String? = null
)

data class NyttMotebehovArbeidsgiverFormFilloutInputDTO(
    val arbeidstakerFnr: String,
    val virksomhetsnummer: @NotEmpty String,
    val motebehovSvarInputDTO: MotebehovSvarFormFilloutInputDTO,
    val tildeltEnhet: String? = null
)

data class NyttMotebehovArbeidsgiverDTO(
    val arbeidstakerFnr: String,
    val virksomhetsnummer: @NotEmpty String,
    val motebehovSvarInputDTO: TemporaryCombinedNyttMotebehovSvar,
    val tildeltEnhet: String? = null
)
