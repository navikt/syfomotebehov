package no.nav.syfo.api

import no.nav.security.oidc.context.OIDCRequestContextHolder
import no.nav.syfo.LocalApplication
import no.nav.syfo.aktorregister.AktorregisterConsumer
import no.nav.syfo.aktorregister.domain.AktorId
import no.nav.syfo.aktorregister.domain.Fodselsnummer
import no.nav.syfo.domain.rest.Motebehov
import no.nav.syfo.domain.rest.MotebehovSvar
import no.nav.syfo.domain.rest.NyttMotebehov
import no.nav.syfo.historikk.HistorikkService
import no.nav.syfo.oversikthendelse.KOversikthendelse
import no.nav.syfo.oidc.OIDCIssuer.AZURE
import no.nav.syfo.oversikthendelse.OversikthendelseProducer
import no.nav.syfo.pdl.PdlConsumer
import no.nav.syfo.repository.dao.MotebehovDAO
import no.nav.syfo.sts.StsConsumer
import no.nav.syfo.testhelper.OidcTestHelper.loggInnBruker
import no.nav.syfo.testhelper.OidcTestHelper.loggInnVeilederAzure
import no.nav.syfo.testhelper.OidcTestHelper.loggUtAlle
import no.nav.syfo.testhelper.UserConstants.ARBEIDSTAKER_AKTORID
import no.nav.syfo.testhelper.UserConstants.ARBEIDSTAKER_FNR
import no.nav.syfo.testhelper.UserConstants.LEDER_AKTORID
import no.nav.syfo.testhelper.UserConstants.LEDER_FNR
import no.nav.syfo.testhelper.UserConstants.PERSON_FULL_NAME
import no.nav.syfo.testhelper.UserConstants.VEILEDER_2_ID
import no.nav.syfo.testhelper.UserConstants.VEILEDER_ID
import no.nav.syfo.testhelper.UserConstants.VIRKSOMHETSNUMMER
import no.nav.syfo.testhelper.generatePdlHentPerson
import no.nav.syfo.testhelper.mockAndExpectBehandlendeEnhetRequest
import no.nav.syfo.testhelper.mockAndExpectBrukertilgangRequest
import no.nav.syfo.testhelper.mockAndExpectSTSService
import no.nav.syfo.veiledertilgang.VeilederTilgangConsumer
import org.assertj.core.api.Assertions
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.cache.CacheManager
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.test.web.client.ExpectedCount
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers
import org.springframework.test.web.client.response.MockRestResponseCreators
import org.springframework.util.concurrent.ListenableFuture
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.text.ParseException
import java.time.LocalDateTime
import java.util.function.Consumer
import javax.inject.Inject

@RunWith(SpringRunner::class)
@SpringBootTest(classes = [LocalApplication::class])
@DirtiesContext
class MotebehovVeilederADControllerTest {
    @Value("\${tilgangskontrollapi.url}")
    private lateinit var tilgangskontrollUrl: String

    @Value("\${syfobehandlendeenhet.url}")
    private lateinit var behandlendeenhetUrl: String

    @Value("\${syfobrukertilgang.url}")
    private lateinit var brukertilgangUrl: String

    @Value("\${security.token.service.rest.url}")
    private lateinit var stsUrl: String

    @Value("\${srv.username}")
    private lateinit var srvUsername: String

    @Value("\${srv.password}")
    private lateinit var srvPassword: String

    @Inject
    private lateinit var motebehovController: MotebehovBrukerController

    @Inject
    private lateinit var motebehovVeilederController: MotebehovVeilederADController

    @Inject
    private lateinit var oidcRequestContextHolder: OIDCRequestContextHolder

    @Inject
    private lateinit var motebehovDAO: MotebehovDAO

    @Inject
    private lateinit var stsConsumer: StsConsumer

    @Inject
    private lateinit var cacheManager: CacheManager

    @Inject
    private lateinit var restTemplate: RestTemplate

