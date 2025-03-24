package no.nav.syfo.motebehov

import java.io.Serializable
import javax.validation.constraints.NotEmpty

data class NyttMotebehov(
    val arbeidstakerFnr: String? = null,
    val virksomhetsnummer: @NotEmpty String,
    val motebehovSvar: MotebehovSvarLegacyInputDTO,
    val tildeltEnhet: String? = null
) : Serializable
