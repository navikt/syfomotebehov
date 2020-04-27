package no.nav.syfo.testhelper

import no.nav.syfo.domain.rest.MotebehovSvar
import no.nav.syfo.motebehov.NyttMotebehov
import no.nav.syfo.motebehov.database.PMotebehov
import no.nav.syfo.testhelper.UserConstants.ARBEIDSTAKER_AKTORID
import no.nav.syfo.testhelper.UserConstants.ARBEIDSTAKER_FNR
import no.nav.syfo.testhelper.UserConstants.LEDER_AKTORID
import no.nav.syfo.testhelper.UserConstants.NAV_ENHET
import no.nav.syfo.testhelper.UserConstants.VIRKSOMHETSNUMMER
import no.nav.syfo.util.DbUtil
import java.time.LocalDateTime
import java.util.*

class MotebehovGenerator {
    private val motebehovSvar = MotebehovSvar()
            .harMotebehov(true)
            .forklaring("")
    private val nyttMotebehovArbeidstaker = NyttMotebehov(
            arbeidstakerFnr = ARBEIDSTAKER_FNR,
            virksomhetsnummer = VIRKSOMHETSNUMMER,
            motebehovSvar = motebehovSvar,
            tildeltEnhet = NAV_ENHET
    )

    fun lagMotebehovSvar(harBehov: Boolean): MotebehovSvar {
        return motebehovSvar
                .harMotebehov(harBehov)
    }

    fun lagNyttMotebehovFraAT(): NyttMotebehov {
        return nyttMotebehovArbeidstaker
    }

    private val nyttPMotebehovArbeidstaker = PMotebehov(
            uuid = UUID.randomUUID(),
            opprettetDato = getOpprettetDato(true),
            opprettetAv = LEDER_AKTORID,
            aktoerId = ARBEIDSTAKER_AKTORID,
            virksomhetsnummer = VIRKSOMHETSNUMMER,
            forklaring = "Megling",
            harMotebehov = true,
            tildeltEnhet = NAV_ENHET
    )

    fun getOpprettetDato(erGyldig: Boolean): LocalDateTime {
        return if (erGyldig) LocalDateTime.now().minusDays(DbUtil.MOTEBEHOVSVAR_GYLDIGHET_DAGER.toLong()) else LocalDateTime.now().minusDays(DbUtil.MOTEBEHOVSVAR_GYLDIGHET_DAGER + 1.toLong())
    }

    fun generatePmotebehov(): PMotebehov {
        return nyttPMotebehovArbeidstaker
    }
}
