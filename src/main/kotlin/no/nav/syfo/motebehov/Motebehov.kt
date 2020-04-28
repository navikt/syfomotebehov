package no.nav.syfo.motebehov

import java.io.Serializable
import java.time.LocalDateTime
import java.util.*

data class Motebehov(
        val id: UUID,
        val opprettetDato: LocalDateTime,
        val aktorId: String,
        val opprettetAv: String,
        val arbeidstakerFnr: String,
        val virksomhetsnummer: String,
        val motebehovSvar: MotebehovSvar,
        val tildeltEnhet: String? = null,
        val behandletTidspunkt: LocalDateTime? = null,
        val behandletVeilederIdent: String? = null
) : Serializable
