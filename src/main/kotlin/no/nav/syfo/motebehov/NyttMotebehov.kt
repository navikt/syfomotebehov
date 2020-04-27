package no.nav.syfo.motebehov

import no.nav.syfo.domain.rest.MotebehovSvar
import java.io.Serializable
import javax.validation.constraints.NotEmpty

data class NyttMotebehov(
        val arbeidstakerFnr: String? = null,
        val virksomhetsnummer: @NotEmpty String,
        val motebehovSvar: MotebehovSvar,
        val tildeltEnhet: String? = null
) : Serializable
