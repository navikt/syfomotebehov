package no.nav.syfo.motebehov.api

import com.ninjasquad.springmockk.MockkBean
import io.kotest.extensions.spring.SpringExtension
import io.mockk.every
import io.mockk.verify
import no.nav.syfo.IntegrationTest
import no.nav.syfo.LocalApplication
import no.nav.syfo.consumer.azuread.v2.AzureAdV2TokenConsumer
import no.nav.syfo.consumer.brukertilgang.BrukertilgangConsumer
import no.nav.syfo.consumer.pdl.PdlConsumer
import no.nav.syfo.dialogmotekandidat.database.DialogmotekandidatDAO
import no.nav.syfo.dialogmotekandidat.database.DialogmotekandidatEndringArsak
import no.nav.syfo.motebehov.MotebehovFormSubmissionDTO
import no.nav.syfo.motebehov.NyttMotebehovArbeidsgiverFormSubmissionDTO
import no.nav.syfo.motebehov.api.internad.v4.MotebehovVeilederADControllerV4
import no.nav.syfo.motebehov.database.MotebehovDAO
import no.nav.syfo.motebehov.formSnapshot.mockArbeidsgiverSvarJaOnskerSykmelderFormSnapshot
import no.nav.syfo.motebehov.motebehovstatus.DAYS_END_SVAR_BEHOV
import no.nav.syfo.motebehov.motebehovstatus.DAYS_START_SVAR_BEHOV
import no.nav.syfo.motebehov.motebehovstatus.MotebehovSkjemaType
import no.nav.syfo.motebehov.toMotebehovFormValuesOutputDTO
import no.nav.syfo.oppfolgingstilfelle.database.OppfolgingstilfelleDAO
import no.nav.syfo.personoppgavehendelse.PersonoppgavehendelseProducer
import no.nav.syfo.testhelper.UserConstants.ARBEIDSTAKER_AKTORID
import no.nav.syfo.testhelper.UserConstants.ARBEIDSTAKER_FNR
import no.nav.syfo.testhelper.UserConstants.LEDER_AKTORID
import no.nav.syfo.testhelper.UserConstants.LEDER_FNR
import no.nav.syfo.testhelper.UserConstants.VEILEDER_ID
import no.nav.syfo.testhelper.UserConstants.VIRKSOMHETSNUMMER
import no.nav.syfo.testhelper.UserConstants.VIRKSOMHETSNUMMER_2
import no.nav.syfo.testhelper.assertion.assertMotebehovStatus
import no.nav.syfo.testhelper.clearCache
import no.nav.syfo.testhelper.generator.MotebehovGenerator
import no.nav.syfo.testhelper.generator.generateOppfolgingstilfellePerson
import no.nav.syfo.testhelper.generator.generatePdlHentPerson
import no.nav.syfo.testhelper.mockAndExpectBehandlendeEnhetRequest
import no.nav.syfo.testhelper.mockAndExpectBehandlendeEnhetRequestWithTilgangskontroll
import no.nav.syfo.util.TokenValidationUtil
import no.nav.syfo.varsel.esyfovarsel.EsyfovarselService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.cache.CacheManager
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.web.client.RestTemplate
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import java.util.function.Consumer

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestConfiguration
@SpringBootTest(classes = [LocalApplication::class])
class MotebehovArbeidsgiverControllerV4Test : IntegrationTest() {
    @Value("\${azure.openid.config.token.endpoint}")
    private lateinit var azureTokenEndpoint: String

    @Value("\${syfobehandlendeenhet.url}")
    private lateinit var behandlendeenhetUrl: String

    @Value("\${istilgangskontroll.url}")
    private lateinit var tilgangskontrollUrl: String

    @Autowired
    private lateinit var motebehovArbeidsgiverController: MotebehovArbeidsgiverControllerV4

    @Autowired
    private lateinit var motebehovVeilederController: MotebehovVeilederADControllerV4

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

