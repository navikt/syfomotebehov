package no.nav.syfo.motebehov.api.internad.v2

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import java.text.ParseException
import java.time.LocalDateTime
import java.util.function.Consumer
import javax.inject.Inject
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.syfo.LocalApplication
import no.nav.syfo.consumer.azuread.v2.AzureAdV2TokenConsumer
import no.nav.syfo.consumer.brukertilgang.BrukertilgangConsumer
import no.nav.syfo.consumer.pdl.PdlConsumer
import no.nav.syfo.consumer.sts.StsConsumer
import no.nav.syfo.motebehov.MotebehovSvar
import no.nav.syfo.motebehov.NyttMotebehovArbeidsgiver
import no.nav.syfo.motebehov.api.MotebehovArbeidsgiverControllerV3
import no.nav.syfo.motebehov.api.MotebehovArbeidstakerControllerV3
import no.nav.syfo.motebehov.api.dbCreateOppfolgingstilfelle
import no.nav.syfo.motebehov.database.MotebehovDAO
import no.nav.syfo.motebehov.historikk.HistorikkService
import no.nav.syfo.oppfolgingstilfelle.database.OppfolgingstilfelleDAO
import no.nav.syfo.personoppgavehendelse.PersonoppgavehendelseProducer
import no.nav.syfo.testhelper.OidcTestHelper.loggInnBrukerTokenX
import no.nav.syfo.testhelper.OidcTestHelper.loggInnVeilederADV2
import no.nav.syfo.testhelper.OidcTestHelper.loggUtAlle
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
import no.nav.syfo.testhelper.generator.generateStsToken
import no.nav.syfo.testhelper.mockAndExpectBehandlendeEnhetRequest
import no.nav.syfo.testhelper.mockSvarFraSyfoTilgangskontrollV2TilgangTilBruker
import no.nav.syfo.varsel.esyfovarsel.EsyfovarselService
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cache.CacheManager
import org.springframework.http.HttpStatus
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.web.client.RestTemplate

@ExtendWith(SpringExtension::class)
@SpringBootTest(classes = [LocalApplication::class])
@DirtiesContext
class MotebehovVeilederADControllerV2Test {

    @Value("\${azure.openid.config.token.endpoint}")
    private lateinit var azureTokenEndpoint: String

    @Value("\${tilgangskontrollapi.url}")
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
    private lateinit var motebehovVeilederController: MotebehovVeilederADControllerV2

    @Inject
    private lateinit var contextHolder: TokenValidationContextHolder

    @Inject
    private lateinit var motebehovDAO: MotebehovDAO

    @Inject
    private lateinit var cacheManager: CacheManager

    @Inject
    private lateinit var restTemplate: RestTemplate

    @MockkBean(relaxed = true)
    private lateinit var esyfovarselService: EsyfovarselService

    @MockkBean
    private lateinit var brukertilgangConsumer: BrukertilgangConsumer

    @MockkBean(relaxed = true)
    private lateinit var pdlConsumer: PdlConsumer

    @MockkBean
    private lateinit var stsConsumer: StsConsumer

    @MockkBean(relaxed = true)
    private lateinit var personoppgavehendelseProducer: PersonoppgavehendelseProducer

    private lateinit var mockRestServiceServer: MockRestServiceServer

    @Inject
    @Qualifier("restTemplateWithProxy")
    private lateinit var restTemplateWithProxy: RestTemplate
    private lateinit var mockRestServiceWithProxyServer: MockRestServiceServer

    private val stsToken = generateStsToken().access_token

    @Value("\${tokenx.idp}")
    private lateinit var tokenxIdp: String

    @Value("\${dialogmote.frontend.client.id}")
    private lateinit var dialogmoteClientId: String

