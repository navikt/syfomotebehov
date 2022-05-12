package no.nav.syfo.motebehov.api

import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.syfo.LocalApplication
import no.nav.syfo.api.auth.OIDCIssuer
import no.nav.syfo.consumer.aktorregister.AktorregisterConsumer
import no.nav.syfo.consumer.aktorregister.domain.Fodselsnummer
import no.nav.syfo.consumer.brukertilgang.BrukertilgangConsumer
import no.nav.syfo.consumer.pdl.PdlConsumer
import no.nav.syfo.consumer.sts.StsConsumer
import no.nav.syfo.motebehov.MotebehovSvar
import no.nav.syfo.motebehov.api.internad.v2.MotebehovVeilederADControllerV2
import no.nav.syfo.motebehov.database.MotebehovDAO
import no.nav.syfo.motebehov.motebehovstatus.*
import no.nav.syfo.oppfolgingstilfelle.database.OppfolgingstilfelleDAO
import no.nav.syfo.oversikthendelse.OversikthendelseProducer
import no.nav.syfo.testhelper.*
import no.nav.syfo.testhelper.OidcTestHelper.loggInnBruker
import no.nav.syfo.testhelper.OidcTestHelper.loggInnVeilederADV2
import no.nav.syfo.testhelper.OidcTestHelper.loggUtAlle
import no.nav.syfo.testhelper.UserConstants.ARBEIDSTAKER_AKTORID
import no.nav.syfo.testhelper.UserConstants.ARBEIDSTAKER_FNR
import no.nav.syfo.testhelper.UserConstants.LEDER_AKTORID
import no.nav.syfo.testhelper.UserConstants.LEDER_FNR
import no.nav.syfo.testhelper.UserConstants.VEILEDER_ID
import no.nav.syfo.testhelper.UserConstants.VIRKSOMHETSNUMMER
import no.nav.syfo.testhelper.UserConstants.VIRKSOMHETSNUMMER_2
import no.nav.syfo.testhelper.assertion.assertMotebehovStatus
import no.nav.syfo.testhelper.generator.*
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.cache.CacheManager
import org.springframework.http.HttpStatus
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.web.client.RestTemplate
import java.time.LocalDate
import java.util.function.Consumer
import javax.inject.Inject

@ExtendWith(SpringExtension::class)
@SpringBootTest(classes = [LocalApplication::class])
@DirtiesContext
class MotebehovArbeidsgiverV2Test {
    @Value("\${azure.openid.config.token.endpoint}")
    private lateinit var azureTokenEndpoint: String

    @Value("\${syfobehandlendeenhet.url}")
    private lateinit var behandlendeenhetUrl: String

    @Value("\${tilgangskontrollapi.url}")
    private lateinit var tilgangskontrollUrl: String

    @Inject
    private lateinit var motebehovArbeidsgiverController: MotebehovArbeidsgiverV2Controller

    @Inject
    private lateinit var motebehovVeilederController: MotebehovVeilederADControllerV2

    @Inject
    private lateinit var contextHolder: TokenValidationContextHolder

    @Inject
    private lateinit var motebehovDAO: MotebehovDAO

    @Inject
    private lateinit var cacheManager: CacheManager

    @Inject
    private lateinit var oppfolgingstilfelleDAO: OppfolgingstilfelleDAO

    @Inject
    @Qualifier("restTemplateWithProxy")
    private lateinit var restTemplateWithProxy: RestTemplate
    private lateinit var mockRestServiceWithProxyServer: MockRestServiceServer

    @Inject
    private lateinit var restTemplate: RestTemplate

    @MockBean
    private lateinit var aktorregisterConsumer: AktorregisterConsumer

    @MockBean
    private lateinit var pdlConsumer: PdlConsumer

    @MockBean
    private lateinit var brukertilgangConsumer: BrukertilgangConsumer

    @MockBean
    private lateinit var stsConsumer: StsConsumer

    @MockBean
    private lateinit var oversikthendelseProducer: OversikthendelseProducer

    private lateinit var mockRestServiceServer: MockRestServiceServer

