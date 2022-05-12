package no.nav.syfo.motebehov.api

import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.syfo.LocalApplication
import no.nav.syfo.api.auth.OIDCIssuer
import no.nav.syfo.consumer.aktorregister.AktorregisterConsumer
import no.nav.syfo.consumer.aktorregister.domain.Fodselsnummer
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
import no.nav.syfo.testhelper.OidcTestHelper.loggUtAlle
import no.nav.syfo.testhelper.UserConstants.ARBEIDSTAKER_AKTORID
import no.nav.syfo.testhelper.UserConstants.ARBEIDSTAKER_FNR
import no.nav.syfo.testhelper.UserConstants.VEILEDER_ID
import no.nav.syfo.testhelper.UserConstants.VIRKSOMHETSNUMMER
import no.nav.syfo.testhelper.assertion.assertMotebehovStatus
import no.nav.syfo.testhelper.generator.*
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.Mockito.times
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
class MotebehovArbeidstakerV2Test {
    @Value("\${azure.openid.config.token.endpoint}")
    private lateinit var azureTokenEndpoint: String

    @Value("\${syfobehandlendeenhet.url}")
    private lateinit var behandlendeenhetUrl: String

    @Value("\${tilgangskontrollapi.url}")
    private lateinit var tilgangskontrollUrl: String

    @Inject
    private lateinit var motebehovArbeidstakerController: MotebehovArbeidstakerV2Controller

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
    private lateinit var stsConsumer: StsConsumer

    @MockBean
    private lateinit var oversikthendelseProducer: OversikthendelseProducer

    private lateinit var mockRestServiceServer: MockRestServiceServer

    private val motebehovGenerator = MotebehovGenerator()

    private val stsToken = generateStsToken().access_token

