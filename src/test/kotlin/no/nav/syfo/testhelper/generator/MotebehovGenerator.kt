package no.nav.syfo.testhelper.generator

import no.nav.syfo.motebehov.*
import no.nav.syfo.motebehov.database.PMotebehov
import no.nav.syfo.motebehov.formSnapshot.LegacyMotebehovToFormSnapshotHelper
import no.nav.syfo.motebehov.formSnapshot.MotebehovInnmelderType
import no.nav.syfo.motebehov.formSnapshot.mockArbeidsgiverMeldOnskerSykmelderOgTolkFormSnapshot
import no.nav.syfo.motebehov.formSnapshot.mockArbeidsgiverSvarJaOnskerSykmelderFormSnapshot
import no.nav.syfo.motebehov.formSnapshot.mockArbeidsgiverSvarNeiFormSnapshot
import no.nav.syfo.motebehov.formSnapshot.mockArbeidstakerMeldSnapshot
import no.nav.syfo.motebehov.formSnapshot.mockArbeidstakerSvarJaFormSnapshot
import no.nav.syfo.motebehov.formSnapshot.mockArbeidstakerSvarNeiFormSnapshot
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
    )

    private val nyttMotebehovArbeidsgiverLegacyInput = NyttMotebehovArbeidsgiverLegacyInputDTO(
        arbeidstakerFnr = ARBEIDSTAKER_FNR,
        virksomhetsnummer = VIRKSOMHETSNUMMER,
        motebehovSvar = motebehovSvarLegacyInputDTO,
        tildeltEnhet = NAV_ENHET,
    )

    private val nyttMotebehovArbeidsgiverFormSubmissionInput = NyttMotebehovArbeidsgiverFormSubmissionInputDTO(
        arbeidstakerFnr = ARBEIDSTAKER_FNR,
        virksomhetsnummer = VIRKSOMHETSNUMMER,
        formSubmission = MotebehovFormSubmissionDTO(
            harMotebehov = true,
            formSnapshot = mockArbeidsgiverSvarJaOnskerSykmelderFormSnapshot,
        ),
        tildeltEnhet = NAV_ENHET,
    )

    fun lagMotebehovSvarInputDTO(harBehov: Boolean): MotebehovSvarLegacyDTO {
        return motebehovSvarLegacyInputDTO.copy(
            harMotebehov = harBehov,
        )
    }

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

    fun lagNyttMotebehovArbeidsgiverLegacyInput(): NyttMotebehovArbeidsgiverLegacyInputDTO {
        return nyttMotebehovArbeidsgiverLegacyInput.copy()
    }

    fun lagNyArbeidsgiverFormSubmissionSvarJa(): NyttMotebehovArbeidsgiverFormSubmissionInputDTO {
        return nyttMotebehovArbeidsgiverFormSubmissionInput.copy(
            formSubmission = MotebehovFormSubmissionDTO(
                harMotebehov = true,
                formSnapshot = mockArbeidsgiverSvarJaOnskerSykmelderFormSnapshot,
            )
        )
    }

    fun lagNyArbeidsgiverFormSubmissionSvarNei(): NyttMotebehovArbeidsgiverFormSubmissionInputDTO {
        return nyttMotebehovArbeidsgiverFormSubmissionInput.copy(
            formSubmission = MotebehovFormSubmissionDTO(
                harMotebehov = false,
                formSnapshot = mockArbeidsgiverSvarNeiFormSnapshot,
            )
        )
    }

    fun lagNyArbeidsgiverFormSubmissionMeld(): NyttMotebehovArbeidsgiverFormSubmissionInputDTO {
        return nyttMotebehovArbeidsgiverFormSubmissionInput.copy(
            formSubmission = MotebehovFormSubmissionDTO(
                harMotebehov = true,
                formSnapshot = mockArbeidsgiverMeldOnskerSykmelderOgTolkFormSnapshot,
            )
        )
    }

    fun lagFormSubmissionDTOMatchingLegacyInputDTO(
        inputDTO: MotebehovSvarLegacyDTO,
        skjemaType: MotebehovSkjemaType,
        innmelderType: MotebehovInnmelderType
    ): MotebehovFormSubmissionCombinedDTO {
        val legacyFieldsToFormSnapshotHelper = LegacyMotebehovToFormSnapshotHelper()

        return MotebehovFormSubmissionCombinedDTO(
            harMotebehov = inputDTO.harMotebehov,
            forklaring = inputDTO.forklaring,
            formSnapshot = legacyFieldsToFormSnapshotHelper.createFormSnapshotFromLegacyMotebehovValues(
                inputDTO.harMotebehov,
                inputDTO.forklaring,
                skjemaType,
                innmelderType
            )
        )
    }

    private val nyttPMotebehovArbeidstaker = PMotebehov(
        uuid = UUID.randomUUID(),
        opprettetDato = getOpprettetDato(true),
        opprettetAv = LEDER_AKTORID,
        aktoerId = ARBEIDSTAKER_AKTORID,
        virksomhetsnummer = VIRKSOMHETSNUMMER,
        forklaring = null,
        harMotebehov = true,
        tildeltEnhet = NAV_ENHET,
        sykmeldtFnr = ARBEIDSTAKER_FNR,
        formSnapshot = mockArbeidstakerSvarJaFormSnapshot,
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

    fun generateMotebehovOutputDTO(): MotebehovWithFormValuesOutputDTO {
        return motebehov.toMotebehovWithFormValuesOutputDTO()
    }
}
