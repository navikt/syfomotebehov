package no.nav.syfo.motebehov.api

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.syfo.LocalApplication
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
import no.nav.syfo.personoppgavehendelse.PersonoppgavehendelseProducer
import no.nav.syfo.testhelper.*
import no.nav.syfo.testhelper.OidcTestHelper.loggInnBrukerTokenX
import no.nav.syfo.testhelper.OidcTestHelper.loggUtAlle
import no.nav.syfo.testhelper.UserConstants.ARBEIDSTAKER_AKTORID
import no.nav.syfo.testhelper.UserConstants.ARBEIDSTAKER_FNR
import no.nav.syfo.testhelper.UserConstants.VEILEDER_ID
import no.nav.syfo.testhelper.UserConstants.VIRKSOMHETSNUMMER
import no.nav.syfo.testhelper.UserConstants.VIRKSOMHETSNUMMER_2
import no.nav.syfo.testhelper.assertion.assertMotebehovStatus
import no.nav.syfo.testhelper.generator.MotebehovGenerator
import no.nav.syfo.testhelper.generator.generateOppfolgingstilfellePerson
import no.nav.syfo.testhelper.generator.generatePdlHentPerson
import no.nav.syfo.testhelper.generator.generateStsToken
import org.assertj.core.api.Assertions.assertThat
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
class MotebehovArbeidstakerControllerV3Test {
    @Value("\${azure.openid.config.token.endpoint}")
    private lateinit var azureTokenEndpoint: String

    @Value("\${syfobehandlendeenhet.url}")
    private lateinit var behandlendeenhetUrl: String

    @Value("\${tilgangskontrollapi.url}")
    private lateinit var tilgangskontrollUrl: String

    @Inject
    private lateinit var motebehovArbeidstakerController: MotebehovArbeidstakerV3Controller

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
    private lateinit var pdlConsumer: PdlConsumer

    @MockkBean
    private lateinit var stsConsumer: StsConsumer

    @MockkBean(relaxed = true)
    private lateinit var personoppgavehendelseProducer: PersonoppgavehendelseProducer

    private lateinit var mockRestServiceServer: MockRestServiceServer

    private val motebehovGenerator = MotebehovGenerator()

    private val stsToken = generateStsToken().access_token

    @Value("\${tokenx.idp}")
    private lateinit var tokenxIdp: String

    @Value("\${dialogmote.frontend.client.id}")
    private lateinit var dialogmoteClientId: String

    @BeforeEach
    fun setUp() {
        every { pdlConsumer.person(ARBEIDSTAKER_FNR) } returns generatePdlHentPerson(null, null)
        every { pdlConsumer.aktorid(any()) } returns ARBEIDSTAKER_AKTORID
        every { pdlConsumer.fnr(any()) } returns ARBEIDSTAKER_FNR
        every { stsConsumer.token() } returns stsToken
        every { pdlConsumer.isKode6(ARBEIDSTAKER_FNR) } returns false

        mockRestServiceServer = MockRestServiceServer.bindTo(restTemplate).build()
        mockRestServiceWithProxyServer = MockRestServiceServer.bindTo(restTemplateWithProxy).build()
        loggInnBrukerTokenX(contextHolder, ARBEIDSTAKER_FNR, dialogmoteClientId, tokenxIdp)
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
        dbCreateOppfolgingstilfelle(
            oppfolgingstilfelleDAO,
            generateOppfolgingstilfellePerson(
                start = LocalDate.now().plusDays(1),
                end = LocalDate.now().plusDays(10),
            )
        )

        motebehovArbeidstakerController.motebehovStatusArbeidstaker()
            .assertMotebehovStatus(false, null, null)
    }

    @Test
    fun `get MotebehovStatus With Today Outside OppfolgingstilfelleEnd`() {
        dbCreateOppfolgingstilfelle(
            oppfolgingstilfelleDAO,
            generateOppfolgingstilfellePerson(
                start = LocalDate.now().minusDays(10),
                end = LocalDate.now().minusDays(1),
            )
        )

        motebehovArbeidstakerController.motebehovStatusArbeidstaker()
            .assertMotebehovStatus(false, null, null)
    }