    private val motebehovGenerator = MotebehovGenerator()

    private val stsToken = generateStsToken().access_token

    @BeforeEach
    fun setUp() {
        `when`(aktorregisterConsumer.getAktorIdForFodselsnummer(Fodselsnummer(ARBEIDSTAKER_FNR))).thenReturn(ARBEIDSTAKER_AKTORID)
        `when`(aktorregisterConsumer.getAktorIdForFodselsnummer(Fodselsnummer(LEDER_FNR))).thenReturn(LEDER_AKTORID)
        `when`(brukertilgangConsumer.hasAccessToAnsatt(ARBEIDSTAKER_FNR)).thenReturn(true)
        `when`(pdlConsumer.person(Fodselsnummer(ARBEIDSTAKER_FNR))).thenReturn(generatePdlHentPerson(null, null))
        `when`(stsConsumer.token()).thenReturn(stsToken)
        mockRestServiceServer = MockRestServiceServer.bindTo(restTemplate).build()
        mockRestServiceWithProxyServer = MockRestServiceServer.bindTo(restTemplateWithProxy).build()
        loggInnBruker(contextHolder, LEDER_FNR)
        cleanDB()
    }

    @AfterEach
    fun tearDown() {
        loggUtAlle(contextHolder)
        mockRestServiceServer.reset()
        mockRestServiceWithProxyServer.reset()
        cacheManager.cacheNames
            .forEach(
                Consumer { cacheName: String ->
                    val cache = cacheManager.getCache(cacheName)
                    cache?.clear()
                }
            )
        cleanDB()
    }

    @Test
    fun getMotebehovStatusWithNoOppfolgingstilfelle() {
        motebehovArbeidsgiverController.motebehovStatusArbeidsgiver(ARBEIDSTAKER_FNR, VIRKSOMHETSNUMMER)
            .assertMotebehovStatus(false, null, null)
    }

    @Test
    fun getMotebehovStatusWithTodayOutsideOppfolgingstilfelleStart() {
        loggUtAlle(contextHolder)
        loggInnBruker(contextHolder, ARBEIDSTAKER_FNR)

        val oppfolgingstilfellePerson = generateOppfolgingstilfellePerson(
            start = LocalDate.now().plusDays(1),
            end = LocalDate.now().plusDays(10)
        )

        dbCreateOppfolgingstilfelle(oppfolgingstilfelleDAO, oppfolgingstilfellePerson)

        motebehovArbeidsgiverController.motebehovStatusArbeidsgiver(ARBEIDSTAKER_FNR, VIRKSOMHETSNUMMER)
            .assertMotebehovStatus(false, null, null)
    }

    @Test
    fun getMotebehovStatusWithTodayOutsideOppfolgingstilfelleEnd() {
        loggUtAlle(contextHolder)
        loggInnBruker(contextHolder, ARBEIDSTAKER_FNR)

        val oppfolgingstilfellePerson = generateOppfolgingstilfellePerson(
            start = LocalDate.now().minusDays(10),
            end = LocalDate.now().minusDays(1)
        )

        dbCreateOppfolgingstilfelle(oppfolgingstilfelleDAO, oppfolgingstilfellePerson)

        motebehovArbeidsgiverController.motebehovStatusArbeidsgiver(ARBEIDSTAKER_FNR, VIRKSOMHETSNUMMER)
            .assertMotebehovStatus(false, null, null)
    }

