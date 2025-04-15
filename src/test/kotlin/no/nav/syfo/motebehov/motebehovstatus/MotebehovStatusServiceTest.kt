package no.nav.syfo.motebehov.motebehovstatus

import io.kotest.core.spec.style.DescribeSpec
import io.mockk.every
import io.mockk.mockk
import no.nav.syfo.LocalApplication
import no.nav.syfo.consumer.pdl.PdlConsumer
import no.nav.syfo.dialogmote.DialogmoteStatusService
import no.nav.syfo.dialogmotekandidat.DialogmotekandidatService
import no.nav.syfo.dialogmotekandidat.database.DialogmoteKandidatEndring
import no.nav.syfo.dialogmotekandidat.database.DialogmotekandidatEndringArsak
import no.nav.syfo.motebehov.MotebehovService
import no.nav.syfo.oppfolgingstilfelle.OppfolgingstilfelleService
import no.nav.syfo.oppfolgingstilfelle.database.PersonOppfolgingstilfelle
import no.nav.syfo.testhelper.UserConstants
import no.nav.syfo.varsel.VarselServiceTest.Companion.userFnr
import org.assertj.core.api.Assertions.assertThat
import org.springframework.boot.test.context.SpringBootTest
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@SpringBootTest(classes = [LocalApplication::class])
class MotebehovStatusServiceTest : DescribeSpec({

    val motebehovService: MotebehovService = mockk<MotebehovService>()

    val dialogmotekandidatService: DialogmotekandidatService = mockk<DialogmotekandidatService>()

    val dialogmoteStatusService: DialogmoteStatusService = mockk<DialogmoteStatusService>()

    val oppfolgingstilfelleService: OppfolgingstilfelleService = mockk<OppfolgingstilfelleService>()

    val pdlConsumer: PdlConsumer = mockk<PdlConsumer>()

    val motebehovStatusServiceV2: MotebehovStatusServiceV2 = MotebehovStatusServiceV2(
        motebehovService,
        dialogmotekandidatService,
        dialogmoteStatusService,
        oppfolgingstilfelleService,
        motebehovStatusHelper = MotebehovStatusHelper(),
    )

    val userFnr = UserConstants.ARBEIDSTAKER_FNR

    beforeTest {
        every { pdlConsumer.aktorid(any()) } returns UserConstants.ARBEIDSTAKER_AKTORID
        every { pdlConsumer.fnr(any()) } returns UserConstants.ARBEIDSTAKER_FNR
    }

    describe("describe") {
        it("kandidatWithNoDialogmoteGivesStatusSvarBehov") {
            every { dialogmoteStatusService.isDialogmotePlanlagtEtterDato(userFnr, null, any()) } returns false
            every { oppfolgingstilfelleService.getActiveOppfolgingstilfelleForArbeidstaker(userFnr) } returns createOppfolgingstilfelle()
            every { motebehovService.hentMotebehovListeForOgOpprettetAvArbeidstaker(userFnr) } returns emptyList()
            every { dialogmotekandidatService.getDialogmotekandidatStatus(userFnr) } returns createDialogmoteKandidatEndring()
            val motebehovStatusForArbeidstaker =
                motebehovStatusServiceV2.motebehovStatusForArbeidstaker(userFnr)

            assertThat(motebehovStatusForArbeidstaker.skjemaType).isEqualTo(MotebehovSkjemaType.SVAR_BEHOV)
            assertThat(motebehovStatusForArbeidstaker.visMotebehov).isTrue()
        }

        it("kandidatWithDialogmoteGivesNoMotebehov") {
            every { dialogmoteStatusService.isDialogmotePlanlagtEtterDato(userFnr, null, any()) } returns true
            every { oppfolgingstilfelleService.getActiveOppfolgingstilfelleForArbeidstaker(userFnr) } returns createOppfolgingstilfelle()
            every { motebehovService.hentMotebehovListeForOgOpprettetAvArbeidstaker(userFnr) } returns emptyList()
            every { dialogmotekandidatService.getDialogmotekandidatStatus(userFnr) } returns createDialogmoteKandidatEndring()

            val motebehovStatusForArbeidstaker =
                motebehovStatusServiceV2.motebehovStatusForArbeidstaker(userFnr)

            assertThat(motebehovStatusForArbeidstaker.visMotebehov).isFalse()
        }

        it("noDialogmoteAndNoKandidatGivesMeldBehov") {
            every { dialogmoteStatusService.isDialogmotePlanlagtEtterDato(userFnr, null, any()) } returns false
            every { oppfolgingstilfelleService.getActiveOppfolgingstilfelleForArbeidstaker(userFnr) } returns createOppfolgingstilfelle()
            every { motebehovService.hentMotebehovListeForOgOpprettetAvArbeidstaker(userFnr) } returns emptyList()
            every { dialogmotekandidatService.getDialogmotekandidatStatus(userFnr) } returns null

            val motebehovStatusForArbeidstaker =
                motebehovStatusServiceV2.motebehovStatusForArbeidstaker(userFnr)

            assertThat(motebehovStatusForArbeidstaker.skjemaType).isEqualTo(MotebehovSkjemaType.MELD_BEHOV)
            assertThat(motebehovStatusForArbeidstaker.visMotebehov).isTrue()
        }
    }
})

private fun createOppfolgingstilfelle(): PersonOppfolgingstilfelle {
    return PersonOppfolgingstilfelle(
        userFnr,
        LocalDate.now().minusWeeks(4),
        LocalDate.now().minusMonths(2)
    )
}

private fun createDialogmoteKandidatEndring(): DialogmoteKandidatEndring {
    return DialogmoteKandidatEndring(
        UUID.randomUUID(),
        UUID.randomUUID(),
        UserConstants.ARBEIDSTAKER_FNR,
        true,
        DialogmotekandidatEndringArsak.STOPPUNKT,
        LocalDateTime.now().minusMinutes(10),
        LocalDateTime.now()
    )
}
