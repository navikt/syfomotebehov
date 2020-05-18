package no.nav.syfo.motebehov.api

import no.nav.security.oidc.context.OIDCRequestContextHolder
import no.nav.syfo.LocalApplication
import no.nav.syfo.api.auth.OIDCIssuer
import no.nav.syfo.consumer.aktorregister.AktorregisterConsumer
import no.nav.syfo.consumer.aktorregister.domain.Fodselsnummer
import no.nav.syfo.consumer.pdl.PdlConsumer
import no.nav.syfo.consumer.sts.StsConsumer
import no.nav.syfo.motebehov.MotebehovSvar
import no.nav.syfo.motebehov.database.MotebehovDAO
import no.nav.syfo.motebehov.motebehovstatus.*
import no.nav.syfo.oppfolgingstilfelle.database.OppfolgingstilfelleDAO
import no.nav.syfo.oversikthendelse.OversikthendelseProducer
import no.nav.syfo.testhelper.*
import no.nav.syfo.testhelper.OidcTestHelper.loggInnBruker
import no.nav.syfo.testhelper.OidcTestHelper.loggInnVeilederAzure
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
import org.junit.*
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.cache.CacheManager
import org.springframework.http.HttpStatus
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.web.client.RestTemplate
import java.time.LocalDate
import java.util.function.Consumer
import javax.inject.Inject

@RunWith(SpringRunner::class)
@SpringBootTest(classes = [LocalApplication::class])
@DirtiesContext
class MotebehovArbeidsgiverV2Test {
    @Value("\${syfobehandlendeenhet.url}")
    private lateinit var behandlendeenhetUrl: String

    @Value("\${syfobrukertilgang.url}")
    private lateinit var brukertilgangUrl: String

    @Value("\${security.token.service.rest.url}")
    private lateinit var stsUrl: String

    @Value("\${tilgangskontrollapi.url}")
    private lateinit var tilgangskontrollUrl: String

    @Value("\${srv.username}")
    private lateinit var srvUsername: String

    @Value("\${srv.password}")
    private lateinit var srvPassword: String

    @Inject
    private lateinit var motebehovArbeidsgiverController: MotebehovArbeidsgiverV2Controller

    @Inject
    private lateinit var motebehovVeilederController: MotebehovVeilederADController

    @Inject
    private lateinit var oidcRequestContextHolder: OIDCRequestContextHolder

    @Inject
    private lateinit var motebehovDAO: MotebehovDAO

    @Inject
    private lateinit var cacheManager: CacheManager

    @Inject
    private lateinit var stsConsumer: StsConsumer

    @Inject
    private lateinit var oppfolgingstilfelleDAO: OppfolgingstilfelleDAO

    @Inject
    private lateinit var restTemplate: RestTemplate

    @MockBean
    private lateinit var aktorregisterConsumer: AktorregisterConsumer

    @MockBean
    private lateinit var pdlConsumer: PdlConsumer

    @MockBean
    private lateinit var oversikthendelseProducer: OversikthendelseProducer

    private lateinit var mockRestServiceServer: MockRestServiceServer

    private val motebehovGenerator = MotebehovGenerator()

    @Before
    fun setUp() {
        `when`(aktorregisterConsumer.getAktorIdForFodselsnummer(Fodselsnummer(ARBEIDSTAKER_FNR))).thenReturn(ARBEIDSTAKER_AKTORID)
        `when`(aktorregisterConsumer.getAktorIdForFodselsnummer(Fodselsnummer(LEDER_FNR))).thenReturn(LEDER_AKTORID)
        `when`(pdlConsumer.person(Fodselsnummer(ARBEIDSTAKER_FNR))).thenReturn(generatePdlHentPerson(null, null))
        mockRestServiceServer = MockRestServiceServer.bindTo(restTemplate).build()
        loggInnBruker(oidcRequestContextHolder, LEDER_FNR)
        cleanDB()
        mockAndExpectBrukertilgangRequest(mockRestServiceServer, brukertilgangUrl, ARBEIDSTAKER_FNR)
    }

    @After
    fun tearDown() {
        loggUtAlle(oidcRequestContextHolder)
        mockRestServiceServer.reset()
        cacheManager.cacheNames
                .forEach(Consumer { cacheName: String ->
                    val cache = cacheManager.getCache(cacheName)
                    cache?.clear()
                })
        cleanDB()
    }