    @Test
    fun getMotebehovStatusWithTodayInsideOppfolgingstilfelleMergedByActiveAndExpiredOppfolgingstilfelleNoOverlapVirksomhetWithoutActiveOppfolgingstilfelle() {
        val activeOppfolgingstilfelleStartDate = LocalDate.now().minusDays(DAYS_START_SVAR_BEHOV).plusDays(1)
        val oppfolgingstilfellePerson = generateOppfolgingstilfellePerson(
            start = LocalDate.now().minusDays(2),
            end = LocalDate.now().minusDays(1)
        )

        dbCreateOppfolgingstilfelle(oppfolgingstilfelleDAO, oppfolgingstilfellePerson)

        val oppfolgingstilfellePerson2 = generateOppfolgingstilfellePerson(
            virksomhetsnummerList = listOf(VIRKSOMHETSNUMMER_2),
            start = activeOppfolgingstilfelleStartDate,
            end = LocalDate.now().plusDays(1)
        )

        dbCreateOppfolgingstilfelle(oppfolgingstilfelleDAO, oppfolgingstilfellePerson2)

        mockAndExpectMoteadminIsMoteplanleggerActive(mockRestServiceServer, false)

        motebehovArbeidsgiverController.motebehovStatusArbeidsgiver(ARBEIDSTAKER_FNR, VIRKSOMHETSNUMMER)
            .assertMotebehovStatus(false, null, null)
    }

    @Test
    fun getMotebehovStatusWithTodayInsideOppfolgingstilfelleMergedByActiveAndExpiredOppfolgingstilfelleNoOverlap() {
        val activeOppfolgingstilfelleStartDate = LocalDate.now().minusDays(DAYS_START_SVAR_BEHOV).plusDays(1)

        val oppfolgingstilfellePerson = generateOppfolgingstilfellePerson(
            virksomhetsnummerList = listOf(VIRKSOMHETSNUMMER_2),
            start = activeOppfolgingstilfelleStartDate.minusDays(2),
            end = activeOppfolgingstilfelleStartDate.minusDays(1)
        )

        dbCreateOppfolgingstilfelle(oppfolgingstilfelleDAO, oppfolgingstilfellePerson)

        val oppfolgingstilfellePerson2 = generateOppfolgingstilfellePerson(
            virksomhetsnummerList = listOf(VIRKSOMHETSNUMMER),
            start = activeOppfolgingstilfelleStartDate,
            end = LocalDate.now().plusDays(1)
        )

        dbCreateOppfolgingstilfelle(oppfolgingstilfelleDAO, oppfolgingstilfellePerson2)

        mockAndExpectMoteadminIsMoteplanleggerActive(mockRestServiceServer, false)

        motebehovArbeidsgiverController.motebehovStatusArbeidsgiver(ARBEIDSTAKER_FNR, VIRKSOMHETSNUMMER)
            .assertMotebehovStatus(true, MotebehovSkjemaType.MELD_BEHOV, null)
    }

    @Test
    fun getMotebehovStatusWithTodayInsideOppfolgingstilfelleMergedByActiveAndExpiredOppfolgingstilfelleWithOverlap() {
        val activeOppfolgingstilfelleStartDate = LocalDate.now().minusDays(DAYS_START_SVAR_BEHOV).plusDays(1)

        val oppfolgingstilfellePerson = generateOppfolgingstilfellePerson(
            virksomhetsnummerList = listOf(VIRKSOMHETSNUMMER_2),
            start = activeOppfolgingstilfelleStartDate.minusDays(2),
            end = activeOppfolgingstilfelleStartDate
        )

        dbCreateOppfolgingstilfelle(oppfolgingstilfelleDAO, oppfolgingstilfellePerson)

        val oppfolgingstilfellePerson2 = generateOppfolgingstilfellePerson(
            virksomhetsnummerList = listOf(VIRKSOMHETSNUMMER),
            start = activeOppfolgingstilfelleStartDate,
            end = LocalDate.now().plusDays(1)
        )

        dbCreateOppfolgingstilfelle(oppfolgingstilfelleDAO, oppfolgingstilfellePerson2)

        mockAndExpectMoteadminHarAktivtMote(mockRestServiceServer, false)

        motebehovArbeidsgiverController.motebehovStatusArbeidsgiver(ARBEIDSTAKER_FNR, VIRKSOMHETSNUMMER)
            .assertMotebehovStatus(true, MotebehovSkjemaType.SVAR_BEHOV, null)
    }