    @MockBean
    private lateinit var aktorregisterConsumer: AktorregisterConsumer

    @MockBean
    private lateinit var pdlConsumer: PdlConsumer

    @MockBean
    private lateinit var kafkaTemplate: KafkaTemplate<String, Any>

    private lateinit var mockRestServiceServer: MockRestServiceServer

    @Before
    fun setUp() {
        cleanDB()
        mockRestServiceServer = MockRestServiceServer.bindTo(restTemplate).build()
        Mockito.`when`(kafkaTemplate.send(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.any(KOversikthendelse::class.java))).thenReturn(Mockito.mock(ListenableFuture::class.java) as ListenableFuture<SendResult<String, Any>>?)
        Mockito.`when`(aktorregisterConsumer.getFnrForAktorId(AktorId(ARBEIDSTAKER_AKTORID))).thenReturn(ARBEIDSTAKER_FNR)
        Mockito.`when`(aktorregisterConsumer.getFnrForAktorId(AktorId(LEDER_AKTORID))).thenReturn(LEDER_FNR)
        Mockito.`when`(aktorregisterConsumer.getAktorIdForFodselsnummer(Fodselsnummer(ARBEIDSTAKER_FNR))).thenReturn(ARBEIDSTAKER_AKTORID)
        Mockito.`when`(aktorregisterConsumer.getAktorIdForFodselsnummer(Fodselsnummer(LEDER_FNR))).thenReturn(LEDER_AKTORID)
        Mockito.`when`(pdlConsumer.person(Fodselsnummer(ARBEIDSTAKER_FNR))).thenReturn(generatePdlHentPerson(null, null))
        Mockito.`when`(pdlConsumer.person(Fodselsnummer(LEDER_FNR))).thenReturn(generatePdlHentPerson(null, null))
    }