    @Test
    fun getMotebehovStatusWithNoOppfolgingstilfelle() {
        motebehovArbeidsgiverController.motebehovStatusArbeidsgiver(ARBEIDSTAKER_FNR, VIRKSOMHETSNUMMER)
                .assertMotebehovStatus(false, null, null)
    }

    @Test
    fun getMotebehovStatusWithTodayOutsideOppfolgingstilfelleStart() {
        loggUtAlle(oidcRequestContextHolder)
        loggInnBruker(oidcRequestContextHolder, ARBEIDSTAKER_FNR)

        oppfolgingstilfelleDAO.create(generateOversikthendelsetilfelle.copy(
                fom = LocalDate.now().plusDays(1),
                tom = LocalDate.now().plusDays(10)
        ))
        motebehovArbeidsgiverController.motebehovStatusArbeidsgiver(ARBEIDSTAKER_FNR, VIRKSOMHETSNUMMER)
                .assertMotebehovStatus(false, null, null)
    }

    @Test
    fun getMotebehovStatusWithTodayOutsideOppfolgingstilfelleEnd() {
        loggUtAlle(oidcRequestContextHolder)
        loggInnBruker(oidcRequestContextHolder, ARBEIDSTAKER_FNR)

        oppfolgingstilfelleDAO.create(generateOversikthendelsetilfelle.copy(
                fom = LocalDate.now().minusDays(10),
                tom = LocalDate.now().minusDays(1)
        ))
        motebehovArbeidsgiverController.motebehovStatusArbeidsgiver(ARBEIDSTAKER_FNR, VIRKSOMHETSNUMMER)
                .assertMotebehovStatus(false, null, null)
    }

    @Test
    fun getMotebehovStatusWithTodayInsideOppfolgingstilfelleMergedBy2Oppfolgingstilfeller() {
        oppfolgingstilfelleDAO.create(generateOversikthendelsetilfelle.copy(
                virksomhetsnummer = VIRKSOMHETSNUMMER,
                fom = LocalDate.now().minusDays(DAYS_END_SVAR_BEHOV).minusDays(1),
                tom = LocalDate.now()
        ))
        oppfolgingstilfelleDAO.create(generateOversikthendelsetilfelle.copy(
                virksomhetsnummer = VIRKSOMHETSNUMMER_2,
                fom = LocalDate.now().minusDays(2),
                tom = LocalDate.now().plusDays(1)
        ))
        mockSTS()
        mockAndExpectMoteadminIsMoteplanleggerActive(mockRestServiceServer, false)

        motebehovArbeidsgiverController.motebehovStatusArbeidsgiver(ARBEIDSTAKER_FNR, VIRKSOMHETSNUMMER)
                .assertMotebehovStatus(true, MotebehovSkjemaType.MELD_BEHOV, null)
    }

    @Test
    fun getMotebehovStatusWithTodayInsideOppfolgingstilfelleDay1() {
        oppfolgingstilfelleDAO.create(generateOversikthendelsetilfelle.copy(
                fom = LocalDate.now(),
                tom = LocalDate.now().plusDays(1)
        ))
        mockSTS()
        mockAndExpectMoteadminIsMoteplanleggerActive(mockRestServiceServer, false)

        motebehovArbeidsgiverController.motebehovStatusArbeidsgiver(ARBEIDSTAKER_FNR, VIRKSOMHETSNUMMER)
                .assertMotebehovStatus(true, MotebehovSkjemaType.MELD_BEHOV, null)
    }

    @Test
    fun getMotebehovStatusWithTodayInsideOppfolgingstilfelleMedBehovMoteplanleggerActive() {
        val kOppfolgingstilfelle = generateOversikthendelsetilfelle.copy(
                fom = LocalDate.now(),
                tom = LocalDate.now().plusDays(1)
        )
        oppfolgingstilfelleDAO.create(kOppfolgingstilfelle)

        mockSTS()
        mockAndExpectMoteadminIsMoteplanleggerActive(mockRestServiceServer, true)

        motebehovArbeidsgiverController.motebehovStatusArbeidsgiver(ARBEIDSTAKER_FNR, VIRKSOMHETSNUMMER)
                .assertMotebehovStatus(false, null, null)
    }

