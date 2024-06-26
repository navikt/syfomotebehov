package no.nav.syfo.motebehov.api

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import no.nav.syfo.LocalApplication
import no.nav.syfo.consumer.azuread.v2.AzureAdV2TokenConsumer
import no.nav.syfo.consumer.pdl.PdlConsumer
import no.nav.syfo.dialogmotekandidat.database.DialogmotekandidatDAO
import no.nav.syfo.dialogmotekandidat.database.DialogmotekandidatEndringArsak
import no.nav.syfo.motebehov.MotebehovSvar
import no.nav.syfo.motebehov.api.internad.v3.MotebehovVeilederADControllerV3
import no.nav.syfo.motebehov.database.MotebehovDAO
import no.nav.syfo.motebehov.motebehovstatus.DAYS_END_SVAR_BEHOV
import no.nav.syfo.motebehov.motebehovstatus.DAYS_START_SVAR_BEHOV
import no.nav.syfo.motebehov.motebehovstatus.MotebehovSkjemaType
import no.nav.syfo.motebehov.motebehovstatus.MotebehovStatus
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
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
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

    @Value("\${istilgangskontroll.url}")
    private lateinit var tilgangskontrollUrl: String

    @Inject
    private lateinit var motebehovArbeidstakerController: MotebehovArbeidstakerControllerV3

    @Inject
    private lateinit var motebehovVeilederController: MotebehovVeilederADControllerV3

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

    @BeforeEach
    fun setUp() {
        every { pdlConsumer.person(ARBEIDSTAKER_FNR) } returns generatePdlHentPerson(null, null)
        every { pdlConsumer.aktorid(any()) } returns ARBEIDSTAKER_AKTORID
        every { pdlConsumer.fnr(any()) } returns ARBEIDSTAKER_FNR
        every { pdlConsumer.isKode6(ARBEIDSTAKER_FNR) } returns false

        mockRestServiceServer = MockRestServiceServer.bindTo(restTemplate).build()
        mockRestServiceServerAzureAD = MockRestServiceServer.bindTo(restTemplateAzureAD).build()
        tokenValidationUtil.logInAsDialogmoteUser(ARBEIDSTAKER_FNR)
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
        AzureAdV2TokenConsumer.Companion.clearCache()
        cleanDB()
    }

    @Test
    fun `get MotebehovStatus With No Oppfolgingstilfelle`() {
        motebehovArbeidstakerController.motebehovStatusArbeidstakerWithCodeSixUsers()
            .assertMotebehovStatus(false, null, null)
    }

    @Test
    fun `get MotebehovStatus With Today Outside OppfolgingstilfelleStart`() {
        dbCreateOppfolgingstilfelle(
            oppfolgingstilfelleDAO,
            generateOppfolgingstilfellePerson(
                start = LocalDate.now().plusDays(1),
                end = LocalDate.now().plusDays(10),
            ),
        )

        motebehovArbeidstakerController.motebehovStatusArbeidstakerWithCodeSixUsers()
            .assertMotebehovStatus(false, null, null)
    }

    @Test
    fun `get MotebehovStatus With Today Outside OppfolgingstilfelleEnd`() {
        dbCreateOppfolgingstilfelle(
            oppfolgingstilfelleDAO,
            generateOppfolgingstilfellePerson(
                start = LocalDate.now().minusDays(17),
                end = LocalDate.now().minusDays(16),
            ),
        )

        motebehovArbeidstakerController.motebehovStatusArbeidstakerWithCodeSixUsers()
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
            .assertMotebehovStatus(true, MotebehovSkjemaType.SVAR_BEHOV, null)
    }

    @Test
    fun `get MotebehovStatus With Today Inside Oppfolgingstilfelle Merged By 2 Oppfolgingstilfeller`() {
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
            .assertMotebehovStatus(true, MotebehovSkjemaType.MELD_BEHOV, null)
    }

    @Test
    fun `get MotebehovStatus With Today Inside Oppfolgingstilfelle Day1`() {
        dbCreateOppfolgingstilfelle(
            oppfolgingstilfelleDAO,
            generateOppfolgingstilfellePerson(
                start = LocalDate.now(),
                end = LocalDate.now(),
            ),
        )
        motebehovArbeidstakerController.motebehovStatusArbeidstakerWithCodeSixUsers()
            .assertMotebehovStatus(true, MotebehovSkjemaType.MELD_BEHOV, null)
    }

    @Test
    fun `get MotebehovStatus With Today Inside Oppfolgingstilfelle LastDay`() {
        dbCreateOppfolgingstilfelle(
            oppfolgingstilfelleDAO,
            generateOppfolgingstilfellePerson(
                start = LocalDate.now().minusDays(DAYS_END_SVAR_BEHOV).minusDays(1),
                end = LocalDate.now(),
            ),
        )

        motebehovArbeidstakerController.motebehovStatusArbeidstakerWithCodeSixUsers()
            .assertMotebehovStatus(true, MotebehovSkjemaType.MELD_BEHOV, null)
    }

    @Test
    fun `get MotebehovStatus With Today Inside Oppfolgingstilfelle`() {
        dbCreateOppfolgingstilfelle(
            oppfolgingstilfelleDAO,
            generateOppfolgingstilfellePerson(
                start = LocalDate.now(),
                end = LocalDate.now().plusDays(1),
            ),
        )

        motebehovArbeidstakerController.motebehovStatusArbeidstakerWithCodeSixUsers()
            .assertMotebehovStatus(true, MotebehovSkjemaType.MELD_BEHOV, null)
    }

    @Test
    fun `get MotebehovStatus With Today Inside Oppfolgingstilfelle, MeldBehov Submitted And Behandlet`() {
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

        tokenValidationUtil.logInAsDialogmoteUser(ARBEIDSTAKER_FNR)
        motebehovArbeidstakerController.motebehovStatusArbeidstakerWithCodeSixUsers()
            .assertMotebehovStatus(true, MotebehovSkjemaType.MELD_BEHOV, null)
    }

    @Test
    fun `get MotebehovStatus With Today Inside Oppfolgingstilfelle MeldBehov, Moteplanlegger Active, MeldBehov Submitted`() {
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

        motebehovArbeidstakerController.motebehovStatusArbeidstakerWithCodeSixUsers()
            .assertMotebehovStatus(true, MotebehovSkjemaType.MELD_BEHOV, motebehovSvar)
    }

    @Test
    fun `get MotebehovStatus With Today Inside Oppfolgingstilfelle Before SvarBehov StartDate`() {
        dbCreateOppfolgingstilfelle(
            oppfolgingstilfelleDAO,
            generateOppfolgingstilfellePerson(
                start = LocalDate.now().minusDays(DAYS_START_SVAR_BEHOV).plusDays(1),
                end = LocalDate.now().plusDays(1),
            ),
        )

        motebehovArbeidstakerController.motebehovStatusArbeidstakerWithCodeSixUsers()
            .assertMotebehovStatus(true, MotebehovSkjemaType.MELD_BEHOV, null)
    }

    @Test
    fun `get MotebehovStatus With Today Inside Oppfolgingstilfelle After SvarBehov EndDate`() {
        dbCreateOppfolgingstilfelle(
            oppfolgingstilfelleDAO,
            generateOppfolgingstilfellePerson(
                start = LocalDate.now().minusDays(DAYS_END_SVAR_BEHOV),
                end = LocalDate.now().plusDays(1),
            ),
        )

        motebehovArbeidstakerController.motebehovStatusArbeidstakerWithCodeSixUsers()
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
            ),
        )

        motebehovArbeidstakerController.motebehovStatusArbeidstakerWithCodeSixUsers()
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
            ),
        )

        val motebehovSvar = motebehovGenerator.lagMotebehovSvar(true)

        submitMotebehovAndSendOversikthendelse(motebehovSvar)
        verify { esyfovarselService.ferdigstillSvarMotebehovForArbeidstaker(ARBEIDSTAKER_FNR) }
        verify(exactly = 0) { esyfovarselService.ferdigstillSvarMotebehovForArbeidsgiver(any(), any(), any()) }

        mockRestServiceServer.reset()

        motebehovArbeidstakerController.motebehovStatusArbeidstakerWithCodeSixUsers()
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
            ),
        )

        motebehovArbeidstakerController.motebehovStatusArbeidstakerWithCodeSixUsers()
            .assertMotebehovStatus(true, MotebehovSkjemaType.SVAR_BEHOV, null)
    }

    @Test
    fun `get MotebehovStatus with no Motebehov and no Mote`() {
        createKandidatInDB()

        dbCreateOppfolgingstilfelle(
            oppfolgingstilfelleDAO,
            generateOppfolgingstilfellePerson(),
        )

        motebehovArbeidstakerController.motebehovStatusArbeidstakerWithCodeSixUsers()
            .assertMotebehovStatus(true, MotebehovSkjemaType.SVAR_BEHOV, null)
    }

    @Test
    fun `get MotebehovStatus and sendOversikthendelse with Motebehov harBehov=true`() {
        createKandidatInDB()

        dbCreateOppfolgingstilfelle(
            oppfolgingstilfelleDAO,
            generateOppfolgingstilfellePerson(virksomhetsnummerList = listOf(VIRKSOMHETSNUMMER)).copy(
                personIdentNumber = ARBEIDSTAKER_FNR,
            ),
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
            ),
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

        val motebehovSvar = motebehovGenerator.lagMotebehovSvar(true)
        motebehovArbeidstakerController.submitMotebehovArbeidstaker(motebehovSvar)

        val motebehovList = motebehovDAO.hentMotebehovListeForOgOpprettetAvArbeidstaker(ARBEIDSTAKER_AKTORID)

        assertEquals(2, motebehovList.size)
        verify(exactly = 2) { personoppgavehendelseProducer.sendPersonoppgavehendelse(any(), any()) }
    }

    private fun submitMotebehovAndSendOversikthendelse(motebehovSvar: MotebehovSvar) {
        mockAndExpectBehandlendeEnhetRequest(
            azureTokenEndpoint,
            mockRestServiceServerAzureAD,
            mockRestServiceServer,
            behandlendeenhetUrl,
            ARBEIDSTAKER_FNR,
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
            mockRestServiceServerAzureAD,
            mockRestServiceServer,
            behandlendeenhetUrl,
            ARBEIDSTAKER_FNR,
        )

        val motebehovSvar = motebehovGenerator.lagMotebehovSvar(harBehov)

        motebehovArbeidstakerController.submitMotebehovArbeidstaker(motebehovSvar)

        if (!harBehov) {
            mockRestServiceServer.reset()
        }

        val motebehovStatus: MotebehovStatus = motebehovArbeidstakerController.motebehovStatusArbeidstakerWithCodeSixUsers()

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
        verify { esyfovarselService.ferdigstillSvarMotebehovForArbeidstaker(motebehov.arbeidstakerFnr) }
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
