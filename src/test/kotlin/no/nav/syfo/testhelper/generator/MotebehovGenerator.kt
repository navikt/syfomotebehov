package no.nav.syfo.testhelper.generator

import no.nav.syfo.motebehov.*
import no.nav.syfo.motebehov.database.PMotebehov
import no.nav.syfo.testhelper.UserConstants.ARBEIDSTAKER_AKTORID
import no.nav.syfo.testhelper.UserConstants.ARBEIDSTAKER_FNR
import no.nav.syfo.testhelper.UserConstants.LEDER_AKTORID
import no.nav.syfo.testhelper.UserConstants.NAV_ENHET
import no.nav.syfo.testhelper.UserConstants.VEILEDER_ID
import no.nav.syfo.testhelper.UserConstants.VIRKSOMHETSNUMMER
import no.nav.syfo.util.MOTEBEHOVSVAR_GYLDIGHET_DAGER
import java.time.LocalDateTime
import java.util.*

class MotebehovGenerator {
    private val motebehovSvar = MotebehovSvar(
        harMotebehov = true,
        forklaring = ""
    )
    private val motebehov = Motebehov(
        id = UUID.randomUUID(),
        arbeidstakerFnr = ARBEIDSTAKER_FNR,
        aktorId = ARBEIDSTAKER_AKTORID,
        virksomhetsnummer = VIRKSOMHETSNUMMER,
        opprettetAv = LEDER_AKTORID,
        opprettetDato = LocalDateTime.now().minusMinutes(2L),
        motebehovSvar = motebehovSvar.copy(harMotebehov = true),
        tildeltEnhet = NAV_ENHET,
        behandletVeilederIdent = VEILEDER_ID,
        behandletTidspunkt = LocalDateTime.now()
    )
    private val nyttMotebehovArbeidstaker = NyttMotebehov(
        arbeidstakerFnr = ARBEIDSTAKER_FNR,
        virksomhetsnummer = VIRKSOMHETSNUMMER,
        motebehovSvar = motebehovSvar,
        tildeltEnhet = NAV_ENHET
    )

    private val nyttMotebehovArbeidsgiver = NyttMotebehovArbeidsgiver(
        arbeidstakerFnr = ARBEIDSTAKER_FNR,
        virksomhetsnummer = VIRKSOMHETSNUMMER,
        motebehovSvar = motebehovSvar,
        tildeltEnhet = NAV_ENHET
    )

    fun lagMotebehovSvar(harBehov: Boolean): MotebehovSvar {
        return motebehovSvar.copy(
            harMotebehov = harBehov
        )
    }

    fun lagNyttMotebehovFraAT(): NyttMotebehov {
        return nyttMotebehovArbeidstaker.copy()
    }

    fun lagNyttMotebehovArbeidsgiver(): NyttMotebehovArbeidsgiver {
        return nyttMotebehovArbeidsgiver.copy()
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
        return if (erGyldig) LocalDateTime.now().minusDays(MOTEBEHOVSVAR_GYLDIGHET_DAGER.toLong()) else LocalDateTime.now().minusDays(MOTEBEHOVSVAR_GYLDIGHET_DAGER + 1.toLong())
    }

    fun generatePmotebehov(): PMotebehov {
        return nyttPMotebehovArbeidstaker.copy()
    }

    fun generateMotebehov(): Motebehov {
        return motebehov.copy()
    }
}
