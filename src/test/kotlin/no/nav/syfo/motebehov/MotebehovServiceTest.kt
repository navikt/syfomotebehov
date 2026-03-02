package no.nav.syfo.motebehov

import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.extensions.ApplyExtension
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import no.nav.syfo.LocalApplication
import no.nav.syfo.consumer.pdl.PdlConsumer
import no.nav.syfo.motebehov.database.MotebehovDAO
import no.nav.syfo.motebehov.formSnapshot.mockArbeidsgiverSvarJaOnskerSykmelderFormSnapshot
import no.nav.syfo.motebehov.motebehovstatus.MotebehovSkjemaType
import no.nav.syfo.personoppgavehendelse.PersonoppgavehendelseService
import no.nav.syfo.testhelper.UserConstants.ARBEIDSTAKER_AKTORID
import no.nav.syfo.testhelper.UserConstants.ARBEIDSTAKER_FNR
import no.nav.syfo.testhelper.UserConstants.LEDER_AKTORID
import no.nav.syfo.testhelper.UserConstants.LEDER_FNR
import no.nav.syfo.testhelper.UserConstants.VIRKSOMHETSNUMMER
import no.nav.syfo.testhelper.generator.MotebehovGenerator
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import java.time.LocalDate
import no.nav.syfo.IntegrationTest

@SpringBootTest(classes = [LocalApplication::class])
@ApplyExtension(SpringExtension::class)
class MotebehovServiceTest : IntegrationTest() {

    @MockkBean(relaxed = true)
    private lateinit var personoppgavehendelseService: PersonoppgavehendelseService

    @Autowired
    private lateinit var motebehovDAO: MotebehovDAO

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    private lateinit var motebehovService: MotebehovService

    private val veilederIdent = "testVeileder"

    @MockkBean
    private lateinit var pdlConsumer: PdlConsumer

    init {
        beforeTest {
            val sqlDeleteAll = "DELETE FROM MOTEBEHOV"
            jdbcTemplate.update(sqlDeleteAll)
        }

        describe("Møtebehov Service") {
            it("skalFerdigstilleMotebehovOpprettetForDato") {
                val pMotebehov = MotebehovGenerator().generatePmotebehov()
                val uuid = motebehovDAO.create(pMotebehov)
                val count = motebehovService.behandleUbehandledeMotebehovOpprettetTidligereEnnDato(
                    LocalDate.now().plusWeeks(1),
                    veilederIdent
                )
                val motebehov = motebehovDAO.hentMotebehov(uuid.toString())
                count shouldBe 1
                motebehov.first().behandletVeilederIdent shouldBe veilederIdent
                motebehov.first().behandletTidspunkt.shouldNotBeNull()
            }

            it("skalIkkeFerdigstilleMotebehovOpprettetEtterDato") {
                val pMotebehov = MotebehovGenerator().generatePmotebehov()
                val uuid = motebehovDAO.create(pMotebehov)
                val count = motebehovService.behandleUbehandledeMotebehovOpprettetTidligereEnnDato(
                    LocalDate.now().minusWeeks(1),
                    veilederIdent
                )
                val motebehov = motebehovDAO.hentMotebehov(uuid.toString())
                count shouldBe 0
                motebehov.first().behandletVeilederIdent.shouldBeNull()
                motebehov.first().behandletTidspunkt.shouldBeNull()
            }

            it("should store motebehov and retrieve it with same values") {
                // Arrange
                every { pdlConsumer.aktorid(ARBEIDSTAKER_FNR) } returns ARBEIDSTAKER_AKTORID
                every { pdlConsumer.aktorid(LEDER_FNR) } returns LEDER_AKTORID

                // Act
                val uuid = motebehovService.lagreMotebehov(
                    LEDER_FNR,
                    ARBEIDSTAKER_FNR,
                    VIRKSOMHETSNUMMER,
                    MotebehovSkjemaType.SVAR_BEHOV,
                    MotebehovInnmelderType.ARBEIDSGIVER,
                    MotebehovFormSubmissionDTO(
                        true,
                        mockArbeidsgiverSvarJaOnskerSykmelderFormSnapshot
                    )
                )

                val retrievedMotebehov = motebehovService.hentMotebehov(uuid.toString())

                // Assert
                retrievedMotebehov.shouldNotBeNull()
                retrievedMotebehov.id shouldBe uuid
                retrievedMotebehov.opprettetAvFnr shouldBe LEDER_FNR
                retrievedMotebehov.arbeidstakerFnr shouldBe ARBEIDSTAKER_FNR
                retrievedMotebehov.opprettetAv shouldBe LEDER_AKTORID
                retrievedMotebehov.aktorId shouldBe ARBEIDSTAKER_AKTORID
                retrievedMotebehov.virksomhetsnummer shouldBe VIRKSOMHETSNUMMER
                retrievedMotebehov.behandletTidspunkt.shouldBeNull()
                retrievedMotebehov.behandletVeilederIdent.shouldBeNull()
                retrievedMotebehov.skjemaType shouldBe MotebehovSkjemaType.SVAR_BEHOV
                retrievedMotebehov.innmelderType shouldBe MotebehovInnmelderType.ARBEIDSGIVER

                val retrievedformSnapshot = retrievedMotebehov.formSubmission.formSnapshot
                retrievedformSnapshot shouldBe mockArbeidsgiverSvarJaOnskerSykmelderFormSnapshot
            }
        }
    }
}