    @Test
    fun getMotebehovStatusWithTodayInsideOppfolgingstilfelleMeldBehovSubmittedAndBehandlet() {
        val kOppfolgingstilfelle = generateOversikthendelsetilfelle.copy(
                fom = LocalDate.now(),
                tom = LocalDate.now().plusDays(1)
        )
        oppfolgingstilfelleDAO.create(kOppfolgingstilfelle)

        mockSTS()
        mockAndExpectMoteadminIsMoteplanleggerActive(mockRestServiceServer, false)
        val motebehovSvar = motebehovGenerator.lagMotebehovSvar(true)
        submitMotebehovAndSendOversikthendelse(motebehovSvar)

        mockRestServiceServer.reset()
        loggUtAlle(oidcRequestContextHolder)
        loggInnVeilederAzure(oidcRequestContextHolder, VEILEDER_ID)
        mockAndExpectSyfoTilgangskontroll(
                mockRestServiceServer,
                tilgangskontrollUrl,
                oidcRequestContextHolder.oidcValidationContext.getToken(OIDCIssuer.AZURE).idToken,
                ARBEIDSTAKER_FNR,
                HttpStatus.OK
        )
        motebehovVeilederController.behandleMotebehov(ARBEIDSTAKER_FNR)
        loggInnBruker(oidcRequestContextHolder, LEDER_FNR)

        mockRestServiceServer.reset()
        mockAndExpectBrukertilgangRequest(mockRestServiceServer, brukertilgangUrl, ARBEIDSTAKER_FNR)
        mockSTS()
        mockAndExpectMoteadminIsMoteplanleggerActive(mockRestServiceServer, false)

        motebehovArbeidsgiverController.motebehovStatusArbeidsgiver(ARBEIDSTAKER_FNR, VIRKSOMHETSNUMMER)
                .assertMotebehovStatus(true, MotebehovSkjemaType.MELD_BEHOV, null)
    }

    @Test
    fun getMotebehovStatusWithTodayInsideOppfolgingstilfelleMeldBehovMoteplanleggerActiveMeldBehovSubmitted() {
        val kOppfolgingstilfelle = generateOversikthendelsetilfelle.copy(
                fom = LocalDate.now(),
                tom = LocalDate.now().plusDays(1)
        )
        oppfolgingstilfelleDAO.create(kOppfolgingstilfelle)

        mockSTS()
        mockAndExpectMoteadminIsMoteplanleggerActive(mockRestServiceServer, false)
        val motebehovSvar = motebehovGenerator.lagMotebehovSvar(true)
        submitMotebehovAndSendOversikthendelse(motebehovSvar)

        mockRestServiceServer.reset()
        mockAndExpectBrukertilgangRequest(mockRestServiceServer, brukertilgangUrl, ARBEIDSTAKER_FNR)
        mockSTS()
        mockAndExpectMoteadminIsMoteplanleggerActive(mockRestServiceServer, true)

        motebehovArbeidsgiverController.motebehovStatusArbeidsgiver(ARBEIDSTAKER_FNR, VIRKSOMHETSNUMMER)
                .assertMotebehovStatus(true, MotebehovSkjemaType.MELD_BEHOV, motebehovSvar)
    }

    @Test
    fun getMotebehovStatusWithTodayInsideOppfolgingstilfelleBeforeSvarBehovStartDate() {
        oppfolgingstilfelleDAO.create(generateOversikthendelsetilfelle.copy(
                fom = LocalDate.now().minusDays(DAYS_START_SVAR_BEHOV).plusDays(1),
                tom = LocalDate.now().plusDays(1)
        ))
        mockSTS()
        mockAndExpectMoteadminIsMoteplanleggerActive(mockRestServiceServer, false)

        motebehovArbeidsgiverController.motebehovStatusArbeidsgiver(ARBEIDSTAKER_FNR, VIRKSOMHETSNUMMER)
                .assertMotebehovStatus(true, MotebehovSkjemaType.MELD_BEHOV, null)
    }