    @Test
    fun getMotebehovStatusWithTodayInsideOppfolgingstilfelleMergedBy2Oppfolgingstilfeller() {

        val oppfolgingstilfellePerson = generateOppfolgingstilfellePerson(
            virksomhetsnummerList = listOf(VIRKSOMHETSNUMMER),
            start = LocalDate.now().minusDays(DAYS_END_SVAR_BEHOV).minusDays(1),
            end = LocalDate.now()
        )

        dbCreateOppfolgingstilfelle(oppfolgingstilfelleDAO, oppfolgingstilfellePerson)

        val oppfolgingstilfellePerson2 = generateOppfolgingstilfellePerson(
            virksomhetsnummerList = listOf(VIRKSOMHETSNUMMER_2),
            start = LocalDate.now().minusDays(2),
            end = LocalDate.now().plusDays(1)
        )
        dbCreateOppfolgingstilfelle(oppfolgingstilfelleDAO, oppfolgingstilfellePerson2)

        mockAndExpectMoteadminIsMoteplanleggerActive(mockRestServiceServer, false)

        motebehovArbeidsgiverController.motebehovStatusArbeidsgiver(ARBEIDSTAKER_FNR, VIRKSOMHETSNUMMER)
            .assertMotebehovStatus(true, MotebehovSkjemaType.MELD_BEHOV, null)
    }

    @Test
    fun getMotebehovStatusWithTodayInsideOppfolgingstilfelleDay1() {
        val oppfolgingstilfellePerson = generateOppfolgingstilfellePerson(
            start = LocalDate.now(),
            end = LocalDate.now()
        )

        dbCreateOppfolgingstilfelle(oppfolgingstilfelleDAO, oppfolgingstilfellePerson)

        mockAndExpectMoteadminIsMoteplanleggerActive(mockRestServiceServer, false)

        motebehovArbeidsgiverController.motebehovStatusArbeidsgiver(ARBEIDSTAKER_FNR, VIRKSOMHETSNUMMER)
            .assertMotebehovStatus(true, MotebehovSkjemaType.MELD_BEHOV, null)
    }

    @Test
    fun getMotebehovStatusWithTodayInsideOppfolgingstilfelleLastDay() {
        val oppfolgingstilfellePerson = generateOppfolgingstilfellePerson(
            start = LocalDate.now().minusDays(DAYS_END_SVAR_BEHOV).minusDays(1),
            end = LocalDate.now()
        )

        dbCreateOppfolgingstilfelle(oppfolgingstilfelleDAO, oppfolgingstilfellePerson)

        mockAndExpectMoteadminIsMoteplanleggerActive(mockRestServiceServer, false)

        motebehovArbeidsgiverController.motebehovStatusArbeidsgiver(ARBEIDSTAKER_FNR, VIRKSOMHETSNUMMER)
            .assertMotebehovStatus(true, MotebehovSkjemaType.MELD_BEHOV, null)
    }

    @Test
    fun getMotebehovStatusWithTodayInsideOppfolgingstilfelleMedBehovMoteplanleggerActive() {
        val oppfolgingstilfellePerson = generateOppfolgingstilfellePerson(
            start = LocalDate.now(),
            end = LocalDate.now().plusDays(1)
        )

        dbCreateOppfolgingstilfelle(oppfolgingstilfelleDAO, oppfolgingstilfellePerson)

        mockAndExpectMoteadminIsMoteplanleggerActive(mockRestServiceServer, true)

        motebehovArbeidsgiverController.motebehovStatusArbeidsgiver(ARBEIDSTAKER_FNR, VIRKSOMHETSNUMMER)
            .assertMotebehovStatus(false, null, null)
    }

