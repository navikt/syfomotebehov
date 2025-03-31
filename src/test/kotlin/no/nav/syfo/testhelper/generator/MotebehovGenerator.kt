package no.nav.syfo.testhelper.generator

import no.nav.syfo.motebehov.*
import no.nav.syfo.motebehov.database.PMotebehov
import no.nav.syfo.motebehov.formSnapshot.LegacyMotebehovToFormSnapshotHelper
import no.nav.syfo.motebehov.formSnapshot.MotebehovInnmelderType
import no.nav.syfo.motebehov.formSnapshot.mockArbeidsgiverSvarOnskerSykmelderFormSnapshot
import no.nav.syfo.motebehov.formSnapshot.mockArbeidstakerSvarFormSnapshot
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
            formSnapshot = mockArbeidsgiverSvarOnskerSykmelderFormSnapshot
        ),
//        lagFormSubmissionDTOMatchingLegacyInputDTO(
//            motebehovSvarLegacyInputDTO,
//            MotebehovSkjemaType.SVAR_BEHOV,
//            MotebehovInnmelderType.ARBEIDSGIVER
//        ),
        tildeltEnhet = NAV_ENHET,
        behandletVeilederIdent = VEILEDER_ID,
        behandletTidspunkt = LocalDateTime.now(),
        opprettetAvFnr = LEDER_FNR,
    )

    private val nyttMotebehovArbeidsgiverInput = NyttMotebehovArbeidsgiverLegacyInputDTO(
        arbeidstakerFnr = ARBEIDSTAKER_FNR,
        virksomhetsnummer = VIRKSOMHETSNUMMER,
        motebehovSvar = motebehovSvarLegacyInputDTO,
        tildeltEnhet = NAV_ENHET,
    )

    fun lagMotebehovSvarInputDTO(harBehov: Boolean): MotebehovSvarLegacyDTO {
        return motebehovSvarLegacyInputDTO.copy(
            harMotebehov = harBehov,
        )
    }

    fun lagNyttMotebehovArbeidsgiverInput(): NyttMotebehovArbeidsgiverLegacyInputDTO {
        return nyttMotebehovArbeidsgiverInput.copy()
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

    fun lagFormValuesOutputDTOMatchingInputDTO(
        inputDTO: MotebehovSvarLegacyDTO,
        skjemaType: MotebehovSkjemaType,
        innmelderType: MotebehovInnmelderType
    ): MotebehovFormValuesOutputDTO {
        return lagFormSubmissionDTOMatchingLegacyInputDTO(
            inputDTO,
            skjemaType,
            innmelderType
        ).toMotebehovFormValuesOutputDTO()
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
        formSnapshot = mockArbeidstakerSvarFormSnapshot,
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

    fun generateMotebehovOutputDTO(): MotebehovOutputDTO {
        return motebehov.toMotebehovOutputDTO()
    }
}