    @BeforeEach
    fun setUp() {
        cleanDB()

        mockRestServiceServer = MockRestServiceServer.bindTo(restTemplate).build()
        mockRestServiceWithProxyServer = MockRestServiceServer.bindTo(restTemplateWithProxy).build()

        every { personoppgavehendelseProducer.sendPersonoppgavehendelse(any(), any()) } returns Unit
        every { brukertilgangConsumer.hasAccessToAnsatt(ARBEIDSTAKER_FNR) } returns true

        every { pdlConsumer.aktorid(ARBEIDSTAKER_FNR) } returns ARBEIDSTAKER_AKTORID
        every { pdlConsumer.aktorid(LEDER_FNR) } returns LEDER_AKTORID
        every { pdlConsumer.fnr(ARBEIDSTAKER_AKTORID) } returns ARBEIDSTAKER_FNR
        every { pdlConsumer.fnr(LEDER_AKTORID) } returns LEDER_FNR
        every { pdlConsumer.person(ARBEIDSTAKER_FNR) } returns generatePdlHentPerson(null, null)
        every { pdlConsumer.person(LEDER_FNR) } returns generatePdlHentPerson(null, null)
        every { stsConsumer.token() } returns stsToken

        createOppfolgingstilfelle()
    }

    @AfterEach
    fun tearDown() {
        // Verify all expectations met
        mockRestServiceServer.verify()
        mockRestServiceWithProxyServer.verify()
        loggUtAlle(contextHolder)
        resetMockRestServers()
        cacheManager.cacheNames
            .forEach(
                Consumer { cacheName: String ->
                    val cache = cacheManager.getCache(cacheName)
                    cache?.clear()
                }
            )
        cleanDB()
        AzureAdV2TokenConsumer.Companion.clearCache()
    }

    @Test
    @Throws(ParseException::class)
    fun `arbeidsgiver lagrer Motebehov og Veileder henter Motebehov`() {
        mockBehandlendEnhet(ARBEIDSTAKER_FNR)
        val nyttMotebehov = arbeidsgiverLagrerMotebehov()

        // Veileder henter møtebehov
        resetMockRestServers()
        loggInnVeilederADV2(contextHolder, VEILEDER_ID)
        mockSvarFraSyfoTilgangskontroll(ARBEIDSTAKER_FNR, HttpStatus.OK)
        val motebehovListe = motebehovVeilederController.hentMotebehovListe(ARBEIDSTAKER_FNR)
        assertThat(motebehovListe).size().isOne
        val motebehov = motebehovListe[0]
        assertThat(motebehov.opprettetAv).isEqualTo(LEDER_AKTORID)
        assertThat(motebehov.arbeidstakerFnr).isEqualTo(ARBEIDSTAKER_FNR)
        assertThat(motebehov.virksomhetsnummer).isEqualTo(VIRKSOMHETSNUMMER)
        assertThat(motebehov.motebehovSvar).usingRecursiveComparison().isEqualTo(nyttMotebehov.motebehovSvar)
    }

    @Test
    @Throws(ParseException::class)
    fun `arbeidstaker lagrer Motebehov og Veileder henter Motebehov`() {
        mockBehandlendEnhet(ARBEIDSTAKER_FNR)
        val motebehovSvar = sykmeldtLagrerMotebehov(true)

        // Veileder henter møtebehov
        resetMockRestServers()
        loggInnVeilederADV2(contextHolder, VEILEDER_ID)
        mockSvarFraSyfoTilgangskontroll(ARBEIDSTAKER_FNR, HttpStatus.OK)
        val motebehovListe = motebehovVeilederController.hentMotebehovListe(ARBEIDSTAKER_FNR)
        assertThat(motebehovListe).size().isOne
        val motebehov = motebehovListe[0]
        assertThat(motebehov.opprettetAv).isEqualTo(ARBEIDSTAKER_AKTORID)
        assertThat(motebehov.arbeidstakerFnr).isEqualTo(ARBEIDSTAKER_FNR)
        assertThat(motebehov.virksomhetsnummer).isEqualTo(VIRKSOMHETSNUMMER)
        assertThat(motebehov.motebehovSvar).usingRecursiveComparison().isEqualTo(motebehovSvar)
    }