    @Test
    fun getMotebehovStatusWithTodayInsideOppfolgingstilfelleMeldBehovSubmittedAndBehandlet() {
        val oppfolgingstilfellePerson = generateOppfolgingstilfellePerson(
            start = LocalDate.now(),
            end = LocalDate.now().plusDays(1)
        )

        dbCreateOppfolgingstilfelle(oppfolgingstilfelleDAO, oppfolgingstilfellePerson)

        mockAndExpectMoteadminIsMoteplanleggerActive(mockRestServiceServer, false)
        val motebehovSvar = motebehovGenerator.lagMotebehovSvar(true)
        submitMotebehovAndSendOversikthendelse(motebehovSvar)

        mockRestServiceServer.reset()
        loggUtAlle(contextHolder)
        loggInnVeilederADV2(contextHolder, VEILEDER_ID)
        mockAndExpectSyfoTilgangskontroll(
            mockRestServiceServer,
            tilgangskontrollUrl,
            contextHolder.tokenValidationContext.getJwtToken(OIDCIssuer.INTERN_AZUREAD_V2).tokenAsString,
            ARBEIDSTAKER_FNR,
            HttpStatus.OK
        )
        motebehovVeilederController.behandleMotebehov(ARBEIDSTAKER_FNR)
        loggInnBruker(contextHolder, LEDER_FNR)

        mockRestServiceServer.reset()

        mockAndExpectMoteadminIsMoteplanleggerActive(mockRestServiceServer, false)

        motebehovArbeidsgiverController.motebehovStatusArbeidsgiver(ARBEIDSTAKER_FNR, VIRKSOMHETSNUMMER)
            .assertMotebehovStatus(true, MotebehovSkjemaType.MELD_BEHOV, null)
    }

    @Test
    fun getMotebehovStatusWithTodayInsideOppfolgingstilfelleMeldBehovMoteplanleggerActiveMeldBehovSubmitted() {
        val oppfolgingstilfellePerson = generateOppfolgingstilfellePerson(
            start = LocalDate.now(),
            end = LocalDate.now().plusDays(1)
        )

        dbCreateOppfolgingstilfelle(oppfolgingstilfelleDAO, oppfolgingstilfellePerson)

        mockAndExpectMoteadminIsMoteplanleggerActive(mockRestServiceServer, false)
        val motebehovSvar = motebehovGenerator.lagMotebehovSvar(true)
        submitMotebehovAndSendOversikthendelse(motebehovSvar)

        mockRestServiceServer.reset()
        mockAndExpectMoteadminIsMoteplanleggerActive(mockRestServiceServer, true)

        motebehovArbeidsgiverController.motebehovStatusArbeidsgiver(ARBEIDSTAKER_FNR, VIRKSOMHETSNUMMER)
            .assertMotebehovStatus(true, MotebehovSkjemaType.MELD_BEHOV, motebehovSvar)
    }

    @Test
    fun getMotebehovStatusWithTodayInsideOppfolgingstilfelleBeforeSvarBehovStartDate() {
        val oppfolgingstilfellePerson = generateOppfolgingstilfellePerson(
            start = LocalDate.now().minusDays(DAYS_START_SVAR_BEHOV).plusDays(1),
            end = LocalDate.now().plusDays(1)
        )

        dbCreateOppfolgingstilfelle(oppfolgingstilfelleDAO, oppfolgingstilfellePerson)

        mockAndExpectMoteadminIsMoteplanleggerActive(mockRestServiceServer, false)

        motebehovArbeidsgiverController.motebehovStatusArbeidsgiver(ARBEIDSTAKER_FNR, VIRKSOMHETSNUMMER)
            .assertMotebehovStatus(true, MotebehovSkjemaType.MELD_BEHOV, null)
    }

    @Test
    fun getMotebehovStatusWithTodayInsideOppfolgingstilfelleAfterSvarBehovEndDate() {
        val oppfolgingstilfellePerson = generateOppfolgingstilfellePerson(
            start = LocalDate.now().minusDays(DAYS_END_SVAR_BEHOV),
            end = LocalDate.now().plusDays(1)
        )

        dbCreateOppfolgingstilfelle(oppfolgingstilfelleDAO, oppfolgingstilfellePerson)

        mockAndExpectMoteadminIsMoteplanleggerActive(mockRestServiceServer, false)

        motebehovArbeidsgiverController.motebehovStatusArbeidsgiver(ARBEIDSTAKER_FNR, VIRKSOMHETSNUMMER)
            .assertMotebehovStatus(true, MotebehovSkjemaType.MELD_BEHOV, null)
    }

