package no.nav.syfo.motebehov.api

import com.ninjasquad.springmockk.MockkBean
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.extensions.ApplyExtension
import io.kotest.extensions.spring.SpringExtension
import io.mockk.every
import io.mockk.verify
import jakarta.ws.rs.ForbiddenException
import no.nav.syfo.IntegrationTest
import no.nav.syfo.LocalApplication
import no.nav.syfo.consumer.azuread.v2.AzureAdV2TokenConsumer
import no.nav.syfo.consumer.brukertilgang.DineSykmeldteConsumer
import no.nav.syfo.consumer.brukertilgang.DineSykmeldteResponse
import no.nav.syfo.consumer.pdl.PdlConsumer
import no.nav.syfo.dialogmotekandidat.database.DialogmotekandidatDAO
import no.nav.syfo.dialogmotekandidat.database.DialogmotekandidatEndringArsak
import no.nav.syfo.motebehov.MotebehovFormSubmissionDTO
import no.nav.syfo.motebehov.NyttMotebehovArbeidsgiverV5DTO
import no.nav.syfo.motebehov.database.MotebehovDAO
import no.nav.syfo.motebehov.formSnapshot.mockArbeidsgiverSvarJaOnskerSykmelderFormSnapshot
import no.nav.syfo.motebehov.motebehovstatus.DAYS_START_SVAR_BEHOV
import no.nav.syfo.oppfolgingstilfelle.database.OppfolgingstilfelleDAO
import no.nav.syfo.personoppgavehendelse.PersonoppgavehendelseProducer
import no.nav.syfo.testhelper.UserConstants.ARBEIDSTAKER_AKTORID
import no.nav.syfo.testhelper.UserConstants.ARBEIDSTAKER_FNR
import no.nav.syfo.testhelper.UserConstants.LEDER_AKTORID
import no.nav.syfo.testhelper.UserConstants.LEDER_FNR
import no.nav.syfo.testhelper.UserConstants.NARMESTE_LEDER_ID
import no.nav.syfo.testhelper.UserConstants.VIRKSOMHETSNUMMER
import no.nav.syfo.testhelper.assertion.assertMotebehovStatus
import no.nav.syfo.testhelper.generator.generateOppfolgingstilfellePerson
import no.nav.syfo.testhelper.mockAndExpectBehandlendeEnhetRequest
import no.nav.syfo.util.TokenValidationUtil
import no.nav.syfo.varsel.esyfovarsel.EsyfovarselService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.cache.CacheManager
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.web.client.RestTemplate
import java.time.LocalDateTime
import java.util.UUID
import java.util.function.Consumer

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestConfiguration
@SpringBootTest(classes = [LocalApplication::class])
@ApplyExtension(SpringExtension::class)
class MotebehovArbeidsgiverControllerV5Test : IntegrationTest() {
    @Value("\${azure.openid.config.token.endpoint}")
    private lateinit var azureTokenEndpoint: String

    @Value("\${syfobehandlendeenhet.url}")
    private lateinit var behandlendeenhetUrl: String

    @Autowired
    private lateinit var motebehovArbeidsgiverController: MotebehovArbeidsgiverControllerV5

    @Autowired
    private lateinit var motebehovDAO: MotebehovDAO

    @Autowired
    private lateinit var cacheManager: CacheManager

    @Autowired
    private lateinit var oppfolgingstilfelleDAO: OppfolgingstilfelleDAO

    @Autowired
    private lateinit var dialogmotekandidatDAO: DialogmotekandidatDAO

    @Autowired
    @Qualifier("AzureAD")
    private lateinit var restTemplateAzureAD: RestTemplate

    @Autowired
    private lateinit var restTemplate: RestTemplate

    @Autowired
    private lateinit var tokenValidationUtil: TokenValidationUtil

    @MockkBean(relaxed = true)
    private lateinit var esyfovarselService: EsyfovarselService

    @MockkBean
    private lateinit var dineSykmeldteConsumer: DineSykmeldteConsumer

    @MockkBean(relaxed = true)
    private lateinit var pdlConsumer: PdlConsumer

    @MockkBean(relaxed = true)
    private lateinit var personoppgavehendelseProducer: PersonoppgavehendelseProducer

    private lateinit var mockRestServiceServerAzureAD: MockRestServiceServer
    private lateinit var mockRestServiceServer: MockRestServiceServer

