package no.nav.syfo.motebehov.api

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import no.nav.syfo.LocalApplication
import no.nav.syfo.consumer.azuread.v2.AzureAdV2TokenConsumer
import no.nav.syfo.consumer.brukertilgang.BrukertilgangConsumer
import no.nav.syfo.consumer.pdl.PdlConsumer
import no.nav.syfo.dialogmotekandidat.database.DialogmotekandidatDAO
import no.nav.syfo.dialogmotekandidat.database.DialogmotekandidatEndringArsak
import no.nav.syfo.motebehov.MotebehovSvar
import no.nav.syfo.motebehov.NyttMotebehovArbeidsgiver
import no.nav.syfo.motebehov.api.internad.v2.MotebehovVeilederADControllerV2
import no.nav.syfo.motebehov.database.MotebehovDAO
import no.nav.syfo.motebehov.motebehovstatus.DAYS_END_SVAR_BEHOV
import no.nav.syfo.motebehov.motebehovstatus.DAYS_START_SVAR_BEHOV
import no.nav.syfo.motebehov.motebehovstatus.MotebehovSkjemaType
import no.nav.syfo.motebehov.motebehovstatus.MotebehovStatus
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
import no.nav.syfo.testhelper.generator.*
import no.nav.syfo.testhelper.mockAndExpectBehandlendeEnhetRequest
import no.nav.syfo.testhelper.mockAndExpectBehandlendeEnhetRequestWithTilgangskontroll
import no.nav.syfo.util.TokenValidationUtil
import no.nav.syfo.varsel.esyfovarsel.EsyfovarselService
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cache.CacheManager
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.web.client.RestTemplate
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import java.util.function.Consumer

@ExtendWith(SpringExtension::class)
@SpringBootTest(classes = [LocalApplication::class])
@DirtiesContext
class MotebehovArbeidsgiverControllerV3Test {
    @Value("\${azure.openid.config.token.endpoint}")
    private lateinit var azureTokenEndpoint: String

    @Value("\${syfobehandlendeenhet.url}")
    private lateinit var behandlendeenhetUrl: String

    @Value("\${istilgangskontroll.url}")
    private lateinit var tilgangskontrollUrl: String

    @Autowired
    private lateinit var motebehovArbeidsgiverController: MotebehovArbeidsgiverControllerV3

    @Autowired
    private lateinit var motebehovVeilederController: MotebehovVeilederADControllerV2

    @Autowired
    private lateinit var motebehovDAO: MotebehovDAO

    @Autowired
    private lateinit var cacheManager: CacheManager

    @Autowired
    private lateinit var oppfolgingstilfelleDAO: OppfolgingstilfelleDAO

    @Autowired
    private lateinit var dialogmotekandidatDAO: DialogmotekandidatDAO

    @Autowired
    @Qualifier("restTemplateWithProxy")
    private lateinit var restTemplateWithProxy: RestTemplate
    private lateinit var mockRestServiceWithProxyServer: MockRestServiceServer

    @Autowired
    private lateinit var restTemplate: RestTemplate

    @Autowired
    private lateinit var tokenValidationUtil: TokenValidationUtil

    @MockkBean(relaxed = true)
    private lateinit var esyfovarselService: EsyfovarselService

    @MockkBean
    private lateinit var pdlConsumer: PdlConsumer

    @MockkBean
    private lateinit var brukertilgangConsumer: BrukertilgangConsumer

    @MockkBean(relaxed = true)
    private lateinit var personoppgavehendelseProducer: PersonoppgavehendelseProducer

    private lateinit var mockRestServiceServer: MockRestServiceServer

    private val motebehovGenerator = MotebehovGenerator()

    @BeforeEach
    fun setUp() {
        every { brukertilgangConsumer.hasAccessToAnsatt(ARBEIDSTAKER_FNR) } returns true
        every { pdlConsumer.person(ARBEIDSTAKER_FNR) } returns generatePdlHentPerson(null, null)
        every { pdlConsumer.aktorid(ARBEIDSTAKER_FNR) } returns ARBEIDSTAKER_AKTORID
        every { pdlConsumer.aktorid(LEDER_FNR) } returns LEDER_AKTORID

        every { pdlConsumer.isKode6(ARBEIDSTAKER_FNR) } returns false

        mockRestServiceServer = MockRestServiceServer.bindTo(restTemplate).build()
        mockRestServiceWithProxyServer = MockRestServiceServer.bindTo(restTemplateWithProxy).build()
        tokenValidationUtil.logInAsDialogmoteUser(LEDER_FNR)
        cleanDB()
    }

