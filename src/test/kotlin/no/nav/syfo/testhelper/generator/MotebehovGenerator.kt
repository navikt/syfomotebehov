package no.nav.syfo.testhelper.generator

import no.nav.syfo.motebehov.*
import no.nav.syfo.motebehov.database.PMotebehov
import no.nav.syfo.motebehov.formSnapshot.*
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
    private val motebehovSvarLegacyInputDTO = MotebehovSvarLegacyDTO(
        harMotebehov = true,
        forklaring = "",
    )

    private val motebehov = Motebehov(
        id = UUID.randomUUID(),
        arbeidstakerFnr = ARBEIDSTAKER_FNR,
        aktorId = ARBEIDSTAKER_AKTORID,
        virksomhetsnummer = VIRKSOMHETSNUMMER,
        opprettetAv = LEDER_AKTORID,
        opprettetDato = LocalDateTime.now().minusMinutes(2L),
        formSubmission = MotebehovFormSubmissionCombinedDTO(
            harMotebehov = true,
            forklaring = "",
            formSnapshot = mockArbeidsgiverSvarJaOnskerSykmelderFormSnapshot
        ),
        tildeltEnhet = NAV_ENHET,
        behandletVeilederIdent = VEILEDER_ID,
        behandletTidspunkt = LocalDateTime.now(),
        opprettetAvFnr = LEDER_FNR,
        skjemaType = MotebehovSkjemaType.SVAR_BEHOV,
        innmelderType = MotebehovInnmelderType.ARBEIDSGIVER,
    )

    private val nyttMotebehovArbeidsgiverLegacyInput = NyttMotebehovArbeidsgiverLegacyDTO(
        arbeidstakerFnr = ARBEIDSTAKER_FNR,
        virksomhetsnummer = VIRKSOMHETSNUMMER,
        motebehovSvar = motebehovSvarLegacyInputDTO,
        tildeltEnhet = NAV_ENHET,
    )

    private val nyttMotebehovArbeidsgiverFormSubmissionInput = NyttMotebehovArbeidsgiverFormSubmissionDTO(
        arbeidstakerFnr = ARBEIDSTAKER_FNR,
        virksomhetsnummer = VIRKSOMHETSNUMMER,
        formSubmission = MotebehovFormSubmissionDTO(
            harMotebehov = true,
            formSnapshot = mockArbeidsgiverSvarJaOnskerSykmelderFormSnapshot,
        ),
        tildeltEnhet = NAV_ENHET,
    )

    fun lagFormSubmissionArbeidstakerSvarJaDTO(): MotebehovFormSubmissionDTO {
        return MotebehovFormSubmissionDTO(
            harMotebehov = true,
            formSnapshot = mockArbeidstakerSvarJaFormSnapshot,
        )
    }

    fun lagFormSubmissionArbeidstakerSvarNeiDTO(): MotebehovFormSubmissionDTO {
        return MotebehovFormSubmissionDTO(
            harMotebehov = false,
            formSnapshot = mockArbeidstakerSvarNeiFormSnapshot,
        )
    }

    fun lagFormSubmissionArbeidstakerMeldDTO(): MotebehovFormSubmissionDTO {
        return MotebehovFormSubmissionDTO(
            harMotebehov = true,
            formSnapshot = mockArbeidstakerMeldSnapshot,
        )
    }

    fun lagNyArbeidsgiverFormSubmissionSvarJa(): NyttMotebehovArbeidsgiverFormSubmissionDTO {
        return nyttMotebehovArbeidsgiverFormSubmissionInput.copy(
            formSubmission = MotebehovFormSubmissionDTO(
                harMotebehov = true,
                formSnapshot = mockArbeidsgiverSvarJaOnskerSykmelderFormSnapshot,
            )
        )
    }

    fun lagNyArbeidsgiverFormSubmissionSvarNei(): NyttMotebehovArbeidsgiverFormSubmissionDTO {
        return nyttMotebehovArbeidsgiverFormSubmissionInput.copy(
            formSubmission = MotebehovFormSubmissionDTO(
                harMotebehov = false,
                formSnapshot = mockArbeidsgiverSvarNeiFormSnapshot,
            )
        )
    }

    fun lagNyArbeidsgiverFormSubmissionMeld(): NyttMotebehovArbeidsgiverFormSubmissionDTO {
        return nyttMotebehovArbeidsgiverFormSubmissionInput.copy(
            formSubmission = MotebehovFormSubmissionDTO(
                harMotebehov = true,
                formSnapshot = mockArbeidsgiverMeldOnskerSykmelderOgTolkFormSnapshot,
            )
        )
    }

    private val nyttPMotebehov = PMotebehov(
        uuid = UUID.randomUUID(),
        opprettetDato = getOpprettetDato(true),
        opprettetAv = LEDER_AKTORID,
        aktoerId = ARBEIDSTAKER_AKTORID,
        virksomhetsnummer = VIRKSOMHETSNUMMER,
        forklaring = null,
        harMotebehov = true,
        tildeltEnhet = NAV_ENHET,
        sykmeldtFnr = ARBEIDSTAKER_FNR,
        skjemaType = MotebehovSkjemaType.SVAR_BEHOV,
        innmelderType = MotebehovInnmelderType.ARBEIDSGIVER,
        formSnapshot = mockArbeidsgiverSvarJaOnskerSykmelderFormSnapshot,
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
        return nyttPMotebehov.copy()
    }

    fun generateMotebehov(): Motebehov {
        return motebehov.copy()
    }
}