    init {
        beforeTest {
            every { dineSykmeldteConsumer.getSykmeldt(NARMESTE_LEDER_ID) } returns
                DineSykmeldteResponse(
                    fnr = ARBEIDSTAKER_FNR,
                    orgnummer = VIRKSOMHETSNUMMER,
                )
            every { pdlConsumer.aktorid(ARBEIDSTAKER_FNR) } returns ARBEIDSTAKER_AKTORID
            every { pdlConsumer.aktorid(LEDER_FNR) } returns LEDER_AKTORID

            mockRestServiceServer = MockRestServiceServer.bindTo(restTemplate).build()
            mockRestServiceServerAzureAD = MockRestServiceServer.bindTo(restTemplateAzureAD).build()
            cleanDB()
        }
        afterEach {
            mockRestServiceServer.reset()
            mockRestServiceServerAzureAD.reset()
            cacheManager.cacheNames
                .forEach(
                    Consumer { cacheName: String ->
                        val cache = cacheManager.getCache(cacheName)
                        cache?.clear()
                    },
                )
            cleanDB()
            AzureAdV2TokenConsumer.Companion.clearCache()
        }

        describe("MotebehovArbeidsgiverControllerV5") {
            it("henter status for arbeidstakeren fra den autoriserte nærmeste-leder-relasjonen") {
                tokenValidationUtil.logInAsDialogmoteUser(LEDER_FNR)

                motebehovArbeidsgiverController
                    .motebehovStatusArbeidsgiver(NARMESTE_LEDER_ID)
                    .assertMotebehovStatus(expVisMotebehov = false)

                verify(exactly = 1) { dineSykmeldteConsumer.getSykmeldt(NARMESTE_LEDER_ID) }
            }

            it("avviser status når nærmeste-leder-relasjonen ikke finnes") {
                tokenValidationUtil.logInAsDialogmoteUser(LEDER_FNR)
                every { dineSykmeldteConsumer.getSykmeldt(NARMESTE_LEDER_ID) } returns null

                shouldThrow<ForbiddenException> {
                    motebehovArbeidsgiverController.motebehovStatusArbeidsgiver(NARMESTE_LEDER_ID)
                }
            }

            it("lagrer møtebehov med arbeidstaker og virksomhet fra nærmeste-leder-relasjonen") {
                tokenValidationUtil.logInAsDialogmoteUser(LEDER_FNR)
                dbCreateOppfolgingstilfelle(
                    oppfolgingstilfelleDAO,
                    generateOppfolgingstilfellePerson(),
                )
                createKandidatInDB(ARBEIDSTAKER_FNR)
                mockAndExpectBehandlendeEnhetRequest(
                    azureTokenEndpoint,
                    mockRestServiceServerAzureAD,
                    mockRestServiceServer,
                    behandlendeenhetUrl,
                    ARBEIDSTAKER_FNR,
                )
                val formSubmission =
                    MotebehovFormSubmissionDTO(
                        harMotebehov = true,
                        formSnapshot = mockArbeidsgiverSvarJaOnskerSykmelderFormSnapshot,
                    )

                motebehovArbeidsgiverController.lagreMotebehovArbeidsgiver(
                    NyttMotebehovArbeidsgiverV5DTO(
                        narmesteLederId = NARMESTE_LEDER_ID,
                        formSubmission = formSubmission,
                    ),
                )

                val motebehovStatus =
                    motebehovArbeidsgiverController.motebehovStatusArbeidsgiver(NARMESTE_LEDER_ID)
                val motebehov = motebehovStatus.motebehov!!
                assertThat(motebehov.arbeidstakerFnr).isEqualTo(ARBEIDSTAKER_FNR)
                assertThat(motebehov.virksomhetsnummer).isEqualTo(VIRKSOMHETSNUMMER)
            }

            it("bevarer egen-leder-håndtering når relasjonen peker på innlogget ident") {
                tokenValidationUtil.logInAsDialogmoteUser(ARBEIDSTAKER_FNR)
                dbCreateOppfolgingstilfelle(
                    oppfolgingstilfelleDAO,
                    generateOppfolgingstilfellePerson(
                        virksomhetsnummerList = listOf(VIRKSOMHETSNUMMER),
                    ),
                )
                createKandidatInDB(ARBEIDSTAKER_FNR)
                mockAndExpectBehandlendeEnhetRequest(
                    azureTokenEndpoint,
                    mockRestServiceServerAzureAD,
                    mockRestServiceServer,
                    behandlendeenhetUrl,
                    ARBEIDSTAKER_FNR,
                )

                motebehovArbeidsgiverController.lagreMotebehovArbeidsgiver(
                    NyttMotebehovArbeidsgiverV5DTO(
                        narmesteLederId = NARMESTE_LEDER_ID,
                        formSubmission =
                            MotebehovFormSubmissionDTO(
                                harMotebehov = true,
                                formSnapshot = mockArbeidsgiverSvarJaOnskerSykmelderFormSnapshot,
                            ),
                    ),
                )

                verify(exactly = 1) {
                    esyfovarselService.ferdigstillSvarMotebehovForArbeidsgiver(
                        ARBEIDSTAKER_FNR,
                        ARBEIDSTAKER_FNR,
                        VIRKSOMHETSNUMMER,
                    )
                }
                verify(exactly = 1) {
                    esyfovarselService.ferdigstillSvarMotebehovForArbeidstaker(ARBEIDSTAKER_FNR)
                }
            }
        }
    }

    private fun cleanDB() {
        motebehovDAO.nullstillMotebehov(ARBEIDSTAKER_AKTORID)
        motebehovDAO.nullstillMotebehov(LEDER_AKTORID)
        oppfolgingstilfelleDAO.nullstillOppfolgingstilfeller(ARBEIDSTAKER_FNR)
        oppfolgingstilfelleDAO.nullstillOppfolgingstilfeller(LEDER_AKTORID)
        dialogmotekandidatDAO.delete(ARBEIDSTAKER_FNR)
        dialogmotekandidatDAO.delete(LEDER_FNR)
    }

    private fun createKandidatInDB(fnr: String) {
        dialogmotekandidatDAO.create(
            dialogmotekandidatExternalUUID = UUID.randomUUID().toString(),
            createdAt = LocalDateTime.now().minusDays(DAYS_START_SVAR_BEHOV),
            fnr = fnr,
            kandidat = true,
            arsak = DialogmotekandidatEndringArsak.STOPPUNKT.name,
        )
    }
}
