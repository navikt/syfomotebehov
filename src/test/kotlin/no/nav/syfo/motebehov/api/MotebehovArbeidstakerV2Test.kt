package no.nav.syfo.motebehov.api

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.syfo.LocalApplication
import no.nav.syfo.consumer.aktorregister.AktorregisterConsumer
import no.nav.syfo.consumer.aktorregister.domain.Fodselsnummer
import no.nav.syfo.consumer.azuread.v2.AzureAdV2TokenConsumer
import no.nav.syfo.consumer.pdl.PdlConsumer
import no.nav.syfo.consumer.sts.StsConsumer
import no.nav.syfo.dialogmotekandidat.database.DialogmotekandidatDAO
import no.nav.syfo.dialogmotekandidat.database.DialogmotekandidatEndringArsak
import no.nav.syfo.motebehov.MotebehovSvar
import no.nav.syfo.motebehov.api.internad.v2.MotebehovVeilederADControllerV2
import no.nav.syfo.motebehov.database.MotebehovDAO
import no.nav.syfo.motebehov.motebehovstatus.DAYS_END_SVAR_BEHOV
import no.nav.syfo.motebehov.motebehovstatus.DAYS_START_SVAR_BEHOV
import no.nav.syfo.motebehov.motebehovstatus.MotebehovSkjemaType
import no.nav.syfo.motebehov.motebehovstatus.MotebehovStatus
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
import no.nav.syfo.testhelper.generator.MotebehovGenerator
import no.nav.syfo.testhelper.generator.generateOversikthendelsetilfelle
import no.nav.syfo.testhelper.generator.generatePdlHentPerson
import no.nav.syfo.testhelper.generator.generateStsToken
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
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

    @Autowired
    private lateinit var contextHolder: TokenValidationContextHolder

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

    @MockkBean
    private lateinit var aktorregisterConsumer: AktorregisterConsumer

    @MockkBean
    private lateinit var pdlConsumer: PdlConsumer

    @MockkBean
    private lateinit var stsConsumer: StsConsumer

    @MockkBean(relaxed = true)
    private lateinit var oversikthendelseProducer: OversikthendelseProducer

    private lateinit var mockRestServiceServer: MockRestServiceServer

    private val motebehovGenerator = MotebehovGenerator()

    private val stsToken = generateStsToken().access_token

    @BeforeEach
    fun setUp() {
        every { aktorregisterConsumer.getAktorIdForFodselsnummer(Fodselsnummer(ARBEIDSTAKER_FNR)) } returns ARBEIDSTAKER_AKTORID
        every { pdlConsumer.person(Fodselsnummer(ARBEIDSTAKER_FNR)) } returns generatePdlHentPerson(null, null)
        every { stsConsumer.token() } returns stsToken
        every { pdlConsumer.isKode6(Fodselsnummer(ARBEIDSTAKER_FNR)) } returns false

        mockRestServiceServer = MockRestServiceServer.bindTo(restTemplate).build()
        mockRestServiceWithProxyServer = MockRestServiceServer.bindTo(restTemplateWithProxy).build()
        loggInnBruker(contextHolder, ARBEIDSTAKER_FNR)
        cleanDB()
    }

    @AfterEach
    fun tearDown() {
        loggUtAlle(contextHolder)
        resetMockRestServers()
        cacheManager.cacheNames
            .forEach(
                Consumer { cacheName: String ->
                    val cache = cacheManager.getCache(cacheName)
                    cache?.clear()
                }
            )
        AzureAdV2TokenConsumer.Companion.clearCache()
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

        oppfolgingstilfelleDAO.create(
            generateOversikthendelsetilfelle.copy(
                fom = LocalDate.now().plusDays(1),
                tom = LocalDate.now().plusDays(10)
            )
        )
        motebehovArbeidstakerController.motebehovStatusArbeidstaker()
            .assertMotebehovStatus(false, null, null)
    }

    @Test
    fun `get MotebehovStatus With Today Outside OppfolgingstilfelleEnd`() {
        loggUtAlle(contextHolder)
        loggInnBruker(contextHolder, ARBEIDSTAKER_FNR)

        oppfolgingstilfelleDAO.create(
            generateOversikthendelsetilfelle.copy(
                fom = LocalDate.now().minusDays(10),
                tom = LocalDate.now().minusDays(1)
            )
        )
        motebehovArbeidstakerController.motebehovStatusArbeidstaker()
            .assertMotebehovStatus(false, null, null)
    }

    @Test
    fun `get MotebehovStatus With Today Inside Oppfolgingstilfelle Merged By Active And Expired Oppfolgingstilfelle No Overlap`() {
        val activeOppfolgingstilfelleStartDate = LocalDate.now().minusDays(DAYS_START_SVAR_BEHOV).plusDays(1)
        oppfolgingstilfelleDAO.create(
            generateOversikthendelsetilfelle.copy(
                virksomhetsnummer = VIRKSOMHETSNUMMER,
                fom = activeOppfolgingstilfelleStartDate.minusDays(2),
                tom = activeOppfolgingstilfelleStartDate.minusDays(1)
            )
        )
        oppfolgingstilfelleDAO.create(
            generateOversikthendelsetilfelle.copy(
                virksomhetsnummer = UserConstants.VIRKSOMHETSNUMMER_2,
                fom = activeOppfolgingstilfelleStartDate,
                tom = LocalDate.now().plusDays(1)
            )
        )

        motebehovArbeidstakerController.motebehovStatusArbeidstaker()
            .assertMotebehovStatus(true, MotebehovSkjemaType.MELD_BEHOV, null)
    }

    @Test
    fun `getMotebehovStatus With Today Inside Oppfolgingstilfelle Merged By Active And Expired Oppfolgingstilfelle With Overlap`() {
        createKandidatInDB()

        val activeOppfolgingstilfelleStartDate = LocalDate.now().minusDays(DAYS_START_SVAR_BEHOV).plusDays(1)
        oppfolgingstilfelleDAO.create(
            generateOversikthendelsetilfelle.copy(
                virksomhetsnummer = VIRKSOMHETSNUMMER,
                fom = activeOppfolgingstilfelleStartDate.minusDays(2),
                tom = activeOppfolgingstilfelleStartDate
            )
        )
        oppfolgingstilfelleDAO.create(
            generateOversikthendelsetilfelle.copy(
                virksomhetsnummer = UserConstants.VIRKSOMHETSNUMMER_2,
                fom = activeOppfolgingstilfelleStartDate,
                tom = LocalDate.now().plusDays(1)
            )
        )

        motebehovArbeidstakerController.motebehovStatusArbeidstaker()
            .assertMotebehovStatus(true, MotebehovSkjemaType.SVAR_BEHOV, null)
    }

    @Test
    fun `get MotebehovStatus With Today Inside Oppfolgingstilfelle Merged By 2 Oppfolgingstilfeller`() {
        oppfolgingstilfelleDAO.create(
            generateOversikthendelsetilfelle.copy(
                virksomhetsnummer = VIRKSOMHETSNUMMER,
                fom = LocalDate.now().minusDays(DAYS_END_SVAR_BEHOV).minusDays(1),
                tom = LocalDate.now()
            )
        )
        oppfolgingstilfelleDAO.create(
            generateOversikthendelsetilfelle.copy(
                virksomhetsnummer = UserConstants.VIRKSOMHETSNUMMER_2,
                fom = LocalDate.now().minusDays(2),
                tom = LocalDate.now().plusDays(1)
            )
        )

        motebehovArbeidstakerController.motebehovStatusArbeidstaker()
            .assertMotebehovStatus(true, MotebehovSkjemaType.MELD_BEHOV, null)
    }

    @Test
    fun `get MotebehovStatus With Today Inside Oppfolgingstilfelle Day1`() {
        oppfolgingstilfelleDAO.create(
            generateOversikthendelsetilfelle.copy(
                fom = LocalDate.now(),
                tom = LocalDate.now()
            )
        )

        motebehovArbeidstakerController.motebehovStatusArbeidstaker()
            .assertMotebehovStatus(true, MotebehovSkjemaType.MELD_BEHOV, null)
    }

    @Test
    fun `get MotebehovStatus With Today Inside Oppfolgingstilfelle LastDay`() {
        oppfolgingstilfelleDAO.create(
            generateOversikthendelsetilfelle.copy(
                fom = LocalDate.now().minusDays(DAYS_END_SVAR_BEHOV).minusDays(1),
                tom = LocalDate.now()
            )
        )

        motebehovArbeidstakerController.motebehovStatusArbeidstaker()
            .assertMotebehovStatus(true, MotebehovSkjemaType.MELD_BEHOV, null)
    }

    @Test
    fun `get MotebehovStatus With Today Inside Oppfolgingstilfelle`() {
        val kOppfolgingstilfelle = generateOversikthendelsetilfelle.copy(
            fom = LocalDate.now(),
            tom = LocalDate.now().plusDays(1)
        )
        oppfolgingstilfelleDAO.create(kOppfolgingstilfelle)

        motebehovArbeidstakerController.motebehovStatusArbeidstaker()
            .assertMotebehovStatus(true, MotebehovSkjemaType.MELD_BEHOV, null)
    }

    @Test
    fun `get MotebehovStatus With Today Inside Oppfolgingstilfelle, MeldBehov Submitted And Behandlet`() {
        val kOppfolgingstilfelle = generateOversikthendelsetilfelle.copy(
            fom = LocalDate.now(),
            tom = LocalDate.now().plusDays(1)
        )
        oppfolgingstilfelleDAO.create(kOppfolgingstilfelle)

        val motebehovSvar = motebehovGenerator.lagMotebehovSvar(true)
        submitMotebehovAndSendOversikthendelse(motebehovSvar)

        resetMockRestServers()
        loggUtAlle(contextHolder)
        OidcTestHelper.loggInnVeilederADV2(contextHolder, VEILEDER_ID)
        mockBehandlendEnhetWithTilgangskontroll(ARBEIDSTAKER_FNR)
        motebehovVeilederController.behandleMotebehov(ARBEIDSTAKER_FNR)

        resetMockRestServers()
        loggInnBruker(contextHolder, ARBEIDSTAKER_FNR)

        motebehovArbeidstakerController.motebehovStatusArbeidstaker()
            .assertMotebehovStatus(true, MotebehovSkjemaType.MELD_BEHOV, null)
    }

    @Test
    fun `get MotebehovStatus With Today Inside Oppfolgingstilfelle MeldBehov, Moteplanlegger Active, MeldBehov Submitted`() {
        val kOppfolgingstilfelle = generateOversikthendelsetilfelle.copy(
            fom = LocalDate.now(),
            tom = LocalDate.now().plusDays(1)
        )
        oppfolgingstilfelleDAO.create(kOppfolgingstilfelle)
        val motebehovSvar = motebehovGenerator.lagMotebehovSvar(true)

        submitMotebehovAndSendOversikthendelse(motebehovSvar)

        mockRestServiceServer.reset()

        motebehovArbeidstakerController.motebehovStatusArbeidstaker()
            .assertMotebehovStatus(true, MotebehovSkjemaType.MELD_BEHOV, motebehovSvar)
    }

    @Test
    fun `get MotebehovStatus With Today Inside Oppfolgingstilfelle Before SvarBehov StartDate`() {
        oppfolgingstilfelleDAO.create(
            generateOversikthendelsetilfelle.copy(
                fom = LocalDate.now().minusDays(DAYS_START_SVAR_BEHOV).plusDays(1),
                tom = LocalDate.now().plusDays(1)
            )
        )

        motebehovArbeidstakerController.motebehovStatusArbeidstaker()
            .assertMotebehovStatus(true, MotebehovSkjemaType.MELD_BEHOV, null)
    }

    @Test
    fun `get MotebehovStatus With Today Inside Oppfolgingstilfelle After SvarBehov EndDate`() {
        oppfolgingstilfelleDAO.create(
            generateOversikthendelsetilfelle.copy(
                fom = LocalDate.now().minusDays(DAYS_END_SVAR_BEHOV),
                tom = LocalDate.now().plusDays(1)
            )
        )

        motebehovArbeidstakerController.motebehovStatusArbeidstaker()
            .assertMotebehovStatus(true, MotebehovSkjemaType.MELD_BEHOV, null)
    }

    @Test
    fun `get MotebehovStatus With No Motebehov And Mote Inside SvarBehov Upper Limit`() {
        createKandidatInDB()

        val kOppfolgingstilfelle = generateOversikthendelsetilfelle.copy(
            fom = LocalDate.now().minusDays(DAYS_END_SVAR_BEHOV).plusDays(1),
            tom = LocalDate.now().plusDays(1)
        )

        oppfolgingstilfelleDAO.create(kOppfolgingstilfelle)

        motebehovArbeidstakerController.motebehovStatusArbeidstaker()
            .assertMotebehovStatus(true, MotebehovSkjemaType.SVAR_BEHOV, null)
    }

    @Test
    fun `get MotebehovStatus with SvarBehov and Mote created`() {
        createKandidatInDB()

        val kOppfolgingstilfelle = generateOversikthendelsetilfelle.copy(
            fom = LocalDate.now().minusDays(DAYS_END_SVAR_BEHOV).plusDays(1),
            tom = LocalDate.now().plusDays(1)
        )
        oppfolgingstilfelleDAO.create(kOppfolgingstilfelle)

        val motebehovSvar = motebehovGenerator.lagMotebehovSvar(true)

        submitMotebehovAndSendOversikthendelse(motebehovSvar)

        mockRestServiceServer.reset()

        motebehovArbeidstakerController.motebehovStatusArbeidstaker()
            .assertMotebehovStatus(true, MotebehovSkjemaType.SVAR_BEHOV, motebehovSvar)
    }

    @Test
    fun `get MotebehovStatus with no Motebehov and no Mote inside SvarBehov lower limit`() {
        createKandidatInDB()

        val kOppfolgingstilfelle = generateOversikthendelsetilfelle.copy(
            fom = LocalDate.now().minusDays(DAYS_START_SVAR_BEHOV),
            tom = LocalDate.now().plusDays(1)
        )

        oppfolgingstilfelleDAO.create(kOppfolgingstilfelle)

        motebehovArbeidstakerController.motebehovStatusArbeidstaker()
            .assertMotebehovStatus(true, MotebehovSkjemaType.SVAR_BEHOV, null)
    }

    @Test
    fun `get MotebehovStatus with no Motebehov and no Mote`() {
        createKandidatInDB()

        val kOppfolgingstilfelle = generateOversikthendelsetilfelle
        oppfolgingstilfelleDAO.create(kOppfolgingstilfelle)

        motebehovArbeidstakerController.motebehovStatusArbeidstaker()
            .assertMotebehovStatus(true, MotebehovSkjemaType.SVAR_BEHOV, null)
    }

    @Test
    fun `get MotebehovStatus and sendOversikthendelse with Motebehov harBehov=true`() {
        createKandidatInDB()

        oppfolgingstilfelleDAO.create(
            generateOversikthendelsetilfelle.copy(
                fnr = ARBEIDSTAKER_FNR,
                virksomhetsnummer = VIRKSOMHETSNUMMER
            )
        )
        lagreOgHentMotebehovOgSendOversikthendelse(harBehov = true)
    }

    @Test
    fun `get MotebehovStatus and SendOversikthendelse with Motebehov harBehov=false`() {
        createKandidatInDB()

        oppfolgingstilfelleDAO.create(
            generateOversikthendelsetilfelle.copy(
                fnr = ARBEIDSTAKER_FNR,
                virksomhetsnummer = VIRKSOMHETSNUMMER
            )
        )
        lagreOgHentMotebehovOgSendOversikthendelse(harBehov = false)
    }

    @Test
    fun `submitMotebehov multiple active Oppfolgingstilfeller`() {
        oppfolgingstilfelleDAO.create(
            generateOversikthendelsetilfelle.copy(
                virksomhetsnummer = VIRKSOMHETSNUMMER,
                fom = LocalDate.now().minusDays(DAYS_END_SVAR_BEHOV).minusDays(1),
                tom = LocalDate.now()
            )
        )
        oppfolgingstilfelleDAO.create(
            generateOversikthendelsetilfelle.copy(
                virksomhetsnummer = UserConstants.VIRKSOMHETSNUMMER_2,
                fom = LocalDate.now().minusDays(2),
                tom = LocalDate.now().plusDays(1)
            )
        )

        mockAndExpectBehandlendeEnhetRequest(
            azureTokenEndpoint,
            mockRestServiceWithProxyServer,
            behandlendeenhetUrl,
            ARBEIDSTAKER_FNR
        )
        mockAndExpectBehandlendeEnhetRequest(
            azureTokenEndpoint,
            mockRestServiceWithProxyServer,
            behandlendeenhetUrl,
            ARBEIDSTAKER_FNR
        )

        val motebehovSvar = motebehovGenerator.lagMotebehovSvar(true)
        motebehovArbeidstakerController.submitMotebehovArbeidstaker(motebehovSvar)

        val motebehovList = motebehovDAO.hentMotebehovListeForOgOpprettetAvArbeidstaker(ARBEIDSTAKER_AKTORID)

        assertEquals(2, motebehovList.size)
        verify(exactly = 2) { oversikthendelseProducer.sendOversikthendelse(any(), any()) }
    }

    private fun submitMotebehovAndSendOversikthendelse(motebehovSvar: MotebehovSvar) {
        mockAndExpectBehandlendeEnhetRequest(
            azureTokenEndpoint,
            mockRestServiceWithProxyServer,
            behandlendeenhetUrl,
            ARBEIDSTAKER_FNR
        )

        motebehovArbeidstakerController.submitMotebehovArbeidstaker(motebehovSvar)
        if (motebehovSvar.harMotebehov) {
            verify { oversikthendelseProducer.sendOversikthendelse(any(), any()) }
        } else {
            verify(exactly = 0) { oversikthendelseProducer.sendOversikthendelse(any(), any()) }
        }
    }

    private fun lagreOgHentMotebehovOgSendOversikthendelse(harBehov: Boolean) {

        mockAndExpectBehandlendeEnhetRequest(
            azureTokenEndpoint,
            mockRestServiceWithProxyServer,
            behandlendeenhetUrl,
            ARBEIDSTAKER_FNR
        )

        val motebehovSvar = motebehovGenerator.lagMotebehovSvar(harBehov)

        motebehovArbeidstakerController.submitMotebehovArbeidstaker(motebehovSvar)

        if (!harBehov) {
            mockRestServiceServer.reset()
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
            verify { oversikthendelseProducer.sendOversikthendelse(any(), any()) }
        } else {
            verify(exactly = 0) { oversikthendelseProducer.sendOversikthendelse(any(), any()) }
        }
    }

    private fun mockBehandlendEnhetWithTilgangskontroll(fnr: String) {
        mockAndExpectBehandlendeEnhetRequestWithTilgangskontroll(
            azureTokenEndpoint,
            mockRestServiceWithProxyServer,
            mockRestServiceServer,
            behandlendeenhetUrl,
            tilgangskontrollUrl,
            fnr
        )
    }

    private fun createKandidatInDB() {
        dialogmotekandidatDAO.create(
            dialogmotekandidatExternalUUID = UUID.randomUUID().toString(),
            createdAt = LocalDateTime.now().minusDays(DAYS_START_SVAR_BEHOV),
            fnr = Fodselsnummer(ARBEIDSTAKER_FNR),
            kandidat = true,
            arsak = DialogmotekandidatEndringArsak.STOPPUNKT.name
        )
    }

    private fun resetMockRestServers() {
        mockRestServiceServer.reset()
        mockRestServiceWithProxyServer.reset()
    }

    private fun cleanDB() {
        motebehovDAO.nullstillMotebehov(ARBEIDSTAKER_AKTORID)
        oppfolgingstilfelleDAO.nullstillOppfolgingstilfeller(Fodselsnummer(ARBEIDSTAKER_FNR))
        dialogmotekandidatDAO.delete(Fodselsnummer(ARBEIDSTAKER_FNR))
    }
}