    @AfterEach
    fun tearDown() {
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

    @Test
    fun getMotebehovStatusWithNoOppfolgingstilfelle() {
        motebehovArbeidsgiverController.motebehovStatusArbeidsgiver(ARBEIDSTAKER_FNR, VIRKSOMHETSNUMMER)
            .assertMotebehovStatus(false, null, null)
    }

    @Test
    fun getMotebehovStatusWithTodayOutsideOppfolgingstilfelleStart() {
        dbCreateOppfolgingstilfelle(
            oppfolgingstilfelleDAO,
            generateOppfolgingstilfellePerson(
                start = LocalDate.now().plusDays(1),
                end = LocalDate.now().plusDays(10),
            ),
        )

        motebehovArbeidsgiverController.motebehovStatusArbeidsgiver(ARBEIDSTAKER_FNR, VIRKSOMHETSNUMMER)
            .assertMotebehovStatus(false, null, null)
    }

    @Test
    fun getMotebehovStatusWithTodayOutsideOppfolgingstilfelleEnd() {
        dbCreateOppfolgingstilfelle(
            oppfolgingstilfelleDAO,
            generateOppfolgingstilfellePerson(
                start = LocalDate.now().minusDays(17),
                end = LocalDate.now().minusDays(16),
            ),
        )

        motebehovArbeidsgiverController.motebehovStatusArbeidsgiver(ARBEIDSTAKER_FNR, VIRKSOMHETSNUMMER)
            .assertMotebehovStatus(false, null, null)
    }

    @Test
    fun getMotebehovStatusWithTodayInsideOppfolgingstilfelleMergedByActiveAndExpiredOppfolgingstilfelleNoOverlapVirksomhetWithoutActiveOppfolgingstilfelle() {
        val activeOppfolgingstilfelleStartDate = LocalDate.now().minusDays(DAYS_START_SVAR_BEHOV).plusDays(1)
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
            .assertMotebehovStatus(false, null, null)
    }

    @Test
    fun getMotebehovStatusWithTodayInsideOppfolgingstilfelleMergedByActiveAndExpiredOppfolgingstilfelleNoOverlap() {
        val activeOppfolgingstilfelleStartDate = LocalDate.now().minusDays(DAYS_START_SVAR_BEHOV).plusDays(1)

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
            .assertMotebehovStatus(true, MotebehovSkjemaType.MELD_BEHOV, null)
    }

    @Test
    fun getMotebehovStatusWithTodayInsideOppfolgingstilfelleMergedByActiveAndExpiredOppfolgingstilfelleWithOverlap() {
        val activeOppfolgingstilfelleStartDate = LocalDate.now().minusDays(DAYS_START_SVAR_BEHOV).plusDays(1)

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
            .assertMotebehovStatus(true, MotebehovSkjemaType.SVAR_BEHOV, null)
    }

    @Test
    fun getMotebehovStatusWithTodayInsideOppfolgingstilfelleMergedBy2Oppfolgingstilfeller() {
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
            .assertMotebehovStatus(true, MotebehovSkjemaType.MELD_BEHOV, null)
    }

    @Test
    fun getMotebehovStatusWithTodayInsideOppfolgingstilfelleDay1() {
        dbCreateOppfolgingstilfelle(
            oppfolgingstilfelleDAO,
            generateOppfolgingstilfellePerson(
                start = LocalDate.now(),
                end = LocalDate.now(),
            ),
        )

        motebehovArbeidsgiverController.motebehovStatusArbeidsgiver(ARBEIDSTAKER_FNR, VIRKSOMHETSNUMMER)
            .assertMotebehovStatus(true, MotebehovSkjemaType.MELD_BEHOV, null)
    }

    @Test
    fun getMotebehovStatusWithTodayInsideOppfolgingstilfelleLastDay() {
        dbCreateOppfolgingstilfelle(
            oppfolgingstilfelleDAO,
            generateOppfolgingstilfellePerson(
                start = LocalDate.now().minusDays(DAYS_END_SVAR_BEHOV).minusDays(1),
                end = LocalDate.now(),
            ),
        )

        motebehovArbeidsgiverController.motebehovStatusArbeidsgiver(ARBEIDSTAKER_FNR, VIRKSOMHETSNUMMER)
            .assertMotebehovStatus(true, MotebehovSkjemaType.MELD_BEHOV, null)
    }

    @Test
    fun getMotebehovStatusWithTodayInsideOppfolgingstilfelleMedBehov() {
        dbCreateOppfolgingstilfelle(
            oppfolgingstilfelleDAO,
            generateOppfolgingstilfellePerson(
                start = LocalDate.now(),
                end = LocalDate.now().plusDays(1),
            ),
        )

        motebehovArbeidsgiverController.motebehovStatusArbeidsgiver(ARBEIDSTAKER_FNR, VIRKSOMHETSNUMMER)
            .assertMotebehovStatus(true, MotebehovSkjemaType.MELD_BEHOV, null)
    }

    @Test
    fun getMotebehovStatusWithTodayInsideOppfolgingstilfelleMeldBehovSubmittedAndBehandlet() {
        dbCreateOppfolgingstilfelle(
            oppfolgingstilfelleDAO,
            generateOppfolgingstilfellePerson(
                start = LocalDate.now(),
                end = LocalDate.now().plusDays(1),
            ),
        )

        val motebehovSvar = motebehovGenerator.lagMotebehovSvar(true)
        submitMotebehovAndSendOversikthendelse(motebehovSvar)

        resetMockRestServers()

        mockBehandlendEnhetWithTilgangskontroll(ARBEIDSTAKER_FNR)
        tokenValidationUtil.logInAsNavCounselor(VEILEDER_ID)
        motebehovVeilederController.behandleMotebehov(ARBEIDSTAKER_FNR)

        resetMockRestServers()
        tokenValidationUtil.logInAsDialogmoteUser(LEDER_FNR)
        motebehovArbeidsgiverController.motebehovStatusArbeidsgiver(ARBEIDSTAKER_FNR, VIRKSOMHETSNUMMER)
            .assertMotebehovStatus(true, MotebehovSkjemaType.MELD_BEHOV, null)
    }

    @Test
    fun getMotebehovStatusWithTodayInsideOppfolgingstilfelleMeldBehovMoteplanleggerActiveMeldBehovSubmitted() {
        dbCreateOppfolgingstilfelle(
            oppfolgingstilfelleDAO,
            generateOppfolgingstilfellePerson(
                start = LocalDate.now(),
                end = LocalDate.now().plusDays(1),
            ),
        )

        val motebehovSvar = motebehovGenerator.lagMotebehovSvar(true)
        submitMotebehovAndSendOversikthendelse(motebehovSvar)

        mockRestServiceServer.reset()

        motebehovArbeidsgiverController.motebehovStatusArbeidsgiver(ARBEIDSTAKER_FNR, VIRKSOMHETSNUMMER)
            .assertMotebehovStatus(true, MotebehovSkjemaType.MELD_BEHOV, motebehovSvar)
    }

    @Test
    fun getMotebehovStatusWithTodayInsideOppfolgingstilfelleBeforeSvarBehovStartDate() {
        dbCreateOppfolgingstilfelle(
            oppfolgingstilfelleDAO,
            generateOppfolgingstilfellePerson(
                start = LocalDate.now().minusDays(DAYS_START_SVAR_BEHOV).plusDays(1),
                end = LocalDate.now().plusDays(1),
            ),
        )

        motebehovArbeidsgiverController.motebehovStatusArbeidsgiver(ARBEIDSTAKER_FNR, VIRKSOMHETSNUMMER)
            .assertMotebehovStatus(true, MotebehovSkjemaType.MELD_BEHOV, null)
    }

    @Test
    fun getMotebehovStatusWithTodayInsideOppfolgingstilfelleAfterSvarBehovEndDate() {
        dbCreateOppfolgingstilfelle(
            oppfolgingstilfelleDAO,
            generateOppfolgingstilfellePerson(
                start = LocalDate.now().minusDays(DAYS_END_SVAR_BEHOV),
                end = LocalDate.now().plusDays(1),
            ),
        )

        motebehovArbeidsgiverController.motebehovStatusArbeidsgiver(ARBEIDSTAKER_FNR, VIRKSOMHETSNUMMER)
            .assertMotebehovStatus(true, MotebehovSkjemaType.MELD_BEHOV, null)
    }

    @Test
    fun getMotebehovStatusWithNoMotebehovInsideSvarBehovUpperLimit() {
        createKandidatInDB(ARBEIDSTAKER_FNR)

        dbCreateOppfolgingstilfelle(
            oppfolgingstilfelleDAO,
            generateOppfolgingstilfellePerson(
                start = LocalDate.now().minusDays(DAYS_END_SVAR_BEHOV).plusDays(1),
                end = LocalDate.now().plusDays(1),
            ),
        )

        motebehovArbeidsgiverController.motebehovStatusArbeidsgiver(ARBEIDSTAKER_FNR, VIRKSOMHETSNUMMER)
            .assertMotebehovStatus(true, MotebehovSkjemaType.SVAR_BEHOV, null)
    }

    @Test
    fun getMotebehovStatusWithSvarBehovAndMoteCreated() {
        createKandidatInDB(ARBEIDSTAKER_FNR)

        dbCreateOppfolgingstilfelle(
            oppfolgingstilfelleDAO,
            generateOppfolgingstilfellePerson(
                start = LocalDate.now().minusDays(DAYS_END_SVAR_BEHOV).plusDays(1),
                end = LocalDate.now().plusDays(1),
            ),
        )

        val motebehovSvar = motebehovGenerator.lagMotebehovSvar(true)
        submitMotebehovAndSendOversikthendelse(motebehovSvar)
        verify { esyfovarselService.ferdigstillSvarMotebehovForArbeidsgiver(any(), ARBEIDSTAKER_FNR, any()) }
        verify(exactly = 0) { esyfovarselService.ferdigstillSvarMotebehovForArbeidstaker(ARBEIDSTAKER_FNR) }

        mockRestServiceServer.reset()

        motebehovArbeidsgiverController.motebehovStatusArbeidsgiver(ARBEIDSTAKER_FNR, VIRKSOMHETSNUMMER)
            .assertMotebehovStatus(true, MotebehovSkjemaType.SVAR_BEHOV, motebehovSvar)
    }

    @Test
    fun getMotebehovStatusWithNoMotebehovAndNoMoteInsideSvarBehovLowerLimit() {
        createKandidatInDB(ARBEIDSTAKER_FNR)

        dbCreateOppfolgingstilfelle(
            oppfolgingstilfelleDAO,
            generateOppfolgingstilfellePerson(
                start = LocalDate.now().minusDays(DAYS_START_SVAR_BEHOV),
                end = LocalDate.now().plusDays(1),
            ),
        )

        motebehovArbeidsgiverController.motebehovStatusArbeidsgiver(ARBEIDSTAKER_FNR, VIRKSOMHETSNUMMER)
            .assertMotebehovStatus(true, MotebehovSkjemaType.SVAR_BEHOV, null)
    }

    @Test
    fun getMotebehovStatusWithNoMotebehovAndNoMote() {
        dbCreateOppfolgingstilfelle(
            oppfolgingstilfelleDAO,
            generateOppfolgingstilfellePerson(),
        )

        createKandidatInDB(ARBEIDSTAKER_FNR)

        motebehovArbeidsgiverController.motebehovStatusArbeidsgiver(ARBEIDSTAKER_FNR, VIRKSOMHETSNUMMER)
            .assertMotebehovStatus(true, MotebehovSkjemaType.SVAR_BEHOV, null)
    }

    @Test
    fun getMotebehovStatusAndSendOversikthendelseWithMotebehovHarBehovTrue() {
        dbCreateOppfolgingstilfelle(
            oppfolgingstilfelleDAO,
            generateOppfolgingstilfellePerson(),
        )
        val motebehov = motebehovGenerator.lagNyttMotebehovArbeidsgiver().copy(
            motebehovSvar = MotebehovSvar(harMotebehov = true, forklaring = ""),
        )

        lagreMotebehov(motebehov)
        verifyMotebehovStatus(motebehov.motebehovSvar)
    }

    @Test
    fun getMotebehovStatusAndSendOversikthendelseWithMotebehovHarBehovFalse() {
        dbCreateOppfolgingstilfelle(
            oppfolgingstilfelleDAO,
            generateOppfolgingstilfellePerson(),
        )

        val motebehov = motebehovGenerator.lagNyttMotebehovArbeidsgiver().copy(
            motebehovSvar = MotebehovSvar(harMotebehov = false, forklaring = ""),
        )

        lagreMotebehov(motebehov)
        verifyMotebehovStatus(motebehov.motebehovSvar)
    }

    @Test
    fun innsendtMotebehovForEgenLederFerdigstillerOgsaaSykmeldtVarsel() {
        dbCreateOppfolgingstilfelle(
            oppfolgingstilfelleDAO,
            generateOppfolgingstilfellePerson(virksomhetsnummerList = listOf(VIRKSOMHETSNUMMER)).copy(
                personIdentNumber = LEDER_FNR,
            ),
        )
        val motebehov = motebehovGenerator.lagNyttMotebehovArbeidsgiver().copy(
            motebehovSvar = MotebehovSvar(harMotebehov = true, forklaring = ""),
            arbeidstakerFnr = LEDER_FNR
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

    private fun submitMotebehovAndSendOversikthendelse(motebehovSvar: MotebehovSvar) {
        mockAndExpectBehandlendeEnhetRequest(
            azureTokenEndpoint,
            mockRestServiceWithProxyServer,
            behandlendeenhetUrl,
            ARBEIDSTAKER_FNR,
        )

        motebehovArbeidsgiverController.lagreMotebehovArbeidsgiver(
            motebehovGenerator.lagNyttMotebehovArbeidsgiver().copy(
                motebehovSvar = motebehovSvar,
            ),
        )
        if (motebehovSvar.harMotebehov) {
            verify { personoppgavehendelseProducer.sendPersonoppgavehendelse(any(), any()) }
        } else {
            verify(exactly = 0) { personoppgavehendelseProducer.sendPersonoppgavehendelse(any(), any()) }
        }
    }

    private fun lagreMotebehov(innsendtMotebehov: NyttMotebehovArbeidsgiver) {
        mockAndExpectBehandlendeEnhetRequest(
            azureTokenEndpoint,
            mockRestServiceWithProxyServer,
            behandlendeenhetUrl,
            innsendtMotebehov.arbeidstakerFnr,
        )

        createKandidatInDB(innsendtMotebehov.arbeidstakerFnr)

        motebehovArbeidsgiverController.lagreMotebehovArbeidsgiver(
            innsendtMotebehov
        )

        if (!innsendtMotebehov.motebehovSvar.harMotebehov) {
            mockRestServiceServer.reset()
        }
    }

    private fun verifyMotebehovStatus(innsendtMotebehovSvar: MotebehovSvar) {
        val motebehovStatus: MotebehovStatus = motebehovArbeidsgiverController.motebehovStatusArbeidsgiver(
            ARBEIDSTAKER_FNR,
            VIRKSOMHETSNUMMER,
        )

        assertTrue(motebehovStatus.visMotebehov)
        assertEquals(MotebehovSkjemaType.SVAR_BEHOV, motebehovStatus.skjemaType)
        val motebehov = motebehovStatus.motebehov!!
        assertNotNull(motebehov)
        assertThat(motebehov.opprettetAv).isEqualTo(LEDER_AKTORID)
        assertThat(motebehov.arbeidstakerFnr).isEqualTo(ARBEIDSTAKER_FNR)
        assertThat(motebehov.virksomhetsnummer).isEqualTo(VIRKSOMHETSNUMMER)
        assertThat(motebehov.skjemaType).isEqualTo(motebehovStatus.skjemaType)
        assertThat(motebehov.motebehovSvar).usingRecursiveComparison().isEqualTo(innsendtMotebehovSvar)
        if (innsendtMotebehovSvar.harMotebehov) {
            verify { personoppgavehendelseProducer.sendPersonoppgavehendelse(any(), any()) }
        } else {
            verify(exactly = 0) { personoppgavehendelseProducer.sendPersonoppgavehendelse(any(), any()) }
        }
        verify { esyfovarselService.ferdigstillSvarMotebehovForArbeidsgiver(any(), any(), any()) }
        verify(exactly = 0) { esyfovarselService.ferdigstillSvarMotebehovForArbeidstaker(motebehov.arbeidstakerFnr) }
    }

    private fun resetMockRestServers() {
        mockRestServiceServer.reset()
        mockRestServiceWithProxyServer.reset()
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
            mockRestServiceWithProxyServer,
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
