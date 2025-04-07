package no.nav.syfo.testdata.reset

import com.ninjasquad.springmockk.MockkBean
import io.kotest.extensions.spring.SpringExtension
import io.mockk.every
import no.nav.syfo.IntegrationTest
import no.nav.syfo.LocalApplication
import no.nav.syfo.consumer.pdl.PdlConsumer
import no.nav.syfo.dialogmote.database.DialogmoteDAO
import no.nav.syfo.dialogmote.database.DialogmoteStatusEndringType
import no.nav.syfo.dialogmotekandidat.database.DialogmotekandidatDAO
import no.nav.syfo.dialogmotekandidat.database.DialogmotekandidatEndringArsak
import no.nav.syfo.motebehov.MotebehovInnmelderType
import no.nav.syfo.motebehov.database.MotebehovDAO
import no.nav.syfo.motebehov.database.PMotebehov
import no.nav.syfo.motebehov.motebehovstatus.MotebehovSkjemaType
import no.nav.syfo.oppfolgingstilfelle.database.OppfolgingstilfelleDAO
import no.nav.syfo.oppfolgingstilfelle.kafka.domain.KafkaOppfolgingstilfelle
import no.nav.syfo.testhelper.UserConstants.ARBEIDSTAKER_AKTORID
import no.nav.syfo.testhelper.UserConstants.ARBEIDSTAKER_FNR
import no.nav.syfo.testhelper.UserConstants.VIRKSOMHETSNUMMER
import org.assertj.core.api.Assertions.assertThat
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@TestConfiguration
@SpringBootTest(classes = [LocalApplication::class])
class TestdataResetServiceTest : IntegrationTest() {

    @MockkBean(relaxed = true)
    private lateinit var pdlConsumer: PdlConsumer

    @Autowired
    private lateinit var motebehovDAO: MotebehovDAO

    @Autowired
    private lateinit var dialogmotekandidatDAO: DialogmotekandidatDAO

    @Autowired
    private lateinit var oppfolgingstilfelleDAO: OppfolgingstilfelleDAO

    @Autowired
    private lateinit var dialogmoteDAO: DialogmoteDAO

    @Autowired
    private lateinit var testdataResetService: TestdataResetService

    init {
        extensions(SpringExtension)
        beforeTest {
            every { pdlConsumer.aktorid(ARBEIDSTAKER_FNR) } returns ARBEIDSTAKER_AKTORID
        }

        describe("TestdataResetService") {
            it("skalNullstilleData") {
                motebehovDAO.create(
                    PMotebehov(
                        UUID.randomUUID(),
                        LocalDateTime.now(),
                        "meg",
                        ARBEIDSTAKER_AKTORID,
                        VIRKSOMHETSNUMMER,
                        true,
                        skjemaType = MotebehovSkjemaType.MELD_BEHOV,
                        innmelderType = MotebehovInnmelderType.ARBEIDSGIVER,
                    )
                )
                assertThat(motebehovDAO.hentMotebehovListeForAktoer(ARBEIDSTAKER_AKTORID).size).isEqualTo(1)
                dialogmotekandidatDAO.create(
                    UUID.randomUUID().toString(),
                    LocalDateTime.now(),
                    ARBEIDSTAKER_FNR,
                    true,
                    DialogmotekandidatEndringArsak.STOPPUNKT.name
                )
                assertThat(dialogmotekandidatDAO.get(ARBEIDSTAKER_FNR)).isNotNull()
                oppfolgingstilfelleDAO.create(
                    ARBEIDSTAKER_FNR,
                    KafkaOppfolgingstilfelle(
                        true,
                        LocalDate.now().minusDays(1),
                        LocalDate.now(),
                        listOf(VIRKSOMHETSNUMMER)
                    ),
                    VIRKSOMHETSNUMMER,
                )
                assertThat(oppfolgingstilfelleDAO.get(ARBEIDSTAKER_FNR)).isNotEmpty()
                dialogmoteDAO.create(
                    UUID.randomUUID().toString(),
                    LocalDateTime.now(),
                    LocalDateTime.now(),
                    DialogmoteStatusEndringType.INNKALT.name,
                    ARBEIDSTAKER_FNR,
                    VIRKSOMHETSNUMMER
                )
                assertThat(
                    dialogmoteDAO.getAktiveDialogmoterEtterDato(
                        ARBEIDSTAKER_FNR,
                        LocalDate.now().minusDays(1)
                    )
                ).isNotEmpty()
                testdataResetService.resetTestdata(ARBEIDSTAKER_FNR)

                assertThat(motebehovDAO.hentMotebehovListeForAktoer(ARBEIDSTAKER_AKTORID).size).isEqualTo(0)
                assertThat(dialogmotekandidatDAO.get(ARBEIDSTAKER_FNR)).isNull()
                assertThat(oppfolgingstilfelleDAO.get(ARBEIDSTAKER_FNR)).isEmpty()
                assertThat(
                    dialogmoteDAO.getAktiveDialogmoterEtterDato(
                        ARBEIDSTAKER_FNR,
                        LocalDate.now().minusDays(1)
                    )
                ).isEmpty()
            }
        }
    }
}
