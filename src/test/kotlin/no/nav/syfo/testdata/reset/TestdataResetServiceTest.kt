package no.nav.syfo.testdata.reset

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import no.nav.syfo.LocalApplication
import no.nav.syfo.consumer.pdl.PdlConsumer
import no.nav.syfo.dialogmote.database.DialogmoteDAO
import no.nav.syfo.dialogmote.database.DialogmoteStatusEndringType
import no.nav.syfo.dialogmotekandidat.database.DialogmotekandidatDAO
import no.nav.syfo.dialogmotekandidat.database.DialogmotekandidatEndringArsak
import no.nav.syfo.motebehov.database.MotebehovDAO
import no.nav.syfo.motebehov.database.PMotebehov
import no.nav.syfo.oppfolgingstilfelle.database.OppfolgingstilfelleDAO
import no.nav.syfo.oppfolgingstilfelle.kafka.domain.KafkaOppfolgingstilfelle
import no.nav.syfo.testhelper.UserConstants.ARBEIDSTAKER_AKTORID
import no.nav.syfo.testhelper.UserConstants.ARBEIDSTAKER_FNR
import no.nav.syfo.testhelper.UserConstants.VIRKSOMHETSNUMMER
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import javax.inject.Inject

@ExtendWith(SpringExtension::class)
@SpringBootTest(classes = [LocalApplication::class])
@DirtiesContext
class TestdataResetServiceTest {

    @MockkBean(relaxed = true)
    private lateinit var pdlConsumer: PdlConsumer

    @Inject
    private lateinit var motebehovDAO: MotebehovDAO

    @Inject
    private lateinit var dialogmotekandidatDAO: DialogmotekandidatDAO

    @Inject
    private lateinit var oppfolgingstilfelleDAO: OppfolgingstilfelleDAO

    @Inject
    private lateinit var dialogmoteDAO: DialogmoteDAO

    @Inject
    private lateinit var testdataResetService: TestdataResetService

    @BeforeEach
    fun setup() {
        every { pdlConsumer.aktorid(ARBEIDSTAKER_FNR) } returns ARBEIDSTAKER_AKTORID
    }

    @Test
    fun skalNullstilleData() {
        motebehovDAO.create(PMotebehov(UUID.randomUUID(), LocalDateTime.now(), "meg", ARBEIDSTAKER_AKTORID, VIRKSOMHETSNUMMER, true, formFillout = emptyList()))
        assertThat(motebehovDAO.hentMotebehovListeForAktoer(ARBEIDSTAKER_AKTORID).size).isEqualTo(1)
        dialogmotekandidatDAO.create(UUID.randomUUID().toString(), LocalDateTime.now(), ARBEIDSTAKER_FNR, true, DialogmotekandidatEndringArsak.STOPPUNKT.name)
        assertThat(dialogmotekandidatDAO.get(ARBEIDSTAKER_FNR)).isNotNull()
        oppfolgingstilfelleDAO.create(
            ARBEIDSTAKER_FNR,
            KafkaOppfolgingstilfelle(true, LocalDate.now().minusDays(1), LocalDate.now(), listOf(VIRKSOMHETSNUMMER)),
            VIRKSOMHETSNUMMER,
        )
        assertThat(oppfolgingstilfelleDAO.get(ARBEIDSTAKER_FNR)).isNotEmpty()
        dialogmoteDAO.create(UUID.randomUUID().toString(), LocalDateTime.now(), LocalDateTime.now(), DialogmoteStatusEndringType.INNKALT.name, ARBEIDSTAKER_FNR, VIRKSOMHETSNUMMER)
        assertThat(dialogmoteDAO.getAktiveDialogmoterEtterDato(ARBEIDSTAKER_FNR, LocalDate.now().minusDays(1))).isNotEmpty()
        testdataResetService.resetTestdata(ARBEIDSTAKER_FNR)

        assertThat(motebehovDAO.hentMotebehovListeForAktoer(ARBEIDSTAKER_AKTORID).size).isEqualTo(0)
        assertThat(dialogmotekandidatDAO.get(ARBEIDSTAKER_FNR)).isNull()
        assertThat(oppfolgingstilfelleDAO.get(ARBEIDSTAKER_FNR)).isEmpty()
        assertThat(dialogmoteDAO.getAktiveDialogmoterEtterDato(ARBEIDSTAKER_FNR, LocalDate.now().minusDays(1))).isEmpty()
    }
}