    @Test
    fun getMotebehovStatusWithNoMotebehovAndMoteInsideSvarBehovUpperLimit() {
        val oppfolgingstilfellePerson = generateOppfolgingstilfellePerson(
            start = LocalDate.now().minusDays(DAYS_END_SVAR_BEHOV).plusDays(1),
            end = LocalDate.now().plusDays(1)
        )

        mockAndExpectMoteadminHarAktivtMote(mockRestServiceServer, true)

        dbCreateOppfolgingstilfelle(oppfolgingstilfelleDAO, oppfolgingstilfellePerson)

        motebehovArbeidsgiverController.motebehovStatusArbeidsgiver(ARBEIDSTAKER_FNR, VIRKSOMHETSNUMMER)
            .assertMotebehovStatus(false, null, null)
    }

    @Test
    fun getMotebehovStatusWithSvarBehovAndMoteCreated() {
        val oppfolgingstilfellePerson = generateOppfolgingstilfellePerson(
            start = LocalDate.now().minusDays(DAYS_END_SVAR_BEHOV).plusDays(1),
            end = LocalDate.now().plusDays(1)
        )

        dbCreateOppfolgingstilfelle(oppfolgingstilfelleDAO, oppfolgingstilfellePerson)

        mockAndExpectMoteadminHarAktivtMote(mockRestServiceServer, false)
        val motebehovSvar = motebehovGenerator.lagMotebehovSvar(true)
        submitMotebehovAndSendOversikthendelse(motebehovSvar)

        mockRestServiceServer.reset()
        mockAndExpectMoteadminHarAktivtMote(mockRestServiceServer, true)

        motebehovArbeidsgiverController.motebehovStatusArbeidsgiver(ARBEIDSTAKER_FNR, VIRKSOMHETSNUMMER)
            .assertMotebehovStatus(true, MotebehovSkjemaType.SVAR_BEHOV, motebehovSvar)
    }

    @Test
    fun getMotebehovStatusWithNoMotebehovAndNoMoteInsideSvarBehovLowerLimit() {
        val oppfolgingstilfellePerson = generateOppfolgingstilfellePerson(
            start = LocalDate.now().minusDays(DAYS_START_SVAR_BEHOV),
            end = LocalDate.now().plusDays(1)
        )
        mockAndExpectMoteadminHarAktivtMote(mockRestServiceServer, false)

        dbCreateOppfolgingstilfelle(oppfolgingstilfelleDAO, oppfolgingstilfellePerson)

        motebehovArbeidsgiverController.motebehovStatusArbeidsgiver(ARBEIDSTAKER_FNR, VIRKSOMHETSNUMMER)
            .assertMotebehovStatus(true, MotebehovSkjemaType.SVAR_BEHOV, null)
    }

    @Test
    fun getMotebehovStatusWithNoMotebehovAndNoMote() {
        val oppfolgingstilfellePerson = generateOppfolgingstilfellePerson()

        dbCreateOppfolgingstilfelle(oppfolgingstilfelleDAO, oppfolgingstilfellePerson)

        mockAndExpectMoteadminHarAktivtMote(mockRestServiceServer, false)

        motebehovArbeidsgiverController.motebehovStatusArbeidsgiver(ARBEIDSTAKER_FNR, VIRKSOMHETSNUMMER)
            .assertMotebehovStatus(true, MotebehovSkjemaType.SVAR_BEHOV, null)
    }

    @Test
    fun getMotebehovStatusWithNoMotebehovAndMote() {
        val oppfolgingstilfellePerson = generateOppfolgingstilfellePerson()

        dbCreateOppfolgingstilfelle(oppfolgingstilfelleDAO, oppfolgingstilfellePerson)

        mockAndExpectMoteadminHarAktivtMote(mockRestServiceServer, true)

        motebehovArbeidsgiverController.motebehovStatusArbeidsgiver(ARBEIDSTAKER_FNR, VIRKSOMHETSNUMMER)
            .assertMotebehovStatus(false, null, null)
    }

    @Test
    fun getMotebehovStatusAndSendOversikthendelseWithMotebehovHarBehovTrue() {
        val oppfolgingstilfellePerson = generateOppfolgingstilfellePerson()

        dbCreateOppfolgingstilfelle(oppfolgingstilfelleDAO, oppfolgingstilfellePerson)

        lagreOgHentMotebehovOgSendOversikthendelse(harBehov = true)
    }