    @Test
    fun getMotebehovStatusWithTodayInsideOppfolgingstilfelleAfterSvarBehovEndDate() {
        oppfolgingstilfelleDAO.create(generateOversikthendelsetilfelle.copy(
                fom = LocalDate.now().minusDays(DAYS_END_SVAR_BEHOV),
                tom = LocalDate.now().plusDays(1)
        ))
        mockSTS()
        mockAndExpectMoteadminIsMoteplanleggerActive(mockRestServiceServer, false)

        motebehovArbeidsgiverController.motebehovStatusArbeidsgiver(ARBEIDSTAKER_FNR, VIRKSOMHETSNUMMER)
                .assertMotebehovStatus(true, MotebehovSkjemaType.MELD_BEHOV, null)
    }

    @Test
    fun getMotebehovStatusWithNoMotebehovAndMoteInsideSvarBehovUpperLimit() {
        val kOppfolgingstilfelle = generateOversikthendelsetilfelle.copy(
                fom = LocalDate.now().minusDays(DAYS_END_SVAR_BEHOV).plusDays(1),
                tom = LocalDate.now().plusDays(1)
        )
        mockSTS()
        mockAndExpectMoteadminHarAktivtMote(mockRestServiceServer, true)

        oppfolgingstilfelleDAO.create(kOppfolgingstilfelle)

        motebehovArbeidsgiverController.motebehovStatusArbeidsgiver(ARBEIDSTAKER_FNR, VIRKSOMHETSNUMMER)
                .assertMotebehovStatus(false, null, null)
    }

    @Test
    fun getMotebehovStatusWithSvarBehovAndMoteCreated() {
        val kOppfolgingstilfelle = generateOversikthendelsetilfelle.copy(
                fom = LocalDate.now().minusDays(DAYS_END_SVAR_BEHOV).plusDays(1),
                tom = LocalDate.now().plusDays(1)
        )
        oppfolgingstilfelleDAO.create(kOppfolgingstilfelle)

        mockSTS()
        mockAndExpectMoteadminHarAktivtMote(mockRestServiceServer, false)
        val motebehovSvar = motebehovGenerator.lagMotebehovSvar(true)
        submitMotebehovAndSendOversikthendelse(motebehovSvar)

        mockRestServiceServer.reset()
        mockAndExpectBrukertilgangRequest(mockRestServiceServer, brukertilgangUrl, ARBEIDSTAKER_FNR)
        mockSTS()
        mockAndExpectMoteadminHarAktivtMote(mockRestServiceServer, true)

        motebehovArbeidsgiverController.motebehovStatusArbeidsgiver(ARBEIDSTAKER_FNR, VIRKSOMHETSNUMMER)
                .assertMotebehovStatus(true, MotebehovSkjemaType.SVAR_BEHOV, motebehovSvar)
    }

    @Test
    fun getMotebehovStatusWithNoMotebehovAndNoMoteInsideSvarBehovLowerLimit() {
        val kOppfolgingstilfelle = generateOversikthendelsetilfelle.copy(
                fom = LocalDate.now().minusDays(DAYS_START_SVAR_BEHOV),
                tom = LocalDate.now().plusDays(1)
        )
        mockSTS()
        mockAndExpectMoteadminHarAktivtMote(mockRestServiceServer, false)

        oppfolgingstilfelleDAO.create(kOppfolgingstilfelle)

        motebehovArbeidsgiverController.motebehovStatusArbeidsgiver(ARBEIDSTAKER_FNR, VIRKSOMHETSNUMMER)
                .assertMotebehovStatus(true, MotebehovSkjemaType.SVAR_BEHOV, null)
    }

    @Test
    fun getMotebehovStatusWithNoMotebehovAndNoMote() {
        val kOppfolgingstilfelle = generateOversikthendelsetilfelle
        oppfolgingstilfelleDAO.create(kOppfolgingstilfelle)

        mockSTS()
        mockAndExpectMoteadminHarAktivtMote(mockRestServiceServer, false)

        motebehovArbeidsgiverController.motebehovStatusArbeidsgiver(ARBEIDSTAKER_FNR, VIRKSOMHETSNUMMER)
                .assertMotebehovStatus(true, MotebehovSkjemaType.SVAR_BEHOV, null)
    }