    @BeforeEach
    fun setUp() {
        `when`(aktorregisterConsumer.getAktorIdForFodselsnummer(Fodselsnummer(ARBEIDSTAKER_FNR))).thenReturn(ARBEIDSTAKER_AKTORID)
        `when`(pdlConsumer.person(Fodselsnummer(ARBEIDSTAKER_FNR))).thenReturn(generatePdlHentPerson(null, null))
        `when`(stsConsumer.token()).thenReturn(stsToken)
        mockRestServiceServer = MockRestServiceServer.bindTo(restTemplate).build()
        mockRestServiceWithProxyServer = MockRestServiceServer.bindTo(restTemplateWithProxy).build()
        loggInnBruker(contextHolder, ARBEIDSTAKER_FNR)
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
    fun `get MotebehovStatus With No Oppfolgingstilfelle`() {
        motebehovArbeidstakerController.motebehovStatusArbeidstaker()
            .assertMotebehovStatus(false, null, null)
    }

    @Test
    fun `get MotebehovStatus With Today Outside OppfolgingstilfelleStart`() {
        loggUtAlle(contextHolder)
        loggInnBruker(contextHolder, ARBEIDSTAKER_FNR)

        val oppfolgingstilfellePerson = generateOppfolgingstilfellePerson(
            start = LocalDate.now().plusDays(1),
            end = LocalDate.now().plusDays(10)
        )

        dbCreateOppfolgingstilfelle(oppfolgingstilfelleDAO, oppfolgingstilfellePerson)

        motebehovArbeidstakerController.motebehovStatusArbeidstaker()
            .assertMotebehovStatus(false, null, null)
    }

    @Test
    fun `get MotebehovStatus With Today Outside OppfolgingstilfelleEnd`() {
        loggUtAlle(contextHolder)
        loggInnBruker(contextHolder, ARBEIDSTAKER_FNR)

        val oppfolgingstilfellePerson = generateOppfolgingstilfellePerson(
            start = LocalDate.now().minusDays(10),
            end = LocalDate.now().minusDays(1)
        )

        dbCreateOppfolgingstilfelle(oppfolgingstilfelleDAO, oppfolgingstilfellePerson)

        motebehovArbeidstakerController.motebehovStatusArbeidstaker()
            .assertMotebehovStatus(false, null, null)
    }

    @Test
    fun `get MotebehovStatus With Today Inside Oppfolgingstilfelle Merged By Active And Expired Oppfolgingstilfelle No Overlap`() {
        val activeOppfolgingstilfelleStartDate = LocalDate.now().minusDays(DAYS_START_SVAR_BEHOV).plusDays(1)

        val oppfolgingstilfellePerson = generateOppfolgingstilfellePerson(
            start = activeOppfolgingstilfelleStartDate.minusDays(2),
            end = activeOppfolgingstilfelleStartDate.minusDays(1)
        )

        dbCreateOppfolgingstilfelle(oppfolgingstilfelleDAO, oppfolgingstilfellePerson)

        val oppfolgingstilfellePerson2 = generateOppfolgingstilfellePerson(
            virksomhetsnummerList = listOf(UserConstants.VIRKSOMHETSNUMMER_2),
            start = activeOppfolgingstilfelleStartDate,
            end = LocalDate.now().plusDays(1)
        )

        dbCreateOppfolgingstilfelle(oppfolgingstilfelleDAO, oppfolgingstilfellePerson2)

        mockAndExpectMoteadminIsMoteplanleggerActive(mockRestServiceServer, false)

        motebehovArbeidstakerController.motebehovStatusArbeidstaker()
            .assertMotebehovStatus(true, MotebehovSkjemaType.MELD_BEHOV, null)
    }

    @Test
    fun `getMotebehovStatus With Today Inside Oppfolgingstilfelle Merged By Active And Expired Oppfolgingstilfelle With Overlap`() {
        val activeOppfolgingstilfelleStartDate = LocalDate.now().minusDays(DAYS_START_SVAR_BEHOV).plusDays(1)

        val oppfolgingstilfellePerson = generateOppfolgingstilfellePerson(
            start = activeOppfolgingstilfelleStartDate.minusDays(2),
            end = activeOppfolgingstilfelleStartDate
        )

        dbCreateOppfolgingstilfelle(oppfolgingstilfelleDAO, oppfolgingstilfellePerson)

        val oppfolgingstilfellePerson2 = generateOppfolgingstilfellePerson(
            virksomhetsnummerList = listOf(UserConstants.VIRKSOMHETSNUMMER_2),
            start = activeOppfolgingstilfelleStartDate,
            end = LocalDate.now().plusDays(1)
        )

        dbCreateOppfolgingstilfelle(oppfolgingstilfelleDAO, oppfolgingstilfellePerson2)

        mockAndExpectMoteadminHarAktivtMote(mockRestServiceServer, false)

        motebehovArbeidstakerController.motebehovStatusArbeidstaker()
            .assertMotebehovStatus(true, MotebehovSkjemaType.SVAR_BEHOV, null)
    }

    @Test
    fun `get MotebehovStatus With Today Inside Oppfolgingstilfelle Merged By 2 Oppfolgingstilfeller`() {
        val oppfolgingstilfellePerson = generateOppfolgingstilfellePerson(
            start = LocalDate.now().minusDays(DAYS_END_SVAR_BEHOV).minusDays(1),
            end = LocalDate.now()
        )

        dbCreateOppfolgingstilfelle(oppfolgingstilfelleDAO, oppfolgingstilfellePerson)

        val oppfolgingstilfellePerson2 = generateOppfolgingstilfellePerson(
            virksomhetsnummerList = listOf(UserConstants.VIRKSOMHETSNUMMER_2),
            start = LocalDate.now().minusDays(2),
            end = LocalDate.now().plusDays(1)
        )

        dbCreateOppfolgingstilfelle(oppfolgingstilfelleDAO, oppfolgingstilfellePerson2)

        mockAndExpectMoteadminIsMoteplanleggerActive(mockRestServiceServer, false)

        motebehovArbeidstakerController.motebehovStatusArbeidstaker()
            .assertMotebehovStatus(true, MotebehovSkjemaType.MELD_BEHOV, null)
    }

    @Test
    fun `get MotebehovStatus With Today Inside Oppfolgingstilfelle Day1`() {
        val oppfolgingstilfellePerson = generateOppfolgingstilfellePerson(
            start = LocalDate.now(),
            end = LocalDate.now()
        )

        dbCreateOppfolgingstilfelle(oppfolgingstilfelleDAO, oppfolgingstilfellePerson)

        mockAndExpectMoteadminIsMoteplanleggerActive(mockRestServiceServer, false)

        motebehovArbeidstakerController.motebehovStatusArbeidstaker()
            .assertMotebehovStatus(true, MotebehovSkjemaType.MELD_BEHOV, null)
    }

    @Test
    fun `get MotebehovStatus With Today Inside Oppfolgingstilfelle LastDay`() {
        val oppfolgingstilfellePerson = generateOppfolgingstilfellePerson(
            start = LocalDate.now().minusDays(DAYS_END_SVAR_BEHOV).minusDays(1),
            end = LocalDate.now()
        )

        dbCreateOppfolgingstilfelle(oppfolgingstilfelleDAO, oppfolgingstilfellePerson)

        mockAndExpectMoteadminIsMoteplanleggerActive(mockRestServiceServer, false)

        motebehovArbeidstakerController.motebehovStatusArbeidstaker()
            .assertMotebehovStatus(true, MotebehovSkjemaType.MELD_BEHOV, null)
    }

    @Test
    fun `get MotebehovStatus With Today Inside Oppfolgingstilfelle, MeldBehov Moteplanlegger Active`() {
        val oppfolgingstilfellePerson = generateOppfolgingstilfellePerson(
            start = LocalDate.now(),
            end = LocalDate.now().plusDays(1)
        )

        dbCreateOppfolgingstilfelle(oppfolgingstilfelleDAO, oppfolgingstilfellePerson)

        mockAndExpectMoteadminIsMoteplanleggerActive(mockRestServiceServer, true)

        motebehovArbeidstakerController.motebehovStatusArbeidstaker()
            .assertMotebehovStatus(false, null, null)
    }

    @Test
    fun `get MotebehovStatus With Today Inside Oppfolgingstilfelle, MeldBehov Submitted And Behandlet`() {
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
        OidcTestHelper.loggInnVeilederADV2(contextHolder, VEILEDER_ID)
        mockAndExpectSyfoTilgangskontroll(
            mockRestServiceServer,
            tilgangskontrollUrl,
            contextHolder.tokenValidationContext.getJwtToken(OIDCIssuer.INTERN_AZUREAD_V2).tokenAsString,
            ARBEIDSTAKER_FNR,
            HttpStatus.OK
        )
        motebehovVeilederController.behandleMotebehov(ARBEIDSTAKER_FNR)
        loggInnBruker(contextHolder, ARBEIDSTAKER_FNR)

        mockRestServiceServer.reset()
        mockAndExpectMoteadminIsMoteplanleggerActive(mockRestServiceServer, false)

        motebehovArbeidstakerController.motebehovStatusArbeidstaker()
            .assertMotebehovStatus(true, MotebehovSkjemaType.MELD_BEHOV, null)
    }

    @Test
    fun `get MotebehovStatus With Today Inside Oppfolgingstilfelle MeldBehov, Moteplanlegger Active, MeldBehov Submitted`() {
        val oppfolgingstilfellePerson = generateOppfolgingstilfellePerson(
            start = LocalDate.now(),
            end = LocalDate.now().plusDays(1)
        )

        dbCreateOppfolgingstilfelle(oppfolgingstilfelleDAO, oppfolgingstilfellePerson)

        val motebehovSvar = motebehovGenerator.lagMotebehovSvar(true)

        mockAndExpectMoteadminIsMoteplanleggerActive(mockRestServiceServer, false)
        submitMotebehovAndSendOversikthendelse(motebehovSvar)

        mockRestServiceServer.reset()
        mockAndExpectMoteadminIsMoteplanleggerActive(mockRestServiceServer, true)

        motebehovArbeidstakerController.motebehovStatusArbeidstaker()
            .assertMotebehovStatus(true, MotebehovSkjemaType.MELD_BEHOV, motebehovSvar)
    }

    @Test
    fun `get MotebehovStatus With Today Inside Oppfolgingstilfelle Before SvarBehov StartDate`() {
        val oppfolgingstilfellePerson = generateOppfolgingstilfellePerson(
            start = LocalDate.now().minusDays(DAYS_START_SVAR_BEHOV).plusDays(1),
            end = LocalDate.now().plusDays(1)
        )

        dbCreateOppfolgingstilfelle(oppfolgingstilfelleDAO, oppfolgingstilfellePerson)

        mockAndExpectMoteadminIsMoteplanleggerActive(mockRestServiceServer, false)

        motebehovArbeidstakerController.motebehovStatusArbeidstaker()
            .assertMotebehovStatus(true, MotebehovSkjemaType.MELD_BEHOV, null)
    }

    @Test
    fun `get MotebehovStatus With Today Inside Oppfolgingstilfelle After SvarBehov EndDate`() {
        val oppfolgingstilfellePerson = generateOppfolgingstilfellePerson(
            start = LocalDate.now().minusDays(DAYS_END_SVAR_BEHOV),
            end = LocalDate.now().plusDays(1)
        )

        dbCreateOppfolgingstilfelle(oppfolgingstilfelleDAO, oppfolgingstilfellePerson)

        mockAndExpectMoteadminIsMoteplanleggerActive(mockRestServiceServer, false)

        motebehovArbeidstakerController.motebehovStatusArbeidstaker()
            .assertMotebehovStatus(true, MotebehovSkjemaType.MELD_BEHOV, null)
    }

    @Test
    fun `get MotebehovStatus With No Motebehov And Mote Inside SvarBehov Upper Limit`() {
        val oppfolgingstilfellePerson = generateOppfolgingstilfellePerson(
            start = LocalDate.now().minusDays(DAYS_END_SVAR_BEHOV).plusDays(1),
            end = LocalDate.now().plusDays(1)
        )

        mockAndExpectMoteadminHarAktivtMote(mockRestServiceServer, true)

        dbCreateOppfolgingstilfelle(oppfolgingstilfelleDAO, oppfolgingstilfellePerson)

        motebehovArbeidstakerController.motebehovStatusArbeidstaker()
            .assertMotebehovStatus(false, null, null)
    }

    @Test
    fun `get MotebehovStatus with SvarBehov and Mote created`() {
        val oppfolgingstilfellePerson = generateOppfolgingstilfellePerson(
            start = LocalDate.now().minusDays(DAYS_END_SVAR_BEHOV).plusDays(1),
            end = LocalDate.now().plusDays(1)
        )

        dbCreateOppfolgingstilfelle(oppfolgingstilfelleDAO, oppfolgingstilfellePerson)

        val motebehovSvar = motebehovGenerator.lagMotebehovSvar(true)

        mockAndExpectMoteadminHarAktivtMote(mockRestServiceServer, false)
        submitMotebehovAndSendOversikthendelse(motebehovSvar)

        mockRestServiceServer.reset()
        mockAndExpectMoteadminHarAktivtMote(mockRestServiceServer, true)

        motebehovArbeidstakerController.motebehovStatusArbeidstaker()
            .assertMotebehovStatus(true, MotebehovSkjemaType.SVAR_BEHOV, motebehovSvar)
    }

    @Test
    fun `get MotebehovStatus with no Motebehov and no Mote inside SvarBehov lower limit`() {
        val oppfolgingstilfellePerson = generateOppfolgingstilfellePerson(
            start = LocalDate.now().minusDays(DAYS_START_SVAR_BEHOV),
            end = LocalDate.now().plusDays(1)
        )
        mockAndExpectMoteadminHarAktivtMote(mockRestServiceServer, false)

        dbCreateOppfolgingstilfelle(oppfolgingstilfelleDAO, oppfolgingstilfellePerson)

        motebehovArbeidstakerController.motebehovStatusArbeidstaker()
            .assertMotebehovStatus(true, MotebehovSkjemaType.SVAR_BEHOV, null)
    }

    @Test
    fun `get MotebehovStatus with no Motebehov and no Mote`() {
        val oppfolgingstilfellePerson = generateOppfolgingstilfellePerson()

        dbCreateOppfolgingstilfelle(oppfolgingstilfelleDAO, oppfolgingstilfellePerson)

        mockAndExpectMoteadminHarAktivtMote(mockRestServiceServer, false)

        motebehovArbeidstakerController.motebehovStatusArbeidstaker()
            .assertMotebehovStatus(true, MotebehovSkjemaType.SVAR_BEHOV, null)
    }

    @Test
    fun `get MotebehovStatus with no Motebehov and Mote`() {
        val oppfolgingstilfellePerson = generateOppfolgingstilfellePerson()

        dbCreateOppfolgingstilfelle(oppfolgingstilfelleDAO, oppfolgingstilfellePerson)

        mockAndExpectMoteadminHarAktivtMote(mockRestServiceServer, true)

        motebehovArbeidstakerController.motebehovStatusArbeidstaker()
            .assertMotebehovStatus(false, null, null)
    }

    @Test
    fun `get MotebehovStatus and sendOversikthendelse with Motebehov harBehov=true`() {
        val oppfolgingstilfellePerson = generateOppfolgingstilfellePerson()

        dbCreateOppfolgingstilfelle(oppfolgingstilfelleDAO, oppfolgingstilfellePerson)

        lagreOgHentMotebehovOgSendOversikthendelse(harBehov = true)
    }

    @Test
    fun `get MotebehovStatus and SendOversikthendelse with Motebehov harBehov=false`() {
        val oppfolgingstilfellePerson = generateOppfolgingstilfellePerson()

        dbCreateOppfolgingstilfelle(oppfolgingstilfelleDAO, oppfolgingstilfellePerson)

        lagreOgHentMotebehovOgSendOversikthendelse(harBehov = false)
    }

    @Test
    fun `submitMotebehov multiple active Oppfolgingstilfeller`() {
        val oppfolgingstilfellePerson = generateOppfolgingstilfellePerson(
            start = LocalDate.now().minusDays(DAYS_END_SVAR_BEHOV).minusDays(1),
            end = LocalDate.now()
        )

        dbCreateOppfolgingstilfelle(oppfolgingstilfelleDAO, oppfolgingstilfellePerson)

        val oppfolgingstilfellePerson2 = generateOppfolgingstilfellePerson(
            virksomhetsnummerList = listOf(UserConstants.VIRKSOMHETSNUMMER_2),
            start = LocalDate.now().minusDays(2),
            end = LocalDate.now().plusDays(1)
        )

        dbCreateOppfolgingstilfelle(oppfolgingstilfelleDAO, oppfolgingstilfellePerson2)

        mockAndExpectMoteadminIsMoteplanleggerActive(mockRestServiceServer, false)
        mockAndExpectBehandlendeEnhetRequest(
            azureTokenEndpoint,
            mockRestServiceWithProxyServer,
            mockRestServiceServer,
            behandlendeenhetUrl,
            ARBEIDSTAKER_FNR
        )
        mockAndExpectBehandlendeEnhetRequest(
            azureTokenEndpoint,
            mockRestServiceWithProxyServer,
            mockRestServiceServer,
            behandlendeenhetUrl,
            ARBEIDSTAKER_FNR
        )

        val motebehovSvar = motebehovGenerator.lagMotebehovSvar(true)
        motebehovArbeidstakerController.submitMotebehovArbeidstaker(motebehovSvar)

        val motebehovList = motebehovDAO.hentMotebehovListeForOgOpprettetAvArbeidstaker(ARBEIDSTAKER_AKTORID)

        assertEquals(2, motebehovList.size)
        Mockito.verify(oversikthendelseProducer, times(2)).sendOversikthendelse(any(), any())
    }

    private fun submitMotebehovAndSendOversikthendelse(motebehovSvar: MotebehovSvar) {
        mockAndExpectBehandlendeEnhetRequest(
            azureTokenEndpoint,
            mockRestServiceWithProxyServer,
            mockRestServiceServer,
            behandlendeenhetUrl,
            ARBEIDSTAKER_FNR
        )

        motebehovArbeidstakerController.submitMotebehovArbeidstaker(motebehovSvar)
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

        motebehovArbeidstakerController.submitMotebehovArbeidstaker(motebehovSvar)

        if (!harBehov) {
            mockRestServiceServer.reset()
            mockAndExpectMoteadminHarAktivtMote(mockRestServiceServer, false)
        }

        val motebehovStatus: MotebehovStatus = motebehovArbeidstakerController.motebehovStatusArbeidstaker()

        assertTrue(motebehovStatus.visMotebehov)
        assertEquals(MotebehovSkjemaType.SVAR_BEHOV, motebehovStatus.skjemaType)
        val motebehov = motebehovStatus.motebehov!!
        assertNotNull(motebehov)
        Assertions.assertThat(motebehov.opprettetAv).isEqualTo(ARBEIDSTAKER_AKTORID)
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