    @Test
    fun `get MotebehovStatus With Today Inside Oppfolgingstilfelle Merged By Active And Expired Oppfolgingstilfelle No Overlap`() {
        val activeOppfolgingstilfelleStartDate = LocalDate.now().minusDays(DAYS_START_SVAR_BEHOV).plusDays(1)

        dbCreateOppfolgingstilfelle(
            oppfolgingstilfelleDAO,
            generateOppfolgingstilfellePerson(
                start = activeOppfolgingstilfelleStartDate.minusDays(2),
                end = activeOppfolgingstilfelleStartDate.minusDays(1),
                virksomhetsnummerList = listOf(VIRKSOMHETSNUMMER)
            )
        )

        dbCreateOppfolgingstilfelle(
            oppfolgingstilfelleDAO,
            generateOppfolgingstilfellePerson(
                start = activeOppfolgingstilfelleStartDate,
                end = LocalDate.now().plusDays(1),
                virksomhetsnummerList = listOf(VIRKSOMHETSNUMMER_2)
            )
        )

        motebehovArbeidstakerController.motebehovStatusArbeidstaker()
            .assertMotebehovStatus(true, MotebehovSkjemaType.MELD_BEHOV, null)
    }

    @Test
    fun `getMotebehovStatus With Today Inside Oppfolgingstilfelle Merged By Active And Expired Oppfolgingstilfelle With Overlap`() {
        createKandidatInDB()

        val activeOppfolgingstilfelleStartDate = LocalDate.now().minusDays(DAYS_START_SVAR_BEHOV).plusDays(1)

        dbCreateOppfolgingstilfelle(
            oppfolgingstilfelleDAO,
            generateOppfolgingstilfellePerson(
                start = activeOppfolgingstilfelleStartDate.minusDays(2),
                end = activeOppfolgingstilfelleStartDate,
                virksomhetsnummerList = listOf(VIRKSOMHETSNUMMER)
            )
        )

        dbCreateOppfolgingstilfelle(
            oppfolgingstilfelleDAO,
            generateOppfolgingstilfellePerson(
                start = activeOppfolgingstilfelleStartDate,
                end = LocalDate.now().plusDays(1),
                virksomhetsnummerList = listOf(VIRKSOMHETSNUMMER_2)
            )
        )

        motebehovArbeidstakerController.motebehovStatusArbeidstaker()
            .assertMotebehovStatus(true, MotebehovSkjemaType.SVAR_BEHOV, null)
    }

    @Test
    fun `get MotebehovStatus With Today Inside Oppfolgingstilfelle Merged By 2 Oppfolgingstilfeller`() {
        dbCreateOppfolgingstilfelle(
            oppfolgingstilfelleDAO,
            generateOppfolgingstilfellePerson(
                start = LocalDate.now().minusDays(DAYS_END_SVAR_BEHOV).minusDays(1),
                end = LocalDate.now(),
                virksomhetsnummerList = listOf(VIRKSOMHETSNUMMER)
            )
        )

        dbCreateOppfolgingstilfelle(
            oppfolgingstilfelleDAO,
            generateOppfolgingstilfellePerson(
                start = LocalDate.now().minusDays(2),
                end = LocalDate.now().plusDays(1),
                virksomhetsnummerList = listOf(VIRKSOMHETSNUMMER_2)
            )
        )

        motebehovArbeidstakerController.motebehovStatusArbeidstaker()
            .assertMotebehovStatus(true, MotebehovSkjemaType.MELD_BEHOV, null)
    }

    @Test
    fun `get MotebehovStatus With Today Inside Oppfolgingstilfelle Day1`() {

        dbCreateOppfolgingstilfelle(
            oppfolgingstilfelleDAO,
            generateOppfolgingstilfellePerson(
                start = LocalDate.now(),
                end = LocalDate.now(),
            )
        )
        motebehovArbeidstakerController.motebehovStatusArbeidstaker()
            .assertMotebehovStatus(true, MotebehovSkjemaType.MELD_BEHOV, null)
    }

    @Test
    fun `get MotebehovStatus With Today Inside Oppfolgingstilfelle LastDay`() {
        dbCreateOppfolgingstilfelle(
            oppfolgingstilfelleDAO,
            generateOppfolgingstilfellePerson(
                start = LocalDate.now().minusDays(DAYS_END_SVAR_BEHOV).minusDays(1),
                end = LocalDate.now(),
            )
        )

        motebehovArbeidstakerController.motebehovStatusArbeidstaker()
            .assertMotebehovStatus(true, MotebehovSkjemaType.MELD_BEHOV, null)
    }

    @Test
    fun `get MotebehovStatus With Today Inside Oppfolgingstilfelle`() {
        dbCreateOppfolgingstilfelle(
            oppfolgingstilfelleDAO,
            generateOppfolgingstilfellePerson(
                start = LocalDate.now(),
                end = LocalDate.now().plusDays(1),
            )
        )

        motebehovArbeidstakerController.motebehovStatusArbeidstaker()
            .assertMotebehovStatus(true, MotebehovSkjemaType.MELD_BEHOV, null)
    }

