package no.nav.syfo.motebehov.motebehovstatus

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
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
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@ExtendWith(SpringExtension::class)
@SpringBootTest(classes = [LocalApplication::class])
@DirtiesContext
class MotebehovStatusServiceTest {

    @MockkBean
    private lateinit var motebehovService: MotebehovService

    @MockkBean
    private lateinit var dialogmotekandidatService: DialogmotekandidatService

    @MockkBean
    private lateinit var dialogmoteStatusService: DialogmoteStatusService

    @MockkBean
    private lateinit var oppfolgingstilfelleService: OppfolgingstilfelleService

    @MockkBean
    private lateinit var pdlConsumer: PdlConsumer

    @Autowired
    private lateinit var motebehovStatusServiceV2: MotebehovStatusServiceV2

    private val userFnr = UserConstants.ARBEIDSTAKER_FNR

    @BeforeEach
    fun setUp() {
        every { pdlConsumer.aktorid(any()) } returns UserConstants.ARBEIDSTAKER_AKTORID
        every { pdlConsumer.fnr(any()) } returns UserConstants.ARBEIDSTAKER_FNR
    }

    @Test
    fun kandidatWithNoDialogmoteGivesStatusSvarBehov() {
        every { dialogmoteStatusService.isDialogmotePlanlagtEtterDato(userFnr, null, any()) } returns false
        every { oppfolgingstilfelleService.getActiveOppfolgingstilfelleForArbeidstaker(userFnr) } returns createOppfolgingstilfelle()
        every { motebehovService.hentMotebehovListeForOgOpprettetAvArbeidstaker(userFnr) } returns emptyList()
        every { dialogmotekandidatService.getDialogmotekandidatStatus(userFnr) } returns createDialogmoteKandidatEndring()
        val motebehovStatusForArbeidstaker =
            motebehovStatusServiceV2.motebehovStatusForArbeidstaker(userFnr)

        assertThat(motebehovStatusForArbeidstaker.skjemaType).isEqualTo(MotebehovSkjemaType.SVAR_BEHOV)
    }

    @Test
    fun kandidatWithDialogmoteGivesNoMotebehov() {
        every { dialogmoteStatusService.isDialogmotePlanlagtEtterDato(userFnr, null, any()) } returns true
        every { oppfolgingstilfelleService.getActiveOppfolgingstilfelleForArbeidstaker(userFnr) } returns createOppfolgingstilfelle()
        every { motebehovService.hentMotebehovListeForOgOpprettetAvArbeidstaker(userFnr) } returns emptyList()
        every { dialogmotekandidatService.getDialogmotekandidatStatus(userFnr) } returns createDialogmoteKandidatEndring()

        val motebehovStatusForArbeidstaker =
            motebehovStatusServiceV2.motebehovStatusForArbeidstaker(userFnr)

        assertThat(motebehovStatusForArbeidstaker.skjemaType).isNull()
    }

    @Test
    fun noDialogmoteAndNoKandidatGivesMeldBehov() {
        every { dialogmoteStatusService.isDialogmotePlanlagtEtterDato(userFnr, null, any()) } returns false
        every { oppfolgingstilfelleService.getActiveOppfolgingstilfelleForArbeidstaker(userFnr) } returns createOppfolgingstilfelle()
        every { motebehovService.hentMotebehovListeForOgOpprettetAvArbeidstaker(userFnr) } returns emptyList()
        every { dialogmotekandidatService.getDialogmotekandidatStatus(userFnr) } returns null

        val motebehovStatusForArbeidstaker =
            motebehovStatusServiceV2.motebehovStatusForArbeidstaker(userFnr)

        assertThat(motebehovStatusForArbeidstaker.skjemaType).isEqualTo(MotebehovSkjemaType.MELD_BEHOV)
    }

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
}
