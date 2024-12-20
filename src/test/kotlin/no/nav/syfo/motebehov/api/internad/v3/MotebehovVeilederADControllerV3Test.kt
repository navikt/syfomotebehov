package no.nav.syfo.motebehov.api.internad.v3

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import no.nav.syfo.LocalApplication
import no.nav.syfo.consumer.azuread.v2.AzureAdV2TokenConsumer
import no.nav.syfo.consumer.brukertilgang.BrukertilgangConsumer
import no.nav.syfo.consumer.pdl.PdlConsumer
import no.nav.syfo.motebehov.MotebehovSvarSubmissionOldDTO
import no.nav.syfo.motebehov.NyttMotebehovArbeidsgiverWithOldSvarSubmissionDTO
import no.nav.syfo.motebehov.api.MotebehovArbeidsgiverControllerV3
import no.nav.syfo.motebehov.api.MotebehovArbeidstakerControllerV3
import no.nav.syfo.motebehov.api.dbCreateOppfolgingstilfelle
import no.nav.syfo.motebehov.api.internad.dto.MotebehovVeilederDTO
import no.nav.syfo.motebehov.database.MotebehovDAO
import no.nav.syfo.motebehov.historikk.Historikk
import no.nav.syfo.motebehov.historikk.HistorikkService
import no.nav.syfo.oppfolgingstilfelle.database.OppfolgingstilfelleDAO
import no.nav.syfo.personoppgavehendelse.PersonoppgavehendelseProducer
import no.nav.syfo.testhelper.UserConstants.ARBEIDSTAKER_AKTORID
import no.nav.syfo.testhelper.UserConstants.ARBEIDSTAKER_FNR
import no.nav.syfo.testhelper.UserConstants.LEDER_AKTORID
import no.nav.syfo.testhelper.UserConstants.LEDER_FNR
import no.nav.syfo.testhelper.UserConstants.PERSON_FULL_NAME
import no.nav.syfo.testhelper.UserConstants.VEILEDER_2_ID
import no.nav.syfo.testhelper.UserConstants.VEILEDER_ID
import no.nav.syfo.testhelper.UserConstants.VIRKSOMHETSNUMMER
import no.nav.syfo.testhelper.clearCache
import no.nav.syfo.testhelper.generator.generateOppfolgingstilfellePerson
import no.nav.syfo.testhelper.generator.generatePdlHentPerson
import no.nav.syfo.testhelper.mockAndExpectBehandlendeEnhetRequest
import no.nav.syfo.testhelper.mockSvarFraIstilgangskontrollTilgangTilBruker
import no.nav.syfo.util.TokenValidationUtil
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cache.CacheManager
import org.springframework.http.HttpStatus
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.web.client.RestTemplate
import java.time.LocalDateTime
import java.util.function.Consumer
import javax.inject.Inject

@ExtendWith(SpringExtension::class)
@SpringBootTest(classes = [LocalApplication::class])
@DirtiesContext
class MotebehovVeilederADControllerV3Test {

    @Value("\${azure.openid.config.token.endpoint}")
    private lateinit var azureTokenEndpoint: String

    @Value("\${istilgangskontroll.url}")
    private lateinit var tilgangskontrollUrl: String

    @Value("\${syfobehandlendeenhet.url}")
    private lateinit var behandlendeenhetUrl: String

    @Inject
    private lateinit var motebehovArbeidstakerControllerV3: MotebehovArbeidstakerControllerV3

    @Inject
    private lateinit var motebehovArbeidsgiverControllerV3: MotebehovArbeidsgiverControllerV3

    @Inject
    private lateinit var oppfolgingstilfelleDAO: OppfolgingstilfelleDAO

    @Inject
    private lateinit var motebehovVeilederController: MotebehovVeilederADControllerV3

    @Inject
    private lateinit var motebehovDAO: MotebehovDAO

    @Inject
    private lateinit var cacheManager: CacheManager

    @Autowired
    @Qualifier("AzureAD")
    private lateinit var restTemplateAzureAD: RestTemplate

    @Inject
    private lateinit var restTemplate: RestTemplate

    @Inject
    private lateinit var tokenValidationUtil: TokenValidationUtil

    @MockkBean
    private lateinit var brukertilgangConsumer: BrukertilgangConsumer

    @MockkBean(relaxed = true)
    private lateinit var pdlConsumer: PdlConsumer

    @MockkBean(relaxed = true)
    private lateinit var personoppgavehendelseProducer: PersonoppgavehendelseProducer

    private lateinit var mockRestServiceServerAzureAD: MockRestServiceServer
    private lateinit var mockRestServiceServer: MockRestServiceServer