    @Test
    fun getMotebehovStatusWithNoMotebehovAndMote() {
        val kOppfolgingstilfelle = generateOversikthendelsetilfelle
        oppfolgingstilfelleDAO.create(kOppfolgingstilfelle)

        mockSTS()
        mockAndExpectMoteadminHarAktivtMote(mockRestServiceServer, true)

        motebehovArbeidsgiverController.motebehovStatusArbeidsgiver(ARBEIDSTAKER_FNR, VIRKSOMHETSNUMMER)
                .assertMotebehovStatus(false, null, null)
    }

    @Test
    fun getMotebehovStatusAndSendOversikthendelseWithMotebehovHarBehovTrue() {
        oppfolgingstilfelleDAO.create(generateOversikthendelsetilfelle)
        lagreOgHentMotebehovOgSendOversikthendelse(harBehov = true)
    }

    @Test
    fun getMotebehovStatusAndSendOversikthendelseWithMotebehovHarBehovFalse() {
        oppfolgingstilfelleDAO.create(generateOversikthendelsetilfelle)
        lagreOgHentMotebehovOgSendOversikthendelse(false)
    }

    private fun mockSTS() {
        if (!stsConsumer.isTokenCached()) {
            mockAndExpectSTSService(mockRestServiceServer, stsUrl, srvUsername, srvPassword)
        }
    }

    private fun submitMotebehovAndSendOversikthendelse(motebehovSvar: MotebehovSvar) {
        mockSTS()
        mockAndExpectBehandlendeEnhetRequest(mockRestServiceServer, behandlendeenhetUrl, ARBEIDSTAKER_FNR)

        motebehovArbeidsgiverController.lagreMotebehovArbeidsgiver(motebehovGenerator.lagNyttMotebehovArbeidsgiver().copy(
                motebehovSvar = motebehovSvar
        ))
        if (motebehovSvar.harMotebehov) {
            Mockito.verify(oversikthendelseProducer).sendOversikthendelse(any())
        } else {
            Mockito.verify(oversikthendelseProducer, Mockito.never()).sendOversikthendelse(any())
        }
    }

    private fun lagreOgHentMotebehovOgSendOversikthendelse(harBehov: Boolean) {
        mockSTS()
        mockAndExpectMoteadminHarAktivtMote(mockRestServiceServer, false)
        mockAndExpectBehandlendeEnhetRequest(mockRestServiceServer, behandlendeenhetUrl, ARBEIDSTAKER_FNR)

        val motebehovSvar = motebehovGenerator.lagMotebehovSvar(harBehov)

        motebehovArbeidsgiverController.lagreMotebehovArbeidsgiver(motebehovGenerator.lagNyttMotebehovArbeidsgiver().copy(
                motebehovSvar = motebehovSvar
        ))
        if (!harBehov) {
            mockRestServiceServer.reset()
            mockAndExpectBrukertilgangRequest(mockRestServiceServer, brukertilgangUrl, ARBEIDSTAKER_FNR)
            mockAndExpectMoteadminHarAktivtMote(mockRestServiceServer, false)
        }

        val motebehovStatus: MotebehovStatus = motebehovArbeidsgiverController.motebehovStatusArbeidsgiver(
                ARBEIDSTAKER_FNR,
                VIRKSOMHETSNUMMER
        )

        Assert.assertTrue(motebehovStatus.visMotebehov)
        Assert.assertEquals(MotebehovSkjemaType.SVAR_BEHOV, motebehovStatus.skjemaType)
        val motebehov = motebehovStatus.motebehov!!
        Assert.assertNotNull(motebehov)
        Assertions.assertThat(motebehov.opprettetAv).isEqualTo(LEDER_AKTORID)
        Assertions.assertThat(motebehov.arbeidstakerFnr).isEqualTo(ARBEIDSTAKER_FNR)
        Assertions.assertThat(motebehov.virksomhetsnummer).isEqualTo(VIRKSOMHETSNUMMER)
        Assertions.assertThat(motebehov.motebehovSvar).isEqualToComparingFieldByField(motebehovSvar)
        if (harBehov) {
            Mockito.verify(oversikthendelseProducer).sendOversikthendelse(any())
        } else {
            Mockito.verify(oversikthendelseProducer, Mockito.never()).sendOversikthendelse(any())
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