    @MockkBean(relaxed = true)
    private lateinit var pdlConsumer: PdlConsumer

    @MockkBean
    private lateinit var brukertilgangConsumer: BrukertilgangConsumer

    @MockkBean(relaxed = true)
    private lateinit var personoppgavehendelseProducer: PersonoppgavehendelseProducer

    private lateinit var mockRestServiceServerAzureAD: MockRestServiceServer
    private lateinit var mockRestServiceServer: MockRestServiceServer

    private val motebehovGenerator = MotebehovGenerator()

    init {
        extensions(SpringExtension)
        beforeTest {

            every { brukertilgangConsumer.hasAccessToAnsatt(ARBEIDSTAKER_FNR) } returns true
            every { brukertilgangConsumer.hasAccessToAnsatt(LEDER_FNR) } returns true
            every { pdlConsumer.person(ARBEIDSTAKER_FNR) } returns generatePdlHentPerson(null, null)
            every { pdlConsumer.aktorid(ARBEIDSTAKER_FNR) } returns ARBEIDSTAKER_AKTORID
            every { pdlConsumer.aktorid(LEDER_FNR) } returns LEDER_AKTORID

            every { pdlConsumer.isKode6(ARBEIDSTAKER_FNR) } returns false

            mockRestServiceServer = MockRestServiceServer.bindTo(restTemplate).build()
            mockRestServiceServerAzureAD = MockRestServiceServer.bindTo(restTemplateAzureAD).build()
            cleanDB()
        }
        afterEach {
            resetMockRestServers()
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
        afterTest {
            tokenValidationUtil.resetAll()
        }

        describe("MotebehovArbeidsgiverControllerV4") {
            it("getMotebehovStatusWithNoOppfolgingstilfelle") {
                tokenValidationUtil.logInAsDialogmoteUser(LEDER_FNR)
                motebehovArbeidsgiverController.motebehovStatusArbeidsgiver(ARBEIDSTAKER_FNR, VIRKSOMHETSNUMMER)
                    .assertMotebehovStatus(expVisMotebehov = false)
            }

            it("getMotebehovStatusWithTodayOutsideOppfolgingstilfelleStart") {
                tokenValidationUtil.logInAsDialogmoteUser(LEDER_FNR)
                dbCreateOppfolgingstilfelle(
                    oppfolgingstilfelleDAO,
                    generateOppfolgingstilfellePerson(
                        start = LocalDate.now().plusDays(1),
                        end = LocalDate.now().plusDays(10),
                    ),
                )

                motebehovArbeidsgiverController.motebehovStatusArbeidsgiver(ARBEIDSTAKER_FNR, VIRKSOMHETSNUMMER)
                    .assertMotebehovStatus(expVisMotebehov = false)
            }

            it("getMotebehovStatusWithTodayOutsideOppfolgingstilfelleEnd") {
                tokenValidationUtil.logInAsDialogmoteUser(LEDER_FNR)
                dbCreateOppfolgingstilfelle(
                    oppfolgingstilfelleDAO,
                    generateOppfolgingstilfellePerson(
                        start = LocalDate.now().minusDays(17),
                        end = LocalDate.now().minusDays(16),
                    ),
                )

                motebehovArbeidsgiverController.motebehovStatusArbeidsgiver(ARBEIDSTAKER_FNR, VIRKSOMHETSNUMMER)
                    .assertMotebehovStatus(expVisMotebehov = false)
            }

            it(
                "getMotebehovStatusWithTodayInsideOppfolgingstilfelleMergedByActiveAndExpired" +
                    "OppfolgingstilfelleNoOverlapVirksomhetWithoutActiveOppfolgingstilfelle"
            ) {
                val activeOppfolgingstilfelleStartDate =
                    LocalDate.now().minusDays(DAYS_START_SVAR_BEHOV).plusDays(1)
                tokenValidationUtil.logInAsDialogmoteUser(LEDER_FNR)
                dbCreateOppfolgingstilfelle(
                    oppfolgingstilfelleDAO,
                    generateOppfolgingstilfellePerson(
                        start = activeOppfolgingstilfelleStartDate.minusDays(2),
                        end = activeOppfolgingstilfelleStartDate.minusDays(1),
                    ),
                )

                dbCreateOppfolgingstilfelle(
                    oppfolgingstilfelleDAO,
                    generateOppfolgingstilfellePerson(
                        start = activeOppfolgingstilfelleStartDate,
                        end = LocalDate.now().plusDays(1),
                        virksomhetsnummerList = listOf(VIRKSOMHETSNUMMER_2),
                    ),
                )

                motebehovArbeidsgiverController.motebehovStatusArbeidsgiver(ARBEIDSTAKER_FNR, VIRKSOMHETSNUMMER)
                    .assertMotebehovStatus(expVisMotebehov = false)
            }

            it(
                "getMotebehovStatusWithTodayInsideOppfolgingstilfelleMergedByActiveAndExpired" +
                    "OppfolgingstilfelleNoOverlap"
            ) {
                val activeOppfolgingstilfelleStartDate = LocalDate.now().minusDays(DAYS_START_SVAR_BEHOV).plusDays(1)
                tokenValidationUtil.logInAsDialogmoteUser(LEDER_FNR)

                dbCreateOppfolgingstilfelle(
                    oppfolgingstilfelleDAO,
                    generateOppfolgingstilfellePerson(
                        start = activeOppfolgingstilfelleStartDate.minusDays(2),
                        end = activeOppfolgingstilfelleStartDate.minusDays(1),
                        virksomhetsnummerList = listOf(VIRKSOMHETSNUMMER_2),
                    ),
                )

                dbCreateOppfolgingstilfelle(
                    oppfolgingstilfelleDAO,
                    generateOppfolgingstilfellePerson(
                        start = activeOppfolgingstilfelleStartDate,
                        end = LocalDate.now().plusDays(1),
                        virksomhetsnummerList = listOf(VIRKSOMHETSNUMMER),
                    ),
                )

                motebehovArbeidsgiverController.motebehovStatusArbeidsgiver(ARBEIDSTAKER_FNR, VIRKSOMHETSNUMMER)
                    .assertMotebehovStatus(expVisMotebehov = true, expSkjemaType = MotebehovSkjemaType.MELD_BEHOV)
            }

            it(
                "getMotebehovStatusWithTodayInsideOppfolgingstilfelleMergedByActiveAndExpired" +
                    "OppfolgingstilfelleWithOverlap"
            ) {
                val activeOppfolgingstilfelleStartDate =
                    LocalDate.now().minusDays(DAYS_START_SVAR_BEHOV).plusDays(1)
                tokenValidationUtil.logInAsDialogmoteUser(LEDER_FNR)

                dbCreateOppfolgingstilfelle(
                    oppfolgingstilfelleDAO,
                    generateOppfolgingstilfellePerson(
                        start = activeOppfolgingstilfelleStartDate.minusDays(2),
                        end = activeOppfolgingstilfelleStartDate,
                        virksomhetsnummerList = listOf(VIRKSOMHETSNUMMER_2),
                    ),
                )

                dbCreateOppfolgingstilfelle(
                    oppfolgingstilfelleDAO,
                    generateOppfolgingstilfellePerson(
                        start = activeOppfolgingstilfelleStartDate,
                        end = LocalDate.now().plusDays(1),
                        virksomhetsnummerList = listOf(VIRKSOMHETSNUMMER),
                    ),
                )

                createKandidatInDB(ARBEIDSTAKER_FNR)

                motebehovArbeidsgiverController.motebehovStatusArbeidsgiver(ARBEIDSTAKER_FNR, VIRKSOMHETSNUMMER)
                    .assertMotebehovStatus(expVisMotebehov = true, expSkjemaType = MotebehovSkjemaType.SVAR_BEHOV)
            }

            it("getMotebehovStatusWithTodayInsideOppfolgingstilfelleMergedBy2Oppfolgingstilfeller") {
                tokenValidationUtil.logInAsDialogmoteUser(LEDER_FNR)
                dbCreateOppfolgingstilfelle(
                    oppfolgingstilfelleDAO,
                    generateOppfolgingstilfellePerson(
                        start = LocalDate.now().minusDays(DAYS_END_SVAR_BEHOV).minusDays(1),
                        end = LocalDate.now(),
                        virksomhetsnummerList = listOf(VIRKSOMHETSNUMMER),
                    ),
                )

                dbCreateOppfolgingstilfelle(
                    oppfolgingstilfelleDAO,
                    generateOppfolgingstilfellePerson(
                        start = LocalDate.now().minusDays(2),
                        end = LocalDate.now().plusDays(1),
                        virksomhetsnummerList = listOf(VIRKSOMHETSNUMMER_2),
                    ),
                )

                motebehovArbeidsgiverController.motebehovStatusArbeidsgiver(ARBEIDSTAKER_FNR, VIRKSOMHETSNUMMER)
                    .assertMotebehovStatus(expVisMotebehov = true, expSkjemaType = MotebehovSkjemaType.MELD_BEHOV)
            }

            it("getMotebehovStatusWithTodayInsideOppfolgingstilfelleDay1") {
                tokenValidationUtil.logInAsDialogmoteUser(LEDER_FNR)
                dbCreateOppfolgingstilfelle(
                    oppfolgingstilfelleDAO,
                    generateOppfolgingstilfellePerson(
                        start = LocalDate.now(),
                        end = LocalDate.now(),
                    ),
                )

                motebehovArbeidsgiverController.motebehovStatusArbeidsgiver(ARBEIDSTAKER_FNR, VIRKSOMHETSNUMMER)
                    .assertMotebehovStatus(expVisMotebehov = true, expSkjemaType = MotebehovSkjemaType.MELD_BEHOV)
            }

            it("getMotebehovStatusWithTodayInsideOppfolgingstilfelleLastDay") {
                tokenValidationUtil.logInAsDialogmoteUser(LEDER_FNR)
                dbCreateOppfolgingstilfelle(
                    oppfolgingstilfelleDAO,
                    generateOppfolgingstilfellePerson(
                        start = LocalDate.now().minusDays(DAYS_END_SVAR_BEHOV).minusDays(1),
                        end = LocalDate.now(),
                    ),
                )

                motebehovArbeidsgiverController.motebehovStatusArbeidsgiver(ARBEIDSTAKER_FNR, VIRKSOMHETSNUMMER)
                    .assertMotebehovStatus(expVisMotebehov = true, expSkjemaType = MotebehovSkjemaType.MELD_BEHOV)
            }

            it("getMotebehovStatusWithTodayInsideOppfolgingstilfelleMedBehov") {
                tokenValidationUtil.logInAsDialogmoteUser(LEDER_FNR)
                dbCreateOppfolgingstilfelle(
                    oppfolgingstilfelleDAO,
                    generateOppfolgingstilfellePerson(
                        start = LocalDate.now(),
                        end = LocalDate.now().plusDays(1),
                    ),
                )

                motebehovArbeidsgiverController.motebehovStatusArbeidsgiver(ARBEIDSTAKER_FNR, VIRKSOMHETSNUMMER)
                    .assertMotebehovStatus(expVisMotebehov = true, expSkjemaType = MotebehovSkjemaType.MELD_BEHOV)
            }

            it("getMotebehovStatusWithTodayInsideOppfolgingstilfelleMeldBehovSubmittedAndBehandlet") {
                tokenValidationUtil.logInAsDialogmoteUser(LEDER_FNR)
                dbCreateOppfolgingstilfelle(
                    oppfolgingstilfelleDAO,
                    generateOppfolgingstilfellePerson(
                        start = LocalDate.now(),
                        end = LocalDate.now().plusDays(1),
                    ),
                )

                val arbeidsgiverFormSubmissionMeld = motebehovGenerator.lagNyArbeidsgiverFormSubmissionMeld()
                submitMotebehovAndSendOversikthendelse(arbeidsgiverFormSubmissionMeld)

                resetMockRestServers()

                mockBehandlendEnhetWithTilgangskontroll(ARBEIDSTAKER_FNR)
                tokenValidationUtil.logInAsNavCounselor(VEILEDER_ID)
                motebehovVeilederController.behandleMotebehov(ARBEIDSTAKER_FNR)

                resetMockRestServers()
                tokenValidationUtil.logInAsDialogmoteUser(LEDER_FNR)
                motebehovArbeidsgiverController.motebehovStatusArbeidsgiver(ARBEIDSTAKER_FNR, VIRKSOMHETSNUMMER)
                    .assertMotebehovStatus(expVisMotebehov = true, expSkjemaType = MotebehovSkjemaType.MELD_BEHOV)
            }

            it(
                "getMotebehovStatusWithTodayInsideOppfolgingstilfelleMeldBehovMoteplanleggerActiveMeldBehovSubmitted"
            ) {
                tokenValidationUtil.logInAsDialogmoteUser(LEDER_FNR)
                dbCreateOppfolgingstilfelle(
                    oppfolgingstilfelleDAO,
                    generateOppfolgingstilfellePerson(
                        start = LocalDate.now(),
                        end = LocalDate.now().plusDays(1),
                    ),
                )

                val arbeidsgiverFormSubmissionMeld = motebehovGenerator.lagNyArbeidsgiverFormSubmissionMeld()
                val formValuesOutputDTOThatShouldBeCreated = arbeidsgiverFormSubmissionMeld.formSubmission
                    .toMotebehovFormValuesOutputDTO()

                submitMotebehovAndSendOversikthendelse(arbeidsgiverFormSubmissionMeld)

                mockRestServiceServer.reset()

                motebehovArbeidsgiverController.motebehovStatusArbeidsgiver(ARBEIDSTAKER_FNR, VIRKSOMHETSNUMMER)
                    .assertMotebehovStatus(
                        expVisMotebehov = true,
                        expSkjemaType = MotebehovSkjemaType.MELD_BEHOV,
                        expMotebehovFormValues = formValuesOutputDTOThatShouldBeCreated
                    )
            }

            it("getMotebehovStatusWithTodayInsideOppfolgingstilfelleBeforeSvarBehovStartDate") {
                tokenValidationUtil.logInAsDialogmoteUser(LEDER_FNR)
                dbCreateOppfolgingstilfelle(
                    oppfolgingstilfelleDAO,
                    generateOppfolgingstilfellePerson(
                        start = LocalDate.now().minusDays(DAYS_START_SVAR_BEHOV).plusDays(1),
                        end = LocalDate.now().plusDays(1),
                    ),
                )

                motebehovArbeidsgiverController.motebehovStatusArbeidsgiver(ARBEIDSTAKER_FNR, VIRKSOMHETSNUMMER)
                    .assertMotebehovStatus(expVisMotebehov = true, expSkjemaType = MotebehovSkjemaType.MELD_BEHOV)
            }

            it("getMotebehovStatusWithTodayInsideOppfolgingstilfelleAfterSvarBehovEndDate") {
                tokenValidationUtil.logInAsDialogmoteUser(LEDER_FNR)
                dbCreateOppfolgingstilfelle(
                    oppfolgingstilfelleDAO,
                    generateOppfolgingstilfellePerson(
                        start = LocalDate.now().minusDays(DAYS_END_SVAR_BEHOV),
                        end = LocalDate.now().plusDays(1),
                    ),
                )

                motebehovArbeidsgiverController.motebehovStatusArbeidsgiver(ARBEIDSTAKER_FNR, VIRKSOMHETSNUMMER)
                    .assertMotebehovStatus(expVisMotebehov = true, expSkjemaType = MotebehovSkjemaType.MELD_BEHOV)
            }

            it("getMotebehovStatusWithNoMotebehovInsideSvarBehovUpperLimit") {
                tokenValidationUtil.logInAsDialogmoteUser(LEDER_FNR)
                createKandidatInDB(ARBEIDSTAKER_FNR)

                dbCreateOppfolgingstilfelle(
                    oppfolgingstilfelleDAO,
                    generateOppfolgingstilfellePerson(
                        start = LocalDate.now().minusDays(DAYS_END_SVAR_BEHOV).plusDays(1),
                        end = LocalDate.now().plusDays(1),
                    ),
                )

                motebehovArbeidsgiverController.motebehovStatusArbeidsgiver(ARBEIDSTAKER_FNR, VIRKSOMHETSNUMMER)
                    .assertMotebehovStatus(expVisMotebehov = true, expSkjemaType = MotebehovSkjemaType.SVAR_BEHOV)
            }

            it("getMotebehovStatusWithSvarBehovAndMoteCreated") {
                tokenValidationUtil.logInAsDialogmoteUser(LEDER_FNR)
                createKandidatInDB(ARBEIDSTAKER_FNR)

                dbCreateOppfolgingstilfelle(
                    oppfolgingstilfelleDAO,
                    generateOppfolgingstilfellePerson(
                        start = LocalDate.now().minusDays(DAYS_END_SVAR_BEHOV).plusDays(1),
                        end = LocalDate.now().plusDays(1),
                    ),
                )

                val arbeidsgiverFormSubmissionSvarJa = motebehovGenerator.lagNyArbeidsgiverFormSubmissionSvarJa()
                val formValuesOutputDTOThatShouldBeCreated = arbeidsgiverFormSubmissionSvarJa.formSubmission
                    .toMotebehovFormValuesOutputDTO()

                submitMotebehovAndSendOversikthendelse(arbeidsgiverFormSubmissionSvarJa)
                verify { esyfovarselService.ferdigstillSvarMotebehovForArbeidsgiver(any(), ARBEIDSTAKER_FNR, any()) }
                verify(exactly = 0) { esyfovarselService.ferdigstillSvarMotebehovForArbeidstaker(ARBEIDSTAKER_FNR) }

                mockRestServiceServer.reset()

                motebehovArbeidsgiverController.motebehovStatusArbeidsgiver(ARBEIDSTAKER_FNR, VIRKSOMHETSNUMMER)
                    .assertMotebehovStatus(
                        expVisMotebehov = true,
                        expSkjemaType = MotebehovSkjemaType.SVAR_BEHOV,
                        expMotebehovFormValues = formValuesOutputDTOThatShouldBeCreated
                    )
            }

            it("getMotebehovStatusWithNoMotebehovAndNoMoteInsideSvarBehovLowerLimit") {
                tokenValidationUtil.logInAsDialogmoteUser(LEDER_FNR)
                createKandidatInDB(ARBEIDSTAKER_FNR)

                dbCreateOppfolgingstilfelle(
                    oppfolgingstilfelleDAO,
                    generateOppfolgingstilfellePerson(
                        start = LocalDate.now().minusDays(DAYS_START_SVAR_BEHOV),
                        end = LocalDate.now().plusDays(1),
                    ),
                )

                motebehovArbeidsgiverController.motebehovStatusArbeidsgiver(ARBEIDSTAKER_FNR, VIRKSOMHETSNUMMER)
                    .assertMotebehovStatus(expVisMotebehov = true, expSkjemaType = MotebehovSkjemaType.SVAR_BEHOV)
            }

            it("getMotebehovStatusWithNoMotebehovAndNoMote") {
                tokenValidationUtil.logInAsDialogmoteUser(LEDER_FNR)
                dbCreateOppfolgingstilfelle(
                    oppfolgingstilfelleDAO,
                    generateOppfolgingstilfellePerson(),
                )

                createKandidatInDB(ARBEIDSTAKER_FNR)

                motebehovArbeidsgiverController.motebehovStatusArbeidsgiver(ARBEIDSTAKER_FNR, VIRKSOMHETSNUMMER)
                    .assertMotebehovStatus(expVisMotebehov = true, expSkjemaType = MotebehovSkjemaType.SVAR_BEHOV)
            }

            it("getMotebehovStatusAndSendOversikthendelseWithMotebehovHarBehovTrue") {
                tokenValidationUtil.logInAsDialogmoteUser(LEDER_FNR)
                dbCreateOppfolgingstilfelle(
                    oppfolgingstilfelleDAO,
                    generateOppfolgingstilfellePerson(),
                )
                val motebehov = motebehovGenerator.lagNyArbeidsgiverFormSubmissionSvarJa()

                lagreMotebehov(motebehov)
                verifyMotebehovStatus(motebehov.formSubmission)
            }

            it("getMotebehovStatusAndSendOversikthendelseWithMotebehovHarBehovFalse") {
                tokenValidationUtil.logInAsDialogmoteUser(LEDER_FNR)
                dbCreateOppfolgingstilfelle(
                    oppfolgingstilfelleDAO,
                    generateOppfolgingstilfellePerson(),
                )

                val motebehov = motebehovGenerator.lagNyArbeidsgiverFormSubmissionSvarNei()

                lagreMotebehov(motebehov)
                verifyMotebehovStatus(motebehov.formSubmission)
            }

            it("innsendtMotebehovForEgenLederFerdigstillerOgsaaSykmeldtVarsel") {
                cleanDB()
                tokenValidationUtil.logInAsDialogmoteUser(LEDER_FNR)
                dbCreateOppfolgingstilfelle(
                    oppfolgingstilfelleDAO,
                    generateOppfolgingstilfellePerson(virksomhetsnummerList = listOf(VIRKSOMHETSNUMMER)).copy(
                        personIdentNumber = LEDER_FNR,
                    ),
                )
                val motebehov = NyttMotebehovArbeidsgiverFormSubmissionDTO(
                    arbeidstakerFnr = LEDER_FNR,
                    virksomhetsnummer = VIRKSOMHETSNUMMER,
                    formSubmission = MotebehovFormSubmissionDTO(
                        harMotebehov = true,
                        formSnapshot = mockArbeidsgiverSvarJaOnskerSykmelderFormSnapshot
                    )

                )

                lagreMotebehov(motebehov)

                verify(exactly = 1) {
                    esyfovarselService.ferdigstillSvarMotebehovForArbeidsgiver(
                        LEDER_FNR,
                        LEDER_FNR,
                        VIRKSOMHETSNUMMER
                    )
                }
                verify(exactly = 1) { esyfovarselService.ferdigstillSvarMotebehovForArbeidstaker(LEDER_FNR) }
            }
        }
    }

    private fun submitMotebehovAndSendOversikthendelse(
        arbeidsgiverFormSubmissionInputDTO: NyttMotebehovArbeidsgiverFormSubmissionDTO
    ) {
        mockAndExpectBehandlendeEnhetRequest(
            azureTokenEndpoint,
            mockRestServiceServerAzureAD,
            mockRestServiceServer,
            behandlendeenhetUrl,
            ARBEIDSTAKER_FNR,
        )

        motebehovArbeidsgiverController.lagreMotebehovArbeidsgiver(arbeidsgiverFormSubmissionInputDTO)
        if (arbeidsgiverFormSubmissionInputDTO.formSubmission.harMotebehov) {
            verify { personoppgavehendelseProducer.sendPersonoppgavehendelse(any(), any()) }
        } else {
            verify(exactly = 0) { personoppgavehendelseProducer.sendPersonoppgavehendelse(any(), any()) }
        }
    }

    private fun lagreMotebehov(innsendtMotebehov: NyttMotebehovArbeidsgiverFormSubmissionDTO) {
        mockAndExpectBehandlendeEnhetRequest(
            azureTokenEndpoint,
            mockRestServiceServerAzureAD,
            mockRestServiceServer,
            behandlendeenhetUrl,
            innsendtMotebehov.arbeidstakerFnr,
        )

        createKandidatInDB(innsendtMotebehov.arbeidstakerFnr)

        motebehovArbeidsgiverController.lagreMotebehovArbeidsgiver(
            innsendtMotebehov
        )

        if (!innsendtMotebehov.formSubmission.harMotebehov) {
            mockRestServiceServer.reset()
        }
    }

    private fun verifyMotebehovStatus(innsendtFormSubmission: MotebehovFormSubmissionDTO) {
        val motebehovStatus = motebehovArbeidsgiverController.motebehovStatusArbeidsgiver(
            ARBEIDSTAKER_FNR,
            VIRKSOMHETSNUMMER,
        )
        val formValuesOutputDTOThatShouldBeCreated = innsendtFormSubmission
            .toMotebehovFormValuesOutputDTO()

        assertTrue(motebehovStatus.visMotebehov)
        assertEquals(MotebehovSkjemaType.SVAR_BEHOV, motebehovStatus.skjemaType)
        val motebehov = motebehovStatus.motebehovWithFormValues!!
        assertNotNull(motebehov)
        assertThat(motebehov.opprettetAv).isEqualTo(LEDER_AKTORID)
        assertThat(motebehov.arbeidstakerFnr).isEqualTo(ARBEIDSTAKER_FNR)
        assertThat(motebehov.virksomhetsnummer).isEqualTo(VIRKSOMHETSNUMMER)
        assertThat(motebehov.skjemaType).isEqualTo(motebehovStatus.skjemaType)

        assertThat(motebehov.formValues).usingRecursiveComparison()
            .isEqualTo(formValuesOutputDTOThatShouldBeCreated)

        if (innsendtFormSubmission.harMotebehov) {
            verify { personoppgavehendelseProducer.sendPersonoppgavehendelse(any(), any()) }
        } else {
            verify(exactly = 0) { personoppgavehendelseProducer.sendPersonoppgavehendelse(any(), any()) }
        }
        verify { esyfovarselService.ferdigstillSvarMotebehovForArbeidsgiver(any(), any(), any()) }
        verify(exactly = 0) { esyfovarselService.ferdigstillSvarMotebehovForArbeidstaker(motebehov.arbeidstakerFnr) }
    }

    private fun resetMockRestServers() {
        mockRestServiceServer.reset()
        mockRestServiceServerAzureAD.reset()
    }

    private fun cleanDB() {
        motebehovDAO.nullstillMotebehov(ARBEIDSTAKER_AKTORID)
        motebehovDAO.nullstillMotebehov(LEDER_AKTORID)
        oppfolgingstilfelleDAO.nullstillOppfolgingstilfeller(ARBEIDSTAKER_FNR)
        oppfolgingstilfelleDAO.nullstillOppfolgingstilfeller(LEDER_AKTORID)
        dialogmotekandidatDAO.delete(ARBEIDSTAKER_FNR)
        dialogmotekandidatDAO.delete(LEDER_FNR)
    }

    private fun mockBehandlendEnhetWithTilgangskontroll(fnr: String) {
        mockAndExpectBehandlendeEnhetRequestWithTilgangskontroll(
            azureTokenEndpoint,
            mockRestServiceServerAzureAD,
            mockRestServiceServer,
            behandlendeenhetUrl,
            tilgangskontrollUrl,
            fnr,
        )
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