    @After
    fun tearDown() {
        // Verify all expectations met
        mockRestServiceServer.verify()
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
    @Throws(ParseException::class)
    fun arbeidsgiverLagrerOgVeilederHenterMotebehov() {
        mockAndExpectBrukertilgangRequest(mockRestServiceServer, brukertilgangUrl, ARBEIDSTAKER_FNR)
        mockBehandlendEnhet(ARBEIDSTAKER_FNR)
        val nyttMotebehov = arbeidsgiverLagrerMotebehov(LEDER_FNR, ARBEIDSTAKER_FNR, VIRKSOMHETSNUMMER)

        // Veileder henter møtebehov
        mockRestServiceServer.reset()
        loggInnVeilederAzure(oidcRequestContextHolder, VEILEDER_ID)
        mockSvarFraSyfoTilgangskontroll(ARBEIDSTAKER_FNR, HttpStatus.OK)
        val motebehovListe = motebehovVeilederController.hentMotebehovListe(ARBEIDSTAKER_FNR)
        Assertions.assertThat(motebehovListe).size().isOne
        val motebehov = motebehovListe[0]
        Assertions.assertThat(motebehov.opprettetAv).isEqualTo(LEDER_AKTORID)
        Assertions.assertThat(motebehov.arbeidstakerFnr).isEqualTo(ARBEIDSTAKER_FNR)
        Assertions.assertThat(motebehov.virksomhetsnummer).isEqualTo(VIRKSOMHETSNUMMER)
        Assertions.assertThat(motebehov.motebehovSvar).isEqualToComparingFieldByField(nyttMotebehov.motebehovSvar)
    }

    @Test
    @Throws(ParseException::class)
    fun sykmeldtLagrerOgVeilederHenterMotebehov() {
        mockBehandlendEnhet(ARBEIDSTAKER_FNR)
        val nyttMotebehov = sykmeldtLagrerMotebehov(ARBEIDSTAKER_FNR, VIRKSOMHETSNUMMER, true)

        // Veileder henter møtebehov
        mockRestServiceServer.reset()
        loggInnVeilederAzure(oidcRequestContextHolder, VEILEDER_ID)
        mockSvarFraSyfoTilgangskontroll(ARBEIDSTAKER_FNR, HttpStatus.OK)
        val motebehovListe = motebehovVeilederController.hentMotebehovListe(ARBEIDSTAKER_FNR)
        Assertions.assertThat(motebehovListe).size().isOne
        val motebehov = motebehovListe[0]
        Assertions.assertThat(motebehov.opprettetAv).isEqualTo(ARBEIDSTAKER_AKTORID)
        Assertions.assertThat(motebehov.arbeidstakerFnr).isEqualTo(ARBEIDSTAKER_FNR)
        Assertions.assertThat(motebehov.virksomhetsnummer).isEqualTo(VIRKSOMHETSNUMMER)
        Assertions.assertThat(motebehov.motebehovSvar).isEqualToComparingFieldByField(nyttMotebehov.motebehovSvar)
    }

    @Test
    @Throws(Exception::class)
    fun hentHistorikk() {
        mockAndExpectBrukertilgangRequest(mockRestServiceServer, brukertilgangUrl, ARBEIDSTAKER_FNR)
        mockBehandlendEnhet(ARBEIDSTAKER_FNR)
        arbeidsgiverLagrerMotebehov(LEDER_FNR, ARBEIDSTAKER_FNR, VIRKSOMHETSNUMMER)
        mockRestServiceServer.reset()
        loggInnVeilederAzure(oidcRequestContextHolder, VEILEDER_ID)
        mockSvarFraSyfoTilgangskontroll(ARBEIDSTAKER_FNR, HttpStatus.OK)
        val motebehovListe = motebehovVeilederController.hentMotebehovListe(ARBEIDSTAKER_FNR)
        val motebehov = motebehovListe[0]
        mockRestServiceServer.reset()
        loggInnVeilederAzure(oidcRequestContextHolder, VEILEDER_ID)
        mockSvarFraSyfoTilgangskontroll(ARBEIDSTAKER_FNR, HttpStatus.OK)
        motebehovVeilederController.behandleMotebehov(ARBEIDSTAKER_FNR)
        val historikkListe = motebehovVeilederController.hentMotebehovHistorikk(ARBEIDSTAKER_FNR)
        Assertions.assertThat(historikkListe).size().isEqualTo(2)
        val (opprettetAv, tekst, tidspunkt) = historikkListe[0]
        Assertions.assertThat(opprettetAv).isEqualTo(LEDER_AKTORID)
        Assertions.assertThat(tekst).isEqualTo(PERSON_FULL_NAME + HistorikkService.HAR_SVART_PAA_MOTEBEHOV)
        Assertions.assertThat(tidspunkt).isEqualTo(motebehov.opprettetDato)
        val (_, tekst1, tidspunkt1) = historikkListe[1]
        Assertions.assertThat(tekst1).isEqualTo(HistorikkService.MOTEBEHOVET_BLE_LEST_AV + VEILEDER_ID)
        val today = LocalDateTime.now()
        Assertions.assertThat(tidspunkt1.minusNanos(tidspunkt1.nano.toLong())).isEqualTo(today.minusNanos(today.nano.toLong()))
    }

    @Test
    @Throws(ParseException::class)
    fun hentMotebehovUbehandlet() {
        mockBehandlendEnhet(ARBEIDSTAKER_FNR)
        sykmeldtLagrerMotebehov(ARBEIDSTAKER_FNR, VIRKSOMHETSNUMMER, true)
        mockRestServiceServer.reset()
        mockAndExpectBrukertilgangRequest(mockRestServiceServer, brukertilgangUrl, ARBEIDSTAKER_FNR)
        arbeidsgiverLagrerMotebehov(LEDER_FNR, ARBEIDSTAKER_FNR, VIRKSOMHETSNUMMER)
        mockRestServiceServer.reset()
        loggInnVeilederAzure(oidcRequestContextHolder, VEILEDER_ID)
        mockSvarFraSyfoTilgangskontroll(ARBEIDSTAKER_FNR, HttpStatus.OK)
        val motebehovListe = motebehovVeilederController.hentMotebehovListe(ARBEIDSTAKER_FNR)
        motebehovListe.forEach(Consumer { motebehov: Motebehov ->
            Assertions.assertThat(motebehov.behandletTidspunkt).isNull()
            Assertions.assertThat(motebehov.behandletVeilederIdent).isNull()
        })
    }

    @Test
    @Throws(ParseException::class)
    fun behandleKunMotebehovMedBehovForMote() {
        mockBehandlendEnhet(ARBEIDSTAKER_FNR)
        sykmeldtLagrerMotebehov(ARBEIDSTAKER_FNR, VIRKSOMHETSNUMMER, false)
        mockRestServiceServer.reset()
        mockAndExpectBrukertilgangRequest(mockRestServiceServer, brukertilgangUrl, ARBEIDSTAKER_FNR)
        arbeidsgiverLagrerMotebehov(LEDER_FNR, ARBEIDSTAKER_FNR, VIRKSOMHETSNUMMER)
        mockRestServiceServer.reset()
        loggInnVeilederAzure(oidcRequestContextHolder, VEILEDER_ID)
        mockSvarFraSyfoTilgangskontroll(ARBEIDSTAKER_FNR, HttpStatus.OK)
        motebehovVeilederController.behandleMotebehov(ARBEIDSTAKER_FNR)
        val motebehovListe = motebehovVeilederController.hentMotebehovListe(ARBEIDSTAKER_FNR)
        Assertions.assertThat(motebehovListe[0].behandletTidspunkt).isNull()
        Assertions.assertThat(motebehovListe[0].behandletVeilederIdent).isEqualTo(null)
        Assert.assertNotNull(motebehovListe[1].behandletTidspunkt)
        Assertions.assertThat(motebehovListe[1].behandletVeilederIdent).isEqualTo(VEILEDER_ID)
        Mockito.verify(kafkaTemplate, Mockito.times(2)).send(ArgumentMatchers.eq(OversikthendelseProducer.OVERSIKTHENDELSE_TOPIC), ArgumentMatchers.anyString(), ArgumentMatchers.any())
    }

    @Test
    @Throws(ParseException::class)
    fun behandleMotebehovUlikVeilederBehandler() {
        mockBehandlendEnhet(ARBEIDSTAKER_FNR)
        sykmeldtLagrerMotebehov(ARBEIDSTAKER_FNR, VIRKSOMHETSNUMMER, true)
        behandleMotebehov(ARBEIDSTAKER_AKTORID, VEILEDER_ID)
        mockRestServiceServer.reset()
        mockAndExpectBrukertilgangRequest(mockRestServiceServer, brukertilgangUrl, ARBEIDSTAKER_FNR)
        arbeidsgiverLagrerMotebehov(LEDER_FNR, ARBEIDSTAKER_FNR, VIRKSOMHETSNUMMER)
        mockRestServiceServer.reset()
        loggInnVeilederAzure(oidcRequestContextHolder, VEILEDER_2_ID)
        mockSvarFraSyfoTilgangskontroll(ARBEIDSTAKER_FNR, HttpStatus.OK)
        motebehovVeilederController.behandleMotebehov(ARBEIDSTAKER_FNR)
        val motebehovListe1 = motebehovVeilederController.hentMotebehovListe(ARBEIDSTAKER_FNR)
        Assert.assertNotNull(motebehovListe1[0].behandletTidspunkt)
        Assertions.assertThat(motebehovListe1[0].behandletVeilederIdent).isEqualTo(VEILEDER_ID)
        Assert.assertNotNull(motebehovListe1[1].behandletTidspunkt)
        Assertions.assertThat(motebehovListe1[1].behandletVeilederIdent).isEqualTo(VEILEDER_2_ID)
        Mockito.verify(kafkaTemplate, Mockito.times(3)).send(ArgumentMatchers.eq(OversikthendelseProducer.OVERSIKTHENDELSE_TOPIC), ArgumentMatchers.anyString(), ArgumentMatchers.any())
    }

    @Test(expected = RuntimeException::class)
    @Throws(ParseException::class)
    fun behandleIkkeEksiterendeMotebehov() {
        mockBehandlendEnhet(ARBEIDSTAKER_FNR)
        sykmeldtLagrerMotebehov(ARBEIDSTAKER_FNR, VIRKSOMHETSNUMMER, true)
        behandleMotebehov(ARBEIDSTAKER_AKTORID, VEILEDER_ID)
        mockRestServiceServer.reset()
        loggInnVeilederAzure(oidcRequestContextHolder, VEILEDER_2_ID)
        mockSvarFraSyfoTilgangskontroll(ARBEIDSTAKER_FNR, HttpStatus.OK)
        motebehovVeilederController.behandleMotebehov(ARBEIDSTAKER_FNR)
    }

    private fun arbeidsgiverLagrerMotebehov(lederFnr: String, arbeidstakerFnr: String, virksomhetsnummer: String): NyttMotebehov {
        loggInnBruker(oidcRequestContextHolder, lederFnr)
        val motebehovSvar = MotebehovSvar()
                .harMotebehov(true)
                .forklaring("")
        val nyttMotebehov = NyttMotebehov()
                .arbeidstakerFnr(arbeidstakerFnr)
                .virksomhetsnummer(virksomhetsnummer)
                .motebehovSvar(
                        motebehovSvar
                )
        motebehovController.lagreMotebehov(nyttMotebehov)
        return nyttMotebehov
    }

    private fun sykmeldtLagrerMotebehov(sykmeldtFnr: String, virksomhetsnummer: String, harBehov: Boolean): NyttMotebehov {
        loggInnBruker(oidcRequestContextHolder, sykmeldtFnr)
        val motebehovSvar = MotebehovSvar()
                .harMotebehov(harBehov)
                .forklaring("")
        val nyttMotebehov = NyttMotebehov()
                .arbeidstakerFnr(sykmeldtFnr)
                .virksomhetsnummer(virksomhetsnummer)
                .motebehovSvar(
                        motebehovSvar
                )
        motebehovController.lagreMotebehov(nyttMotebehov)
        return nyttMotebehov
    }

    private fun behandleMotebehov(aktoerId: String, veileder: String) {
        motebehovDAO.oppdaterUbehandledeMotebehovTilBehandlet(aktoerId, veileder)
    }

    private fun mockSvarFraSyfoTilgangskontroll(fnr: String, status: HttpStatus) {
        val uriString = UriComponentsBuilder.fromHttpUrl(tilgangskontrollUrl)
                .path(VeilederTilgangConsumer.TILGANG_TIL_BRUKER_VIA_AZURE_PATH)
                .queryParam(VeilederTilgangConsumer.FNR, fnr)
                .toUriString()
        val idToken = oidcRequestContextHolder.oidcValidationContext.getToken(AZURE).idToken
        mockRestServiceServer.expect(ExpectedCount.manyTimes(), MockRestRequestMatchers.requestTo(uriString))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, "Bearer $idToken"))
                .andRespond(MockRestResponseCreators.withStatus(status))
    }

    private fun mockSTS() {
        if (!stsConsumer.isTokenCached()) {
            mockAndExpectSTSService(mockRestServiceServer, stsUrl, srvUsername, srvPassword)
        }
    }

    private fun mockBehandlendEnhet(fnr: String) {
        mockSTS()
        mockAndExpectBehandlendeEnhetRequest(mockRestServiceServer, behandlendeenhetUrl, fnr)
    }

    private fun cleanDB() {
        motebehovDAO.nullstillMotebehov(ARBEIDSTAKER_AKTORID)
    }

    companion object {
        private const val HISTORIKK_SIST_ENDRET = "2018-10-10"
    }
}