package no.nav.syfo.motebehov.api.internad.v2

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.syfo.LocalApplication
import no.nav.syfo.consumer.azuread.v2.AzureAdV2TokenConsumer
import no.nav.syfo.consumer.brukertilgang.BrukertilgangConsumer
import no.nav.syfo.consumer.pdl.PdlConsumer
import no.nav.syfo.consumer.sts.StsConsumer
import no.nav.syfo.motebehov.MotebehovSvar
import no.nav.syfo.motebehov.NyttMotebehov
import no.nav.syfo.motebehov.api.MotebehovBrukerController
import no.nav.syfo.motebehov.database.MotebehovDAO
import no.nav.syfo.motebehov.historikk.HistorikkService
import no.nav.syfo.testhelper.OidcTestHelper.loggInnBruker
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
import no.nav.syfo.testhelper.generator.generatePdlHentPerson
import no.nav.syfo.testhelper.generator.generateStsToken
import no.nav.syfo.testhelper.mockAndExpectBehandlendeEnhetRequest
import no.nav.syfo.testhelper.mockSvarFraSyfoTilgangskontrollV2TilgangTilBruker
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cache.CacheManager
import org.springframework.http.HttpStatus
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.util.concurrent.ListenableFuture
import org.springframework.web.client.RestTemplate
import java.text.ParseException
import java.time.LocalDateTime
import java.util.function.Consumer
import javax.inject.Inject

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
    private lateinit var motebehovController: MotebehovBrukerController

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

    @MockkBean
    private lateinit var brukertilgangConsumer: BrukertilgangConsumer

    @MockkBean(relaxed = true)
    private lateinit var pdlConsumer: PdlConsumer

    @MockkBean
    private lateinit var stsConsumer: StsConsumer

    @MockkBean
    private lateinit var kafkaTemplate: KafkaTemplate<String, Any>

    private lateinit var mockRestServiceServer: MockRestServiceServer

    @Inject
    @Qualifier("restTemplateWithProxy")
    private lateinit var restTemplateWithProxy: RestTemplate
    private lateinit var mockRestServiceWithProxyServer: MockRestServiceServer

    private val stsToken = generateStsToken().access_token

    @BeforeEach
    fun setUp() {
        cleanDB()

        mockRestServiceServer = MockRestServiceServer.bindTo(restTemplate).build()
        mockRestServiceWithProxyServer = MockRestServiceServer.bindTo(restTemplateWithProxy).build()

        every { kafkaTemplate.send(any(), any(), any()) } returns mockk<ListenableFuture<SendResult<String, Any>>>(relaxed = true)
        every { brukertilgangConsumer.hasAccessToAnsatt(ARBEIDSTAKER_FNR) } returns true

        every { pdlConsumer.aktorid(ARBEIDSTAKER_FNR) } returns ARBEIDSTAKER_AKTORID
        every { pdlConsumer.aktorid(LEDER_FNR) } returns LEDER_AKTORID
        every { pdlConsumer.fnr(ARBEIDSTAKER_AKTORID) } returns ARBEIDSTAKER_FNR
        every { pdlConsumer.fnr(LEDER_AKTORID) } returns LEDER_FNR
        every { pdlConsumer.person(ARBEIDSTAKER_FNR) } returns generatePdlHentPerson(null, null)
        every { pdlConsumer.person(LEDER_FNR) } returns generatePdlHentPerson(null, null)
        every { stsConsumer.token() } returns stsToken
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
        val nyttMotebehov = arbeidsgiverLagrerMotebehov(LEDER_FNR, ARBEIDSTAKER_FNR, VIRKSOMHETSNUMMER)

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
        val nyttMotebehov = sykmeldtLagrerMotebehov(ARBEIDSTAKER_FNR, VIRKSOMHETSNUMMER, true)

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
        assertThat(motebehov.motebehovSvar).usingRecursiveComparison().isEqualTo(nyttMotebehov.motebehovSvar)
    }

    @Test
    @Throws(Exception::class)
    fun `hent Historikk`() {
        mockBehandlendEnhet(ARBEIDSTAKER_FNR)
        arbeidsgiverLagrerMotebehov(LEDER_FNR, ARBEIDSTAKER_FNR, VIRKSOMHETSNUMMER)
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
        sykmeldtLagrerMotebehov(ARBEIDSTAKER_FNR, VIRKSOMHETSNUMMER, true)
        resetMockRestServers()
        arbeidsgiverLagrerMotebehov(LEDER_FNR, ARBEIDSTAKER_FNR, VIRKSOMHETSNUMMER)
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
        sykmeldtLagrerMotebehov(ARBEIDSTAKER_FNR, VIRKSOMHETSNUMMER, false)
        resetMockRestServers()
        arbeidsgiverLagrerMotebehov(LEDER_FNR, ARBEIDSTAKER_FNR, VIRKSOMHETSNUMMER)
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
        verify(exactly = 2) { kafkaTemplate.send(any(), any(), any()) }
    }

    @Test
    @Throws(ParseException::class)
    fun `behandle Motebehov og ulik Veileder behandler`() {
        mockBehandlendEnhet(ARBEIDSTAKER_FNR)
        sykmeldtLagrerMotebehov(ARBEIDSTAKER_FNR, VIRKSOMHETSNUMMER, true)
        behandleMotebehov(ARBEIDSTAKER_AKTORID, VEILEDER_ID)
        resetMockRestServers()
        arbeidsgiverLagrerMotebehov(LEDER_FNR, ARBEIDSTAKER_FNR, VIRKSOMHETSNUMMER)
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
        verify(exactly = 3) { kafkaTemplate.send(any(), any(), any()) }
    }

    @Test
    @Throws(ParseException::class)
    fun `behandle ikkeeksisterende Motebehov`() {
        mockBehandlendEnhet(ARBEIDSTAKER_FNR)
        sykmeldtLagrerMotebehov(ARBEIDSTAKER_FNR, VIRKSOMHETSNUMMER, true)
        behandleMotebehov(ARBEIDSTAKER_AKTORID, VEILEDER_ID)
        resetMockRestServers()
        loggInnVeilederADV2(contextHolder, VEILEDER_2_ID)
        mockSvarFraSyfoTilgangskontroll(ARBEIDSTAKER_FNR, HttpStatus.OK)

        assertThrows<RuntimeException> { motebehovVeilederController.behandleMotebehov(ARBEIDSTAKER_FNR) }
    }

    private fun arbeidsgiverLagrerMotebehov(
        lederFnr: String,
        arbeidstakerFnr: String,
        virksomhetsnummer: String
    ): NyttMotebehov {
        loggInnBruker(contextHolder, lederFnr)
        val motebehovSvar = MotebehovSvar(
            harMotebehov = true,
            forklaring = ""
        )
        val nyttMotebehov = NyttMotebehov(
            arbeidstakerFnr = arbeidstakerFnr,
            virksomhetsnummer = virksomhetsnummer,
            motebehovSvar = motebehovSvar
        )
        motebehovController.lagreMotebehov(nyttMotebehov)
        return nyttMotebehov
    }

    private fun sykmeldtLagrerMotebehov(
        sykmeldtFnr: String,
        virksomhetsnummer: String,
        harBehov: Boolean
    ): NyttMotebehov {
        loggInnBruker(contextHolder, sykmeldtFnr)
        val motebehovSvar = MotebehovSvar(
            harMotebehov = harBehov,
            forklaring = ""
        )
        val nyttMotebehov = NyttMotebehov(
            arbeidstakerFnr = sykmeldtFnr,
            virksomhetsnummer = virksomhetsnummer,
            motebehovSvar = motebehovSvar
        )
        motebehovController.lagreMotebehov(nyttMotebehov)
        return nyttMotebehov
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

    private fun resetMockRestServers() {
        mockRestServiceServer.reset()
        mockRestServiceWithProxyServer.reset()
    }

    private fun cleanDB() {
        motebehovDAO.nullstillMotebehov(ARBEIDSTAKER_AKTORID)
    }
}
