package no.nav.syfo.testhelper.generator

import no.nav.syfo.motebehov.*
import no.nav.syfo.motebehov.database.PMotebehov
import no.nav.syfo.motebehov.motebehovstatus.MotebehovSkjemaType
import no.nav.syfo.testhelper.UserConstants.ARBEIDSTAKER_AKTORID
import no.nav.syfo.testhelper.UserConstants.ARBEIDSTAKER_FNR
import no.nav.syfo.testhelper.UserConstants.LEDER_AKTORID
import no.nav.syfo.testhelper.UserConstants.LEDER_FNR
import no.nav.syfo.testhelper.UserConstants.NAV_ENHET
import no.nav.syfo.testhelper.UserConstants.VEILEDER_ID
import no.nav.syfo.testhelper.UserConstants.VIRKSOMHETSNUMMER
import no.nav.syfo.util.MOTEBEHOVSVAR_GYLDIGHET_DAGER
import java.time.LocalDateTime
import java.util.*

class MotebehovGenerator {
    private val motebehovSvar = MotebehovSvar(
        harMotebehov = true,
        forklaring = "",
        formFillout = emptyList(),
        skjemaType = MotebehovSkjemaType.SVAR_BEHOV
    )

    private val nyttMotebehovSvar = TemporaryCombinedNyttMotebehovSvar(
        harMotebehov = true,
        forklaring = "",
        formFillout = emptyList(),
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
        behandletTidspunkt = LocalDateTime.now(),
        opprettetAvFnr = LEDER_FNR,
    )
    private val nyttMotebehovArbeidstaker = NyttMotebehov(
        arbeidstakerFnr = ARBEIDSTAKER_FNR,
        virksomhetsnummer = VIRKSOMHETSNUMMER,
        motebehovSvar = motebehovSvar,
        tildeltEnhet = NAV_ENHET,
    )

    private val nyttMotebehovArbeidsgiverDTO = MotebehovSvarArbeidsgiverDTO(
        arbeidstakerFnr = ARBEIDSTAKER_FNR,
        virksomhetsnummer = VIRKSOMHETSNUMMER,
        motebehovSvar = nyttMotebehovSvar,
        tildeltEnhet = NAV_ENHET,
    )

    fun lagMotebehovSvar(harBehov: Boolean): MotebehovSvar {
        return motebehovSvar.copy(
            harMotebehov = harBehov,
        )
    }

    fun lagMotebehovSvarOldSubmissionDTO(harBehov: Boolean): NyttMotebehovSvarInputDTO {
        return NyttMotebehovSvarInputDTO(
            harMotebehov = harBehov,
            forklaring = "",
        )
    }

    fun lagMotebehovSvarFromOldSubmissionDTO(nyttMotebehovSvarInputDTO: NyttMotebehovSvarInputDTO): MotebehovSvar {
        return MotebehovSvar(
            harMotebehov = nyttMotebehovSvarInputDTO.harMotebehov,
            forklaring = nyttMotebehovSvarInputDTO.forklaring,
            formFillout = emptyList(),
        )
    }

    fun lagNyttMotebehovArbeidsgiver(): MotebehovSvarArbeidsgiverDTO {
        return nyttMotebehovArbeidsgiverDTO.copy()
    }

    fun lagNyttMotebehovArbeidsgiverOldSvarSubmissionDTO(motebehovSvar: MotebehovSvar? = null): MotebehovSvarArbeidsgiverInputDTO {
        val nyttMotebehovAG = lagNyttMotebehovArbeidsgiver()

        return MotebehovSvarArbeidsgiverInputDTO(
            arbeidstakerFnr = nyttMotebehovAG.arbeidstakerFnr,
            virksomhetsnummer = nyttMotebehovAG.virksomhetsnummer,
            motebehovSvar = NyttMotebehovSvarInputDTO(
                harMotebehov = motebehovSvar?.harMotebehov ?: nyttMotebehovAG.motebehovSvar.harMotebehov,
                forklaring = motebehovSvar?.forklaring ?: nyttMotebehovAG.motebehovSvar.forklaring,
            ),
        )
    }

    private val nyttPMotebehovArbeidstaker = PMotebehov(
        uuid = UUID.randomUUID(),
        opprettetDato = getOpprettetDato(true),
        opprettetAv = LEDER_AKTORID,
        aktoerId = ARBEIDSTAKER_AKTORID,
        virksomhetsnummer = VIRKSOMHETSNUMMER,
        forklaring = "Megling",
        harMotebehov = true,
        formFillout = emptyList(),
        tildeltEnhet = NAV_ENHET,
        sykmeldtFnr = ARBEIDSTAKER_FNR,
    )

    fun getOpprettetDato(erGyldig: Boolean): LocalDateTime {
        return if (erGyldig) {
            LocalDateTime.now().minusDays(MOTEBEHOVSVAR_GYLDIGHET_DAGER.toLong())
        } else {
            LocalDateTime.now()
                .minusDays(MOTEBEHOVSVAR_GYLDIGHET_DAGER + 1.toLong())
        }
    }

    fun generatePmotebehov(): PMotebehov {
        return nyttPMotebehovArbeidstaker.copy()
    }

    fun generateMotebehov(): Motebehov {
        return motebehov.copy()
    }
}