    @Test
    fun `get MotebehovStatus With Today Inside Oppfolgingstilfelle, MeldBehov Submitted And Behandlet`() {
        dbCreateOppfolgingstilfelle(
            oppfolgingstilfelleDAO,
            generateOppfolgingstilfellePerson(
                start = LocalDate.now(),
                end = LocalDate.now().plusDays(1),
            )
        )

        val motebehovSvar = motebehovGenerator.lagMotebehovSvar(true)
        submitMotebehovAndSendOversikthendelse(motebehovSvar)

        resetMockRestServers()
        loggUtAlle(contextHolder)
        OidcTestHelper.loggInnVeilederADV2(contextHolder, VEILEDER_ID)
        mockBehandlendEnhetWithTilgangskontroll(ARBEIDSTAKER_FNR)
        motebehovVeilederController.behandleMotebehov(ARBEIDSTAKER_FNR)

        resetMockRestServers()
        loggInnBrukerTokenX(contextHolder, ARBEIDSTAKER_FNR, dialogmoteClientId, tokenxIdp)

        motebehovArbeidstakerController.motebehovStatusArbeidstaker()
            .assertMotebehovStatus(true, MotebehovSkjemaType.MELD_BEHOV, null)
    }

    @Test
    fun `get MotebehovStatus With Today Inside Oppfolgingstilfelle MeldBehov, Moteplanlegger Active, MeldBehov Submitted`() {
        dbCreateOppfolgingstilfelle(
            oppfolgingstilfelleDAO,
            generateOppfolgingstilfellePerson(
                start = LocalDate.now(),
                end = LocalDate.now().plusDays(1),
            )
        )

        val motebehovSvar = motebehovGenerator.lagMotebehovSvar(true)

        submitMotebehovAndSendOversikthendelse(motebehovSvar)

        mockRestServiceServer.reset()

        motebehovArbeidstakerController.motebehovStatusArbeidstaker()
            .assertMotebehovStatus(true, MotebehovSkjemaType.MELD_BEHOV, motebehovSvar)
    }

    @Test
    fun `get MotebehovStatus With Today Inside Oppfolgingstilfelle Before SvarBehov StartDate`() {
        dbCreateOppfolgingstilfelle(
            oppfolgingstilfelleDAO,
            generateOppfolgingstilfellePerson(
                start = LocalDate.now().minusDays(DAYS_START_SVAR_BEHOV).plusDays(1),
                end = LocalDate.now().plusDays(1),
            )
        )

        motebehovArbeidstakerController.motebehovStatusArbeidstaker()
            .assertMotebehovStatus(true, MotebehovSkjemaType.MELD_BEHOV, null)
    }

    @Test
    fun `get MotebehovStatus With Today Inside Oppfolgingstilfelle After SvarBehov EndDate`() {
        dbCreateOppfolgingstilfelle(
            oppfolgingstilfelleDAO,
            generateOppfolgingstilfellePerson(
                start = LocalDate.now().minusDays(DAYS_END_SVAR_BEHOV),
                end = LocalDate.now().plusDays(1),
            )
        )

        motebehovArbeidstakerController.motebehovStatusArbeidstaker()
            .assertMotebehovStatus(true, MotebehovSkjemaType.MELD_BEHOV, null)
    }

    @Test
    fun `get MotebehovStatus With No Motebehov And Mote Inside SvarBehov Upper Limit`() {
        createKandidatInDB()

        dbCreateOppfolgingstilfelle(
            oppfolgingstilfelleDAO,
            generateOppfolgingstilfellePerson(
                start = LocalDate.now().minusDays(DAYS_END_SVAR_BEHOV).plusDays(1),
                end = LocalDate.now().plusDays(1),
            )
        )

        motebehovArbeidstakerController.motebehovStatusArbeidstaker()
            .assertMotebehovStatus(true, MotebehovSkjemaType.SVAR_BEHOV, null)
    }

    @Test
    fun `get MotebehovStatus with SvarBehov and Mote created`() {
        createKandidatInDB()

        dbCreateOppfolgingstilfelle(
            oppfolgingstilfelleDAO,
            generateOppfolgingstilfellePerson(
                start = LocalDate.now().minusDays(DAYS_END_SVAR_BEHOV).plusDays(1),
                end = LocalDate.now().plusDays(1),
            )
        )

        val motebehovSvar = motebehovGenerator.lagMotebehovSvar(true)

        submitMotebehovAndSendOversikthendelse(motebehovSvar)

        mockRestServiceServer.reset()

        motebehovArbeidstakerController.motebehovStatusArbeidstaker()
            .assertMotebehovStatus(true, MotebehovSkjemaType.SVAR_BEHOV, motebehovSvar)
    }