    @BeforeEach
    fun setUp() {
        cleanDB()

        mockRestServiceServer = MockRestServiceServer.bindTo(restTemplate).build()
        mockRestServiceServerAzureAD = MockRestServiceServer.bindTo(restTemplateAzureAD).build()

        every { personoppgavehendelseProducer.sendPersonoppgavehendelse(any(), any()) } returns Unit
        every { brukertilgangConsumer.hasAccessToAnsatt(ARBEIDSTAKER_FNR) } returns true

        every { pdlConsumer.aktorid(ARBEIDSTAKER_FNR) } returns ARBEIDSTAKER_AKTORID
        every { pdlConsumer.aktorid(LEDER_FNR) } returns LEDER_AKTORID
        every { pdlConsumer.fnr(ARBEIDSTAKER_AKTORID) } returns ARBEIDSTAKER_FNR
        every { pdlConsumer.fnr(LEDER_AKTORID) } returns LEDER_FNR
        every { pdlConsumer.person(ARBEIDSTAKER_FNR) } returns generatePdlHentPerson(null, null)
        every { pdlConsumer.person(LEDER_FNR) } returns generatePdlHentPerson(null, null)

        createOppfolgingstilfelle()
    }

    @AfterEach
    fun tearDown() {
        // Verify all expectations met
        mockRestServiceServer.verify()
        mockRestServiceServerAzureAD.verify()
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
    fun `arbeidsgiver lagrer Motebehov og Veileder henter Motebehov`() {
        // Arbeidsgiver lagrer nytt motebehov
        mockBehandlendEnhet(ARBEIDSTAKER_FNR)
        val nyttMotebehov = arbeidsgiverLoggerInnOgLagrerMotebehov()

        // Veileder henter møtebehov
        resetMockRestServers()
        mockSvarFraIstilgangskontroll(ARBEIDSTAKER_FNR, HttpStatus.OK)
        val motebehovListe = loggInnOgKallHentMotebehovListe(ARBEIDSTAKER_FNR, VEILEDER_ID)
        assertThat(motebehovListe).size().isOne
        val motebehov = motebehovListe[0]
        assertThat(motebehov.opprettetAv).isEqualTo(LEDER_AKTORID)
        assertThat(motebehov.arbeidstakerFnr).isEqualTo(ARBEIDSTAKER_FNR)
        assertThat(motebehov.virksomhetsnummer).isEqualTo(VIRKSOMHETSNUMMER)
        assertThat(motebehov.motebehovSvar).usingRecursiveComparison().isEqualTo(nyttMotebehov.motebehovSvar)
    }

    @Test
    fun `arbeidstaker lagrer Motebehov og Veileder henter Motebehov`() {
        // Arbeidstaker lagrer nytt motebehov
        mockBehandlendEnhet(ARBEIDSTAKER_FNR)
        val motebehovSvar = sykmeldtLoggerInnOgLagrerMotebehov(true)

        // Veileder henter møtebehov
        resetMockRestServers()
        mockSvarFraIstilgangskontroll(ARBEIDSTAKER_FNR, HttpStatus.OK)
        val motebehovListe = loggInnOgKallHentMotebehovListe(ARBEIDSTAKER_FNR, VEILEDER_ID)
        assertThat(motebehovListe).size().isOne
        val motebehov = motebehovListe[0]
        assertThat(motebehov.opprettetAv).isEqualTo(ARBEIDSTAKER_AKTORID)
        assertThat(motebehov.arbeidstakerFnr).isEqualTo(ARBEIDSTAKER_FNR)
        assertThat(motebehov.virksomhetsnummer).isEqualTo(VIRKSOMHETSNUMMER)
        assertThat(motebehov.motebehovSvar).usingRecursiveComparison().isEqualTo(motebehovSvar)
    }

    @Test
    fun `hent Historikk`() {
        // Arbeidsgiver lagrer motebehov
        mockBehandlendEnhet(ARBEIDSTAKER_FNR)
        arbeidsgiverLoggerInnOgLagrerMotebehov()

        // Veileder henter motebehovliste
        resetMockRestServers()
        mockSvarFraIstilgangskontroll(ARBEIDSTAKER_FNR, HttpStatus.OK)
        val motebehovListe = loggInnOgKallHentMotebehovListe(ARBEIDSTAKER_FNR, VEILEDER_ID)
        val motebehov = motebehovListe[0]
        resetMockRestServers()

        // Veileder behandler motebehov
        mockSvarFraIstilgangskontroll(ARBEIDSTAKER_FNR, HttpStatus.OK)
        loggInnOgKallBehandleMotebehov(ARBEIDSTAKER_FNR, VEILEDER_ID)

        // Veileder leser motebehov historikk
        resetMockRestServers()
        mockSvarFraIstilgangskontroll(ARBEIDSTAKER_FNR, HttpStatus.OK)
        val historikkListe = loggInnOgKallHentMotebehovHistorikk(ARBEIDSTAKER_FNR, VEILEDER_ID)
        assertThat(historikkListe).size().isEqualTo(2)
        val (opprettetAv, tekst, tidspunkt) = historikkListe[0]
        assertThat(opprettetAv).isEqualTo(LEDER_AKTORID)
        assertThat(tekst).isEqualTo(PERSON_FULL_NAME + HistorikkService.HAR_SVART_PAA_MOTEBEHOV)
        assertThat(tidspunkt).isEqualTo(motebehov.opprettetDato)
        val (_, tekst1, tidspunkt1) = historikkListe[1]
        assertThat(tekst1).isEqualTo(HistorikkService.MOTEBEHOVET_BLE_LEST_AV + VEILEDER_ID)
        val today = LocalDateTime.now()
        assertThat(tidspunkt1.isEqual(today))
    }

    @Test
    fun `hent ubehandlede Motebehov`() {
        mockBehandlendEnhet(ARBEIDSTAKER_FNR)
        sykmeldtLoggerInnOgLagrerMotebehov(true)
        resetMockRestServers()
        arbeidsgiverLoggerInnOgLagrerMotebehov()
        resetMockRestServers()
        mockSvarFraIstilgangskontroll(ARBEIDSTAKER_FNR, HttpStatus.OK)
        tokenValidationUtil.logInAsNavCounselor(VEILEDER_ID)
        val motebehovListe = loggInnOgKallHentMotebehovListe(ARBEIDSTAKER_FNR, VEILEDER_ID)

        motebehovListe.forEach(
            Consumer { motebehovVeilederDTO ->
                assertThat(motebehovVeilederDTO.behandletTidspunkt).isNull()
                assertThat(motebehovVeilederDTO.behandletVeilederIdent).isNull()
            },
        )
    }

    @Test
    fun `behandle kun motebehov med Motebehov`() {
        // AT og AG lagrer møtebehov
        mockBehandlendEnhet(ARBEIDSTAKER_FNR)
        sykmeldtLoggerInnOgLagrerMotebehov(false)
        resetMockRestServers()
        arbeidsgiverLoggerInnOgLagrerMotebehov()

        // Veileder behandler møtebehov med behov satt til 'true'
        resetMockRestServers()
        mockSvarFraIstilgangskontroll(ARBEIDSTAKER_FNR, HttpStatus.OK)
        loggInnOgKallBehandleMotebehov(ARBEIDSTAKER_FNR, VEILEDER_ID)
        resetMockRestServers()
        mockSvarFraIstilgangskontroll(ARBEIDSTAKER_FNR, HttpStatus.OK)
        val motebehovListe = loggInnOgKallHentMotebehovListe(ARBEIDSTAKER_FNR, VEILEDER_ID)
        assertThat(motebehovListe[0].behandletTidspunkt).isNull()
        assertThat(motebehovListe[0].behandletVeilederIdent).isEqualTo(null)
        assertNotNull(motebehovListe[1].behandletTidspunkt)
        assertThat(motebehovListe[1].behandletVeilederIdent).isEqualTo(VEILEDER_ID)
        verify(exactly = 2) { personoppgavehendelseProducer.sendPersonoppgavehendelse(any(), any()) }
    }

    @Test
    fun `behandle Motebehov og ulik Veileder behandler`() {
        mockBehandlendEnhet(ARBEIDSTAKER_FNR)
        sykmeldtLoggerInnOgLagrerMotebehov(true)
        behandleMotebehov(ARBEIDSTAKER_AKTORID, VEILEDER_ID)
        resetMockRestServers()
        arbeidsgiverLoggerInnOgLagrerMotebehov()
        resetMockRestServers()
        mockSvarFraIstilgangskontroll(ARBEIDSTAKER_FNR, HttpStatus.OK)
        loggInnOgKallBehandleMotebehov(ARBEIDSTAKER_FNR, VEILEDER_2_ID)
        resetMockRestServers()
        mockSvarFraIstilgangskontroll(ARBEIDSTAKER_FNR, HttpStatus.OK)
        val motebehovListe1 = loggInnOgKallHentMotebehovListe(ARBEIDSTAKER_FNR, VEILEDER_ID)
        assertNotNull(motebehovListe1[0].behandletTidspunkt)
        assertThat(motebehovListe1[0].behandletVeilederIdent).isEqualTo(VEILEDER_ID)
        assertNotNull(motebehovListe1[1].behandletTidspunkt)
        assertThat(motebehovListe1[1].behandletVeilederIdent).isEqualTo(VEILEDER_2_ID)
        verify(exactly = 3) { personoppgavehendelseProducer.sendPersonoppgavehendelse(any(), any()) }
    }

    @Test
    fun `behandle ikkeeksisterende Motebehov`() {
        mockBehandlendEnhet(ARBEIDSTAKER_FNR)
        sykmeldtLoggerInnOgLagrerMotebehov(true)
        behandleMotebehov(ARBEIDSTAKER_AKTORID, VEILEDER_ID)
        resetMockRestServers()
        mockSvarFraIstilgangskontroll(ARBEIDSTAKER_FNR, HttpStatus.OK)

        assertThrows<RuntimeException> { loggInnOgKallBehandleMotebehov(ARBEIDSTAKER_FNR, VEILEDER_2_ID) }
    }

    private fun arbeidsgiverLoggerInnOgLagrerMotebehov(): NyttMotebehovArbeidsgiverWithOldSvarSubmissionDTO {
        val motebehovSvar = MotebehovSvarSubmissionOldDTO(
            harMotebehov = true,
            forklaring = "",
        )
        val nyttMotebehov = NyttMotebehovArbeidsgiverWithOldSvarSubmissionDTO(
            arbeidstakerFnr = ARBEIDSTAKER_FNR,
            virksomhetsnummer = VIRKSOMHETSNUMMER,
            motebehovSvar = motebehovSvar,
        )
        tokenValidationUtil.logInAsDialogmoteUser(LEDER_FNR)
        motebehovArbeidsgiverControllerV3.lagreMotebehovArbeidsgiver(nyttMotebehov)
        return nyttMotebehov
    }

    private fun sykmeldtLoggerInnOgLagrerMotebehov(
        harBehov: Boolean,
    ): MotebehovSvarSubmissionOldDTO {
        val motebehovSvar = MotebehovSvarSubmissionOldDTO(
            harMotebehov = harBehov,
            forklaring = "",
        )
        tokenValidationUtil.logInAsDialogmoteUser(ARBEIDSTAKER_FNR)
        motebehovArbeidstakerControllerV3.submitMotebehovArbeidstaker(motebehovSvar)
        return motebehovSvar
    }

    private fun behandleMotebehov(aktoerId: String, veileder: String) {
        val ubehandledeMotebehov = motebehovDAO.hentUbehandledeMotebehov(aktoerId)
        ubehandledeMotebehov.forEach {
            motebehovDAO.oppdaterUbehandledeMotebehovTilBehandlet(it.uuid, veileder)
        }
    }

    private fun loggInnOgKallBehandleMotebehov(fnr: String, veileder: String) {
        tokenValidationUtil.logInAsNavCounselor(veileder)
        motebehovVeilederController.behandleMotebehov(fnr)
    }

    private fun loggInnOgKallHentMotebehovListe(fnr: String, veileder: String): List<MotebehovVeilederDTO> {
        tokenValidationUtil.logInAsNavCounselor(veileder)
        return motebehovVeilederController.hentMotebehovListe(fnr)
    }

    private fun loggInnOgKallHentMotebehovHistorikk(fnr: String, veileder: String): List<Historikk> {
        tokenValidationUtil.logInAsNavCounselor(veileder)
        return motebehovVeilederController.hentMotebehovHistorikk(fnr)
    }

    private fun mockSvarFraIstilgangskontroll(
        fnr: String,
        status: HttpStatus,
    ) {
        mockSvarFraIstilgangskontrollTilgangTilBruker(
            azureTokenEndpoint = azureTokenEndpoint,
            tilgangskontrollUrl = tilgangskontrollUrl,
            mockRestServiceServer = mockRestServiceServer,
            mockRestServiceServerAzureAD = mockRestServiceServerAzureAD,
            status = status,
            fnr = fnr,
        )
    }

    private fun mockBehandlendEnhet(fnr: String) {
        mockAndExpectBehandlendeEnhetRequest(
            azureTokenEndpoint,
            mockRestServiceServerAzureAD,
            mockRestServiceServer,
            behandlendeenhetUrl,
            fnr,
        )
    }

    private fun createOppfolgingstilfelle() {
        dbCreateOppfolgingstilfelle(
            oppfolgingstilfelleDAO,
            generateOppfolgingstilfellePerson(virksomhetsnummerList = listOf(VIRKSOMHETSNUMMER)).copy(
                personIdentNumber = ARBEIDSTAKER_FNR,
            ),
        )
    }

    private fun resetMockRestServers() {
        mockRestServiceServer.reset()
        mockRestServiceServerAzureAD.reset()
    }

    private fun cleanDB() {
        motebehovDAO.nullstillMotebehov(ARBEIDSTAKER_AKTORID)
        oppfolgingstilfelleDAO.nullstillOppfolgingstilfeller(ARBEIDSTAKER_FNR)
    }
}