    @Test
    fun getMotebehovStatusAndSendOversikthendelseWithMotebehovHarBehovFalse() {
        val oppfolgingstilfellePerson = generateOppfolgingstilfellePerson()

        dbCreateOppfolgingstilfelle(oppfolgingstilfelleDAO, oppfolgingstilfellePerson)

        lagreOgHentMotebehovOgSendOversikthendelse(false)
    }

    private fun submitMotebehovAndSendOversikthendelse(motebehovSvar: MotebehovSvar) {
        mockAndExpectBehandlendeEnhetRequest(
            azureTokenEndpoint,
            mockRestServiceWithProxyServer,
            mockRestServiceServer,
            behandlendeenhetUrl,
            ARBEIDSTAKER_FNR
        )

        motebehovArbeidsgiverController.lagreMotebehovArbeidsgiver(
            motebehovGenerator.lagNyttMotebehovArbeidsgiver().copy(
                motebehovSvar = motebehovSvar
            )
        )
        if (motebehovSvar.harMotebehov) {
            Mockito.verify(oversikthendelseProducer).sendOversikthendelse(any(), any())
        } else {
            Mockito.verify(oversikthendelseProducer, Mockito.never()).sendOversikthendelse(any(), any())
        }
    }

    private fun lagreOgHentMotebehovOgSendOversikthendelse(harBehov: Boolean) {
        mockAndExpectMoteadminHarAktivtMote(mockRestServiceServer, false)
        mockAndExpectBehandlendeEnhetRequest(
            azureTokenEndpoint,
            mockRestServiceWithProxyServer,
            mockRestServiceServer,
            behandlendeenhetUrl,
            ARBEIDSTAKER_FNR
        )

        val motebehovSvar = motebehovGenerator.lagMotebehovSvar(harBehov)

        motebehovArbeidsgiverController.lagreMotebehovArbeidsgiver(
            motebehovGenerator.lagNyttMotebehovArbeidsgiver().copy(
                motebehovSvar = motebehovSvar
            )
        )
        if (!harBehov) {
            mockRestServiceServer.reset()
            mockAndExpectMoteadminHarAktivtMote(mockRestServiceServer, false)
        }

        val motebehovStatus: MotebehovStatus = motebehovArbeidsgiverController.motebehovStatusArbeidsgiver(
            ARBEIDSTAKER_FNR,
            VIRKSOMHETSNUMMER
        )

        assertTrue(motebehovStatus.visMotebehov)
        assertEquals(MotebehovSkjemaType.SVAR_BEHOV, motebehovStatus.skjemaType)
        val motebehov = motebehovStatus.motebehov!!
        assertNotNull(motebehov)
        Assertions.assertThat(motebehov.opprettetAv).isEqualTo(LEDER_AKTORID)
        Assertions.assertThat(motebehov.arbeidstakerFnr).isEqualTo(ARBEIDSTAKER_FNR)
        Assertions.assertThat(motebehov.virksomhetsnummer).isEqualTo(VIRKSOMHETSNUMMER)
        Assertions.assertThat(motebehov.skjemaType).isEqualTo(motebehovStatus.skjemaType)
        Assertions.assertThat(motebehov.motebehovSvar).isEqualToComparingFieldByField(motebehovSvar)
        if (harBehov) {
            Mockito.verify(oversikthendelseProducer).sendOversikthendelse(any(), any())
        } else {
            Mockito.verify(oversikthendelseProducer, Mockito.never()).sendOversikthendelse(any(), any())
        }
    }

    private fun cleanDB() {
        motebehovDAO.nullstillMotebehov(ARBEIDSTAKER_AKTORID)
        oppfolgingstilfelleDAO.nullstillOppfolgingstilfeller(Fodselsnummer(ARBEIDSTAKER_FNR))
    }
}

private fun <T> any(): T {
    Mockito.any<T>()
    return uninitialized()
}

private fun <T> uninitialized(): T = null as T