    @Test
    fun `get MotebehovStatus with no Motebehov and no Mote inside SvarBehov lower limit`() {
        createKandidatInDB()

        dbCreateOppfolgingstilfelle(
            oppfolgingstilfelleDAO,
            generateOppfolgingstilfellePerson(
                start = LocalDate.now().minusDays(DAYS_START_SVAR_BEHOV),
                end = LocalDate.now().plusDays(1),
            )
        )

        motebehovArbeidstakerController.motebehovStatusArbeidstaker()
            .assertMotebehovStatus(true, MotebehovSkjemaType.SVAR_BEHOV, null)
    }

    @Test
    fun `get MotebehovStatus with no Motebehov and no Mote`() {
        createKandidatInDB()

        dbCreateOppfolgingstilfelle(
            oppfolgingstilfelleDAO,
            generateOppfolgingstilfellePerson()
        )

        motebehovArbeidstakerController.motebehovStatusArbeidstaker()
            .assertMotebehovStatus(true, MotebehovSkjemaType.SVAR_BEHOV, null)
    }

    @Test
    fun `get MotebehovStatus and sendOversikthendelse with Motebehov harBehov=true`() {
        createKandidatInDB()

        dbCreateOppfolgingstilfelle(
            oppfolgingstilfelleDAO,
            generateOppfolgingstilfellePerson(virksomhetsnummerList = listOf(VIRKSOMHETSNUMMER)).copy(
                personIdentNumber = ARBEIDSTAKER_FNR,
            )
        )

        lagreOgHentMotebehovOgSendOversikthendelse(harBehov = true)
    }

    @Test
    fun `get MotebehovStatus and SendOversikthendelse with Motebehov harBehov=false`() {
        createKandidatInDB()

        dbCreateOppfolgingstilfelle(
            oppfolgingstilfelleDAO,
            generateOppfolgingstilfellePerson(virksomhetsnummerList = listOf(VIRKSOMHETSNUMMER)).copy(
                personIdentNumber = ARBEIDSTAKER_FNR,
            )
        )

        lagreOgHentMotebehovOgSendOversikthendelse(harBehov = false)
    }

    @Test
    fun `submitMotebehov multiple active Oppfolgingstilfeller`() {
        dbCreateOppfolgingstilfelle(
            oppfolgingstilfelleDAO,
            generateOppfolgingstilfellePerson(
                start = LocalDate.now().minusDays(DAYS_END_SVAR_BEHOV).minusDays(1),
                end = LocalDate.now(),
                virksomhetsnummerList = listOf(VIRKSOMHETSNUMMER)
            )
        )

        dbCreateOppfolgingstilfelle(
            oppfolgingstilfelleDAO,
            generateOppfolgingstilfellePerson(
                start = LocalDate.now().minusDays(2),
                end = LocalDate.now().plusDays(1),
                virksomhetsnummerList = listOf(VIRKSOMHETSNUMMER_2)
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
        verify(exactly = 2) { personoppgavehendelseProducer.sendPersonoppgavehendelse(any(), any()) }
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
            verify { personoppgavehendelseProducer.sendPersonoppgavehendelse(any(), any()) }
        } else {
            verify(exactly = 0) { personoppgavehendelseProducer.sendPersonoppgavehendelse(any(), any()) }
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
        assertThat(motebehov.opprettetAv).isEqualTo(ARBEIDSTAKER_AKTORID)
        assertThat(motebehov.arbeidstakerFnr).isEqualTo(ARBEIDSTAKER_FNR)
        assertThat(motebehov.virksomhetsnummer).isEqualTo(VIRKSOMHETSNUMMER)
        assertThat(motebehov.skjemaType).isEqualTo(motebehovStatus.skjemaType)
        assertThat(motebehov.motebehovSvar).usingRecursiveComparison().isEqualTo(motebehovSvar)
        if (harBehov) {
            verify { personoppgavehendelseProducer.sendPersonoppgavehendelse(any(), any()) }
        } else {
            verify(exactly = 0) { personoppgavehendelseProducer.sendPersonoppgavehendelse(any(), any()) }
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
            fnr = ARBEIDSTAKER_FNR,
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
        oppfolgingstilfelleDAO.nullstillOppfolgingstilfeller(ARBEIDSTAKER_FNR)
        dialogmotekandidatDAO.delete(ARBEIDSTAKER_FNR)
    }
}
