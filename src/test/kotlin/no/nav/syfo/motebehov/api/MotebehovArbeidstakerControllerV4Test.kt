package no.nav.syfo.motebehov.api

import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.extensions.ApplyExtension
import io.kotest.extensions.spring.SpringExtension
import io.mockk.every
import io.mockk.verify
import no.nav.syfo.IntegrationTest
import no.nav.syfo.LocalApplication
import no.nav.syfo.consumer.azuread.v2.AzureAdV2TokenConsumer
import no.nav.syfo.consumer.pdl.PdlConsumer
import no.nav.syfo.dialogmotekandidat.database.DialogmotekandidatDAO
import no.nav.syfo.dialogmotekandidat.database.DialogmotekandidatEndringArsak
import no.nav.syfo.motebehov.MotebehovFormSubmissionDTO
import no.nav.syfo.motebehov.api.internad.v4.MotebehovVeilederADControllerV4
import no.nav.syfo.motebehov.database.MotebehovDAO
import no.nav.syfo.motebehov.motebehovstatus.DAYS_END_SVAR_BEHOV
import no.nav.syfo.motebehov.motebehovstatus.DAYS_START_SVAR_BEHOV
import no.nav.syfo.motebehov.motebehovstatus.MotebehovSkjemaType
import no.nav.syfo.motebehov.toMotebehovFormValuesOutputDTO
import no.nav.syfo.oppfolgingstilfelle.database.OppfolgingstilfelleDAO
import no.nav.syfo.personoppgavehendelse.PersonoppgavehendelseProducer
import no.nav.syfo.testhelper.UserConstants.ARBEIDSTAKER_AKTORID
import no.nav.syfo.testhelper.UserConstants.ARBEIDSTAKER_FNR
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

@TestConfiguration
@SpringBootTest(classes = [LocalApplication::class])
@ApplyExtension(SpringExtension::class)
class MotebehovArbeidstakerControllerV4Test : IntegrationTest() {
    @Value("\${azure.openid.config.token.endpoint}")
    private lateinit var azureTokenEndpoint: String

    @Value("\${syfobehandlendeenhet.url}")
    private lateinit var behandlendeenhetUrl: String

    @Value("\${istilgangskontroll.url}")
    private lateinit var tilgangskontrollUrl: String

    @Autowired
    private lateinit var motebehovArbeidstakerController: MotebehovArbeidstakerControllerV4

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

    @MockkBean
    private lateinit var pdlConsumer: PdlConsumer

    @MockkBean(relaxed = true)
    private lateinit var personoppgavehendelseProducer: PersonoppgavehendelseProducer

    private lateinit var mockRestServiceServerAzureAD: MockRestServiceServer
    private lateinit var mockRestServiceServer: MockRestServiceServer

    private val motebehovGenerator = MotebehovGenerator()

