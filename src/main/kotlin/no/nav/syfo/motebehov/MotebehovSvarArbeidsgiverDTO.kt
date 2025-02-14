package no.nav.syfo.motebehov

import java.io.Serializable
import javax.validation.constraints.NotEmpty

data class MotebehovSvarArbeidsgiverDTO(
    val arbeidstakerFnr: String,
    val virksomhetsnummer: @NotEmpty String,
    val motebehovSvar: TemporaryCombinedNyttMotebehovSvar,
    val tildeltEnhet: String? = null
) : Serializable

data class MotebehovSvarArbeidsgiverInputDTO(
    val arbeidstakerFnr: String,
    val virksomhetsnummer: @NotEmpty String,
    val motebehovSvar: NyttMotebehovSvarInputDTO,
    val tildeltEnhet: String? = null
) : Serializable

data class MotebehovSvarArbeidsgiverFormFilloutInputDTO(
    val arbeidstakerFnr: String,
    val virksomhetsnummer: @NotEmpty String,
    val motebehovSvar: NyttMotebehovSvarFormFilloutInputDTO,
    val tildeltEnhet: String? = null
) : Serializable
