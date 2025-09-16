package no.nav.syfo.motebehov

import com.ninjasquad.springmockk.MockkBean
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import no.nav.syfo.IntegrationTest
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
import no.nav.syfo.testhelper.mockAndExpectBehandlendeEnhetRequest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.web.client.RestTemplate
import java.time.LocalDate

@TestConfiguration
@SpringBootTest(classes = [LocalApplication::class])
class MotebehovServiceTest : IntegrationTest() {
    @Value("\${azure.openid.config.token.endpoint}")
    private lateinit var azureTokenEndpoint: String

    @Value("\${syfobehandlendeenhet.url}")
    private lateinit var behandlendeenhetUrl: String

    @MockkBean(relaxed = true)
    private lateinit var personoppgavehendelseService: PersonoppgavehendelseService

    @Autowired
    private lateinit var motebehovDAO: MotebehovDAO

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    private lateinit var motebehovService: MotebehovService

    private val veilederIdent = "testVeileder"

    @Autowired
    private lateinit var restTemplate: RestTemplate

    @MockkBean
    private lateinit var pdlConsumer: PdlConsumer

    private lateinit var mockRestServiceServerAzureAD: MockRestServiceServer

    @Autowired
    @Qualifier("AzureAD")
    private lateinit var restTemplateAzureAD: RestTemplate

    private lateinit var mockRestServiceServer: MockRestServiceServer

    init {
        extensions(SpringExtension)
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
                mockRestServiceServer = MockRestServiceServer.bindTo(restTemplate).build()
                mockRestServiceServerAzureAD = MockRestServiceServer.bindTo(restTemplateAzureAD).build()

                mockAndExpectBehandlendeEnhetRequest(
                    azureTokenEndpoint,
                    mockRestServiceServerAzureAD,
                    mockRestServiceServer,
                    behandlendeenhetUrl,
                    ARBEIDSTAKER_FNR,
                )

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