    init {
        beforeTest {
            every { pdlConsumer.person(ARBEIDSTAKER_FNR) } returns generatePdlHentPerson(null, null)
            every { pdlConsumer.aktorid(any()) } returns ARBEIDSTAKER_AKTORID
            every { pdlConsumer.fnr(any()) } returns ARBEIDSTAKER_FNR
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
            AzureAdV2TokenConsumer.Companion.clearCache()
            cleanDB()
        }

        describe("Møtebehov arbeidstaker controller V4") {
            it("get MotebehovStatus With No Oppfolgingstilfelle") {
                tokenValidationUtil.logInAsDialogmoteUser(ARBEIDSTAKER_FNR)
                motebehovArbeidstakerController.motebehovStatusArbeidstakerWithCodeSixUsers()
                    .assertMotebehovStatus(expVisMotebehov = false)
            }

            it("get MotebehovStatus With Today Outside OppfolgingstilfelleStart") {
                tokenValidationUtil.logInAsDialogmoteUser(ARBEIDSTAKER_FNR)
                dbCreateOppfolgingstilfelle(
                    oppfolgingstilfelleDAO,
                    generateOppfolgingstilfellePerson(
                        start = LocalDate.now().plusDays(1),
                        end = LocalDate.now().plusDays(10),
                    ),
                )

                motebehovArbeidstakerController.motebehovStatusArbeidstakerWithCodeSixUsers()
                    .assertMotebehovStatus(expVisMotebehov = false)
            }

            it("get MotebehovStatus With Today Outside OppfolgingstilfelleEnd") {
                tokenValidationUtil.logInAsDialogmoteUser(ARBEIDSTAKER_FNR)
                dbCreateOppfolgingstilfelle(
                    oppfolgingstilfelleDAO,
                    generateOppfolgingstilfellePerson(
                        start = LocalDate.now().minusDays(17),
                        end = LocalDate.now().minusDays(16),
                    ),
                )

                motebehovArbeidstakerController.motebehovStatusArbeidstakerWithCodeSixUsers()
                    .assertMotebehovStatus(expVisMotebehov = false)
            }

            it(
                "get MotebehovStatus With Today Inside Oppfolgingstilfelle Merged By Active And" +
                    " Expired Oppfolgingstilfelle No Overlap"
            ) {
                tokenValidationUtil.logInAsDialogmoteUser(ARBEIDSTAKER_FNR)
                val activeOppfolgingstilfelleStartDate = LocalDate.now().minusDays(DAYS_START_SVAR_BEHOV).plusDays(1)

                dbCreateOppfolgingstilfelle(
                    oppfolgingstilfelleDAO,
                    generateOppfolgingstilfellePerson(
                        start = activeOppfolgingstilfelleStartDate.minusDays(2),
                        end = activeOppfolgingstilfelleStartDate.minusDays(1),
                        virksomhetsnummerList = listOf(VIRKSOMHETSNUMMER),
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

                motebehovArbeidstakerController.motebehovStatusArbeidstakerWithCodeSixUsers()
                    .assertMotebehovStatus(expVisMotebehov = true, expSkjemaType = MotebehovSkjemaType.MELD_BEHOV)
            }

            it(
                "getMotebehovStatus With Today Inside Oppfolgingstilfelle Merged By Active And" +
                    " Expired Oppfolgingstilfelle With Overlap"
            ) {
                createKandidatInDB()
                tokenValidationUtil.logInAsDialogmoteUser(ARBEIDSTAKER_FNR)

                val activeOppfolgingstilfelleStartDate = LocalDate.now().minusDays(DAYS_START_SVAR_BEHOV).plusDays(1)

                dbCreateOppfolgingstilfelle(
                    oppfolgingstilfelleDAO,
                    generateOppfolgingstilfellePerson(
                        start = activeOppfolgingstilfelleStartDate.minusDays(2),
                        end = activeOppfolgingstilfelleStartDate,
                        virksomhetsnummerList = listOf(VIRKSOMHETSNUMMER),
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

                motebehovArbeidstakerController.motebehovStatusArbeidstakerWithCodeSixUsers()
                    .assertMotebehovStatus(expVisMotebehov = true, expSkjemaType = MotebehovSkjemaType.SVAR_BEHOV)
            }

            it("get MotebehovStatus With Today Inside Oppfolgingstilfelle Merged By 2 Oppfolgingstilfeller") {
                tokenValidationUtil.logInAsDialogmoteUser(ARBEIDSTAKER_FNR)
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

                motebehovArbeidstakerController.motebehovStatusArbeidstakerWithCodeSixUsers()
                    .assertMotebehovStatus(expVisMotebehov = true, expSkjemaType = MotebehovSkjemaType.MELD_BEHOV)
            }

            it("get MotebehovStatus With Today Inside Oppfolgingstilfelle Day1") {
                tokenValidationUtil.logInAsDialogmoteUser(ARBEIDSTAKER_FNR)
                dbCreateOppfolgingstilfelle(
                    oppfolgingstilfelleDAO,
                    generateOppfolgingstilfellePerson(
                        start = LocalDate.now(),
                        end = LocalDate.now(),
                    ),
                )
                motebehovArbeidstakerController.motebehovStatusArbeidstakerWithCodeSixUsers()
                    .assertMotebehovStatus(expVisMotebehov = true, expSkjemaType = MotebehovSkjemaType.MELD_BEHOV)
            }

            it("get MotebehovStatus With Today Inside Oppfolgingstilfelle LastDay") {
                tokenValidationUtil.logInAsDialogmoteUser(ARBEIDSTAKER_FNR)
                dbCreateOppfolgingstilfelle(
                    oppfolgingstilfelleDAO,
                    generateOppfolgingstilfellePerson(
                        start = LocalDate.now().minusDays(DAYS_END_SVAR_BEHOV).minusDays(1),
                        end = LocalDate.now(),
                    ),
                )

                motebehovArbeidstakerController.motebehovStatusArbeidstakerWithCodeSixUsers()
                    .assertMotebehovStatus(expVisMotebehov = true, expSkjemaType = MotebehovSkjemaType.MELD_BEHOV)
            }

            it("get MotebehovStatus With Today Inside Oppfolgingstilfelle") {
                tokenValidationUtil.logInAsDialogmoteUser(ARBEIDSTAKER_FNR)
                dbCreateOppfolgingstilfelle(
                    oppfolgingstilfelleDAO,
                    generateOppfolgingstilfellePerson(
                        start = LocalDate.now(),
                        end = LocalDate.now().plusDays(1),
                    ),
                )

                motebehovArbeidstakerController.motebehovStatusArbeidstakerWithCodeSixUsers()
                    .assertMotebehovStatus(expVisMotebehov = true, expSkjemaType = MotebehovSkjemaType.MELD_BEHOV)
            }

            it("get MotebehovStatus With Today Inside Oppfolgingstilfelle, MeldBehov Submitted And Behandlet") {
                tokenValidationUtil.logInAsDialogmoteUser(ARBEIDSTAKER_FNR)
                dbCreateOppfolgingstilfelle(
                    oppfolgingstilfelleDAO,
                    generateOppfolgingstilfellePerson(
                        start = LocalDate.now(),
                        end = LocalDate.now().plusDays(1),
                    ),
                )

                val motebehovFormSubmissionDTO = motebehovGenerator.lagFormSubmissionArbeidstakerMeldDTO()

                submitMotebehovAndSendOversikthendelse(motebehovFormSubmissionDTO)

                resetMockRestServers()
                mockBehandlendEnhetWithTilgangskontroll(ARBEIDSTAKER_FNR)
                tokenValidationUtil.logInAsNavCounselor(VEILEDER_ID)
                motebehovVeilederController.behandleMotebehov(ARBEIDSTAKER_FNR)

                resetMockRestServers()
                tokenValidationUtil.logInAsDialogmoteUser(ARBEIDSTAKER_FNR)

                motebehovArbeidstakerController.motebehovStatusArbeidstakerWithCodeSixUsers()
                    .assertMotebehovStatus(expVisMotebehov = true, expSkjemaType = MotebehovSkjemaType.MELD_BEHOV)
            }

            it(
                "get MotebehovStatus With Today Inside Oppfolgingstilfelle MeldBehov," +
                    " Moteplanlegger Active, MeldBehov Submitted"
            ) {
                tokenValidationUtil.logInAsDialogmoteUser(ARBEIDSTAKER_FNR)
                dbCreateOppfolgingstilfelle(
                    oppfolgingstilfelleDAO,
                    generateOppfolgingstilfellePerson(
                        start = LocalDate.now(),
                        end = LocalDate.now().plusDays(1),
                    ),
                )

                val motebehovFormSubmissionDTO = motebehovGenerator.lagFormSubmissionArbeidstakerMeldDTO()

                submitMotebehovAndSendOversikthendelse(motebehovFormSubmissionDTO)

                mockRestServiceServer.reset()

                motebehovArbeidstakerController.motebehovStatusArbeidstakerWithCodeSixUsers()
                    .assertMotebehovStatus(
                        expVisMotebehov = true,
                        expSkjemaType = MotebehovSkjemaType.MELD_BEHOV,
                        expMotebehovFormValues = motebehovFormSubmissionDTO
                            .toMotebehovFormValuesOutputDTO()
                    )
            }

            it("get MotebehovStatus With Today Inside Oppfolgingstilfelle Before SvarBehov StartDate") {
                tokenValidationUtil.logInAsDialogmoteUser(ARBEIDSTAKER_FNR)
                dbCreateOppfolgingstilfelle(
                    oppfolgingstilfelleDAO,
                    generateOppfolgingstilfellePerson(
                        start = LocalDate.now().minusDays(DAYS_START_SVAR_BEHOV).plusDays(1),
                        end = LocalDate.now().plusDays(1),
                    ),
                )

                motebehovArbeidstakerController.motebehovStatusArbeidstakerWithCodeSixUsers()
                    .assertMotebehovStatus(expVisMotebehov = true, expSkjemaType = MotebehovSkjemaType.MELD_BEHOV)
            }

            it("get MotebehovStatus With Today Inside Oppfolgingstilfelle After SvarBehov EndDate") {
                tokenValidationUtil.logInAsDialogmoteUser(ARBEIDSTAKER_FNR)
                dbCreateOppfolgingstilfelle(
                    oppfolgingstilfelleDAO,
                    generateOppfolgingstilfellePerson(
                        start = LocalDate.now().minusDays(DAYS_END_SVAR_BEHOV),
                        end = LocalDate.now().plusDays(1),
                    ),
                )

                motebehovArbeidstakerController.motebehovStatusArbeidstakerWithCodeSixUsers()
                    .assertMotebehovStatus(expVisMotebehov = true, expSkjemaType = MotebehovSkjemaType.MELD_BEHOV)
            }

            it("get MotebehovStatus With No Motebehov And Mote Inside SvarBehov Upper Limit") {
                createKandidatInDB()
                tokenValidationUtil.logInAsDialogmoteUser(ARBEIDSTAKER_FNR)

                dbCreateOppfolgingstilfelle(
                    oppfolgingstilfelleDAO,
                    generateOppfolgingstilfellePerson(
                        start = LocalDate.now().minusDays(DAYS_END_SVAR_BEHOV).plusDays(1),
                        end = LocalDate.now().plusDays(1),
                    ),
                )

                motebehovArbeidstakerController.motebehovStatusArbeidstakerWithCodeSixUsers()
                    .assertMotebehovStatus(expVisMotebehov = true, expSkjemaType = MotebehovSkjemaType.SVAR_BEHOV)
            }

            it("get MotebehovStatus with SvarBehov and Mote created") {
                createKandidatInDB()
                tokenValidationUtil.logInAsDialogmoteUser(ARBEIDSTAKER_FNR)

                dbCreateOppfolgingstilfelle(
                    oppfolgingstilfelleDAO,
                    generateOppfolgingstilfellePerson(
                        start = LocalDate.now().minusDays(DAYS_END_SVAR_BEHOV).plusDays(1),
                        end = LocalDate.now().plusDays(1),
                    ),
                )

                val motebehovFormSubmissionDTO = motebehovGenerator.lagFormSubmissionArbeidstakerSvarJaDTO()

                submitMotebehovAndSendOversikthendelse(motebehovFormSubmissionDTO)
                verify { esyfovarselService.ferdigstillSvarMotebehovForArbeidstaker(ARBEIDSTAKER_FNR) }
                verify(exactly = 0) { esyfovarselService.ferdigstillSvarMotebehovForArbeidsgiver(any(), any(), any()) }

                mockRestServiceServer.reset()

                motebehovArbeidstakerController.motebehovStatusArbeidstakerWithCodeSixUsers()
                    .assertMotebehovStatus(
                        expVisMotebehov = true,
                        expSkjemaType = MotebehovSkjemaType.SVAR_BEHOV,
                        expMotebehovFormValues = motebehovFormSubmissionDTO
                            .toMotebehovFormValuesOutputDTO()
                    )
            }

            it("get MotebehovStatus with no Motebehov and no Mote inside SvarBehov lower limit") {
                createKandidatInDB()
                tokenValidationUtil.logInAsDialogmoteUser(ARBEIDSTAKER_FNR)

                dbCreateOppfolgingstilfelle(
                    oppfolgingstilfelleDAO,
                    generateOppfolgingstilfellePerson(
                        start = LocalDate.now().minusDays(DAYS_START_SVAR_BEHOV),
                        end = LocalDate.now().plusDays(1),
                    ),
                )

                motebehovArbeidstakerController.motebehovStatusArbeidstakerWithCodeSixUsers()
                    .assertMotebehovStatus(expVisMotebehov = true, expSkjemaType = MotebehovSkjemaType.SVAR_BEHOV)
            }

            it("get MotebehovStatus with no Motebehov and no Mote") {
                createKandidatInDB()
                tokenValidationUtil.logInAsDialogmoteUser(ARBEIDSTAKER_FNR)

                dbCreateOppfolgingstilfelle(
                    oppfolgingstilfelleDAO,
                    generateOppfolgingstilfellePerson(),
                )

                motebehovArbeidstakerController.motebehovStatusArbeidstakerWithCodeSixUsers()
                    .assertMotebehovStatus(expVisMotebehov = true, expSkjemaType = MotebehovSkjemaType.SVAR_BEHOV)
            }

            it("get MotebehovStatus and sendOversikthendelse with Motebehov harBehov=true") {
                createKandidatInDB()
                tokenValidationUtil.logInAsDialogmoteUser(ARBEIDSTAKER_FNR)

                dbCreateOppfolgingstilfelle(
                    oppfolgingstilfelleDAO,
                    generateOppfolgingstilfellePerson(virksomhetsnummerList = listOf(VIRKSOMHETSNUMMER)).copy(
                        personIdentNumber = ARBEIDSTAKER_FNR,
                    ),
                )

                lagreOgHentMotebehovOgSendOversikthendelse(harBehov = true)
            }

            it("get MotebehovStatus and SendOversikthendelse with Motebehov harBehov=false") {
                createKandidatInDB()
                tokenValidationUtil.logInAsDialogmoteUser(ARBEIDSTAKER_FNR)

                dbCreateOppfolgingstilfelle(
                    oppfolgingstilfelleDAO,
                    generateOppfolgingstilfellePerson(virksomhetsnummerList = listOf(VIRKSOMHETSNUMMER)).copy(
                        personIdentNumber = ARBEIDSTAKER_FNR,
                    ),
                )

                lagreOgHentMotebehovOgSendOversikthendelse(harBehov = false)
            }

            it("submitMotebehov multiple active Oppfolgingstilfeller") {
                tokenValidationUtil.logInAsDialogmoteUser(ARBEIDSTAKER_FNR)
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

                mockAndExpectBehandlendeEnhetRequest(
                    azureTokenEndpoint,
                    mockRestServiceServerAzureAD,
                    mockRestServiceServer,
                    behandlendeenhetUrl,
                    ARBEIDSTAKER_FNR,
                )
                mockAndExpectBehandlendeEnhetRequest(
                    azureTokenEndpoint,
                    mockRestServiceServerAzureAD,
                    mockRestServiceServer,
                    behandlendeenhetUrl,
                    ARBEIDSTAKER_FNR,
                )

                val formSubmission = motebehovGenerator.lagFormSubmissionArbeidstakerSvarJaDTO()

                motebehovArbeidstakerController.submitMotebehovArbeidstaker(formSubmission)

                val motebehovList = motebehovDAO.hentMotebehovListeForOgOpprettetAvArbeidstaker(ARBEIDSTAKER_AKTORID)

                assertEquals(2, motebehovList.size)
                verify(exactly = 2) { personoppgavehendelseProducer.sendPersonoppgavehendelse(any(), any()) }
            }
        }
    }

    private fun submitMotebehovAndSendOversikthendelse(motebehovFormSubmission: MotebehovFormSubmissionDTO) {
        mockAndExpectBehandlendeEnhetRequest(
            azureTokenEndpoint,
            mockRestServiceServerAzureAD,
            mockRestServiceServer,
            behandlendeenhetUrl,
            ARBEIDSTAKER_FNR,
        )

        motebehovArbeidstakerController.submitMotebehovArbeidstaker(motebehovFormSubmission)
        if (motebehovFormSubmission.harMotebehov) {
            verify { personoppgavehendelseProducer.sendPersonoppgavehendelse(any(), any()) }
        } else {
            verify(exactly = 0) { personoppgavehendelseProducer.sendPersonoppgavehendelse(any(), any()) }
        }
    }

    private fun lagreOgHentMotebehovOgSendOversikthendelse(harBehov: Boolean) {
        mockAndExpectBehandlendeEnhetRequest(
            azureTokenEndpoint,
            mockRestServiceServerAzureAD,
            mockRestServiceServer,
            behandlendeenhetUrl,
            ARBEIDSTAKER_FNR,
        )

        val formSubmission =
            if (harBehov) {
                motebehovGenerator.lagFormSubmissionArbeidstakerSvarJaDTO()
            } else {
                motebehovGenerator.lagFormSubmissionArbeidstakerSvarNeiDTO()
            }

        motebehovArbeidstakerController.submitMotebehovArbeidstaker(formSubmission)

        if (!harBehov) {
            mockRestServiceServer.reset()
        }

        val motebehovStatus = motebehovArbeidstakerController.motebehovStatusArbeidstakerWithCodeSixUsers()

        assertTrue(motebehovStatus.visMotebehov)
        assertEquals(MotebehovSkjemaType.SVAR_BEHOV, motebehovStatus.skjemaType)
        val motebehov = motebehovStatus.motebehov
        assertNotNull(motebehov)
        assertThat(motebehov?.opprettetAv).isEqualTo(ARBEIDSTAKER_AKTORID)
        assertThat(motebehov?.arbeidstakerFnr).isEqualTo(ARBEIDSTAKER_FNR)
        assertThat(motebehov?.virksomhetsnummer).isEqualTo(VIRKSOMHETSNUMMER)
        assertThat(motebehov?.skjemaType).isEqualTo(motebehovStatus.skjemaType)

        assertNotNull(motebehov?.formValues)
        assertThat(motebehov?.formValues?.harMotebehov).isEqualTo(formSubmission.harMotebehov)
        assertThat(motebehov?.formValues?.formSnapshot).isEqualTo(formSubmission.formSnapshot)

        if (harBehov) {
            verify { personoppgavehendelseProducer.sendPersonoppgavehendelse(any(), any()) }
        } else {
            verify(exactly = 0) { personoppgavehendelseProducer.sendPersonoppgavehendelse(any(), any()) }
        }
        verify {
            if (motebehov != null) {
                esyfovarselService.ferdigstillSvarMotebehovForArbeidstaker(motebehov.arbeidstakerFnr)
            }
        }
        verify(exactly = 0) { esyfovarselService.ferdigstillSvarMotebehovForArbeidsgiver(any(), any(), any()) }
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

    private fun createKandidatInDB() {
        dialogmotekandidatDAO.create(
            dialogmotekandidatExternalUUID = UUID.randomUUID().toString(),
            createdAt = LocalDateTime.now().minusDays(DAYS_START_SVAR_BEHOV),
            fnr = ARBEIDSTAKER_FNR,
            kandidat = true,
            arsak = DialogmotekandidatEndringArsak.STOPPUNKT.name,
        )
    }

    private fun resetMockRestServers() {
        mockRestServiceServer.reset()
        mockRestServiceServerAzureAD.reset()
    }

    private fun cleanDB() {
        motebehovDAO.nullstillMotebehov(ARBEIDSTAKER_AKTORID)
        oppfolgingstilfelleDAO.nullstillOppfolgingstilfeller(ARBEIDSTAKER_FNR)
        dialogmotekandidatDAO.delete(ARBEIDSTAKER_FNR)
    }
}