    @Test
    @Throws(Exception::class)
    fun `hent Historikk`() {
        mockBehandlendEnhet(ARBEIDSTAKER_FNR)
        arbeidsgiverLagrerMotebehov()
        resetMockRestServers()
        loggInnVeilederADV2(contextHolder, VEILEDER_ID)
        mockSvarFraSyfoTilgangskontroll(ARBEIDSTAKER_FNR, HttpStatus.OK)
        val motebehovListe = motebehovVeilederController.hentMotebehovListe(ARBEIDSTAKER_FNR)
        val motebehov = motebehovListe[0]
        resetMockRestServers()
        loggInnVeilederADV2(contextHolder, VEILEDER_ID)
        mockSvarFraSyfoTilgangskontroll(ARBEIDSTAKER_FNR, HttpStatus.OK)
        motebehovVeilederController.behandleMotebehov(ARBEIDSTAKER_FNR)
        resetMockRestServers()
        loggInnVeilederADV2(contextHolder, VEILEDER_ID)
        mockSvarFraSyfoTilgangskontroll(ARBEIDSTAKER_FNR, HttpStatus.OK)
        val historikkListe = motebehovVeilederController.hentMotebehovHistorikk(ARBEIDSTAKER_FNR)
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
    @Throws(ParseException::class)
    fun `hent ubehandlede Motebehov`() {
        mockBehandlendEnhet(ARBEIDSTAKER_FNR)
        sykmeldtLagrerMotebehov(true)
        resetMockRestServers()
        arbeidsgiverLagrerMotebehov()
        resetMockRestServers()
        loggInnVeilederADV2(contextHolder, VEILEDER_ID)
        mockSvarFraSyfoTilgangskontroll(ARBEIDSTAKER_FNR, HttpStatus.OK)
        val motebehovListe = motebehovVeilederController.hentMotebehovListe(ARBEIDSTAKER_FNR)

        motebehovListe.forEach(
            Consumer { motebehovVeilederDTO ->
                assertThat(motebehovVeilederDTO.behandletTidspunkt).isNull()
                assertThat(motebehovVeilederDTO.behandletVeilederIdent).isNull()
            }
        )
    }

    @Test
    @Throws(ParseException::class)
    fun `behandle kun motebehov med Motebehov`() {
        mockBehandlendEnhet(ARBEIDSTAKER_FNR)
        sykmeldtLagrerMotebehov(false)
        resetMockRestServers()
        arbeidsgiverLagrerMotebehov()
        resetMockRestServers()
        loggInnVeilederADV2(contextHolder, VEILEDER_ID)
        mockSvarFraSyfoTilgangskontroll(ARBEIDSTAKER_FNR, HttpStatus.OK)
        motebehovVeilederController.behandleMotebehov(ARBEIDSTAKER_FNR)
        resetMockRestServers()
        mockSvarFraSyfoTilgangskontroll(ARBEIDSTAKER_FNR, HttpStatus.OK)
        val motebehovListe = motebehovVeilederController.hentMotebehovListe(ARBEIDSTAKER_FNR)
        assertThat(motebehovListe[0].behandletTidspunkt).isNull()
        assertThat(motebehovListe[0].behandletVeilederIdent).isEqualTo(null)
        assertNotNull(motebehovListe[1].behandletTidspunkt)
        assertThat(motebehovListe[1].behandletVeilederIdent).isEqualTo(VEILEDER_ID)
        verify(exactly = 2) { personoppgavehendelseProducer.sendPersonoppgavehendelse(any(), any()) }
    }

    @Test
    @Throws(ParseException::class)
    fun `behandle Motebehov og ulik Veileder behandler`() {
        mockBehandlendEnhet(ARBEIDSTAKER_FNR)
        sykmeldtLagrerMotebehov(true)
        behandleMotebehov(ARBEIDSTAKER_AKTORID, VEILEDER_ID)
        resetMockRestServers()
        arbeidsgiverLagrerMotebehov()
        resetMockRestServers()
        loggInnVeilederADV2(contextHolder, VEILEDER_2_ID)
        mockSvarFraSyfoTilgangskontroll(ARBEIDSTAKER_FNR, HttpStatus.OK)
        motebehovVeilederController.behandleMotebehov(ARBEIDSTAKER_FNR)
        resetMockRestServers()
        mockSvarFraSyfoTilgangskontroll(ARBEIDSTAKER_FNR, HttpStatus.OK)
        val motebehovListe1 = motebehovVeilederController.hentMotebehovListe(ARBEIDSTAKER_FNR)
        assertNotNull(motebehovListe1[0].behandletTidspunkt)
        assertThat(motebehovListe1[0].behandletVeilederIdent).isEqualTo(VEILEDER_ID)
        assertNotNull(motebehovListe1[1].behandletTidspunkt)
        assertThat(motebehovListe1[1].behandletVeilederIdent).isEqualTo(VEILEDER_2_ID)
        verify(exactly = 3) { personoppgavehendelseProducer.sendPersonoppgavehendelse(any(), any()) }
    }

    @Test
    @Throws(ParseException::class)
    fun `behandle ikkeeksisterende Motebehov`() {
        mockBehandlendEnhet(ARBEIDSTAKER_FNR)
        sykmeldtLagrerMotebehov(true)
        behandleMotebehov(ARBEIDSTAKER_AKTORID, VEILEDER_ID)
        resetMockRestServers()
        loggInnVeilederADV2(contextHolder, VEILEDER_2_ID)
        mockSvarFraSyfoTilgangskontroll(ARBEIDSTAKER_FNR, HttpStatus.OK)

        assertThrows<RuntimeException> { motebehovVeilederController.behandleMotebehov(ARBEIDSTAKER_FNR) }
    }

    private fun arbeidsgiverLagrerMotebehov(): NyttMotebehovArbeidsgiver {
        loggInnBrukerTokenX(contextHolder, LEDER_FNR, dialogmoteClientId, tokenxIdp)

        val motebehovSvar = MotebehovSvar(
            harMotebehov = true,
            forklaring = ""
        )
        val nyttMotebehov = NyttMotebehovArbeidsgiver(
            arbeidstakerFnr = ARBEIDSTAKER_FNR,
            virksomhetsnummer = VIRKSOMHETSNUMMER,
            motebehovSvar = motebehovSvar
        )
        motebehovArbeidsgiverControllerV3.lagreMotebehovArbeidsgiver(nyttMotebehov)
        return nyttMotebehov
    }

    private fun sykmeldtLagrerMotebehov(
        harBehov: Boolean
    ): MotebehovSvar {
        loggInnBrukerTokenX(contextHolder, ARBEIDSTAKER_FNR, dialogmoteClientId, tokenxIdp)

        val motebehovSvar = MotebehovSvar(
            harMotebehov = harBehov,
            forklaring = ""
        )
        motebehovArbeidstakerControllerV3.submitMotebehovArbeidstaker(motebehovSvar)
        return motebehovSvar
    }

    private fun behandleMotebehov(aktoerId: String, veileder: String) {
        val ubehandledeMotebehov = motebehovDAO.hentUbehandledeMotebehov(aktoerId)
        ubehandledeMotebehov.forEach {
            motebehovDAO.oppdaterUbehandledeMotebehovTilBehandlet(it.uuid, veileder)
        }
    }

    private fun mockSvarFraSyfoTilgangskontroll(
        fnr: String,
        status: HttpStatus
    ) {
        mockSvarFraSyfoTilgangskontrollV2TilgangTilBruker(
            azureTokenEndpoint = azureTokenEndpoint,
            tilgangskontrollUrl = tilgangskontrollUrl,
            mockRestServiceServer = mockRestServiceServer,
            mockRestServiceWithProxyServer = mockRestServiceWithProxyServer,
            status = status,
            fnr = fnr
        )
    }

    private fun mockBehandlendEnhet(fnr: String) {
        mockAndExpectBehandlendeEnhetRequest(
            azureTokenEndpoint,
            mockRestServiceWithProxyServer,
            behandlendeenhetUrl,
            fnr
        )
    }

    private fun createOppfolgingstilfelle() {
        dbCreateOppfolgingstilfelle(
            oppfolgingstilfelleDAO,
            generateOppfolgingstilfellePerson(virksomhetsnummerList = listOf(VIRKSOMHETSNUMMER)).copy(
                personIdentNumber = ARBEIDSTAKER_FNR,
            )
        )
    }

    private fun resetMockRestServers() {
        mockRestServiceServer.reset()
        mockRestServiceWithProxyServer.reset()
    }

    private fun cleanDB() {
        motebehovDAO.nullstillMotebehov(ARBEIDSTAKER_AKTORID)
        oppfolgingstilfelleDAO.nullstillOppfolgingstilfeller(ARBEIDSTAKER_FNR)
    }
}
