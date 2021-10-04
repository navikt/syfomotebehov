package no.nav.syfo.motebehov.api.internad.v2

import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.syfo.LocalApplication
import no.nav.syfo.consumer.aktorregister.AktorregisterConsumer
import no.nav.syfo.consumer.aktorregister.domain.AktorId
import no.nav.syfo.consumer.aktorregister.domain.Fodselsnummer
import no.nav.syfo.consumer.brukertilgang.BrukertilgangConsumer
import no.nav.syfo.consumer.pdl.PdlConsumer
import no.nav.syfo.consumer.sts.StsConsumer
import no.nav.syfo.motebehov.*
import no.nav.syfo.motebehov.api.MotebehovBrukerController
import no.nav.syfo.motebehov.database.MotebehovDAO
import no.nav.syfo.motebehov.historikk.HistorikkService
import no.nav.syfo.oversikthendelse.KOversikthendelse
import no.nav.syfo.oversikthendelse.OversikthendelseProducer
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
import no.nav.syfo.testhelper.generator.generatePdlHentPerson
import no.nav.syfo.testhelper.generator.generateStsToken
import no.nav.syfo.testhelper.mockAndExpectBehandlendeEnhetRequest
import no.nav.syfo.testhelper.mockSvarFraSyfoTilgangskontrollV2TilgangTilBruker
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
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

    @Value("\${security.token.service.rest.url}")
    private lateinit var stsUrl: String

    @Value("\${srv.username}")
    private lateinit var srvUsername: String

    @Value("\${srv.password}")
    private lateinit var srvPassword: String

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

    @MockBean
    private lateinit var aktorregisterConsumer: AktorregisterConsumer

    @MockBean
    private lateinit var brukertilgangConsumer: BrukertilgangConsumer

    @MockBean
    private lateinit var pdlConsumer: PdlConsumer

    @MockBean
    private lateinit var stsConsumer: StsConsumer

    @MockBean
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

        Mockito.`when`(kafkaTemplate.send(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.any(KOversikthendelse::class.java))).thenReturn(Mockito.mock(ListenableFuture::class.java) as ListenableFuture<SendResult<String, Any>>?)
        Mockito.`when`(aktorregisterConsumer.getFnrForAktorId(AktorId(ARBEIDSTAKER_AKTORID))).thenReturn(ARBEIDSTAKER_FNR)
        Mockito.`when`(aktorregisterConsumer.getFnrForAktorId(AktorId(LEDER_AKTORID))).thenReturn(LEDER_FNR)
        Mockito.`when`(aktorregisterConsumer.getAktorIdForFodselsnummer(Fodselsnummer(ARBEIDSTAKER_FNR))).thenReturn(ARBEIDSTAKER_AKTORID)
        Mockito.`when`(aktorregisterConsumer.getAktorIdForFodselsnummer(Fodselsnummer(LEDER_FNR))).thenReturn(LEDER_AKTORID)
        Mockito.`when`(brukertilgangConsumer.hasAccessToAnsatt(ARBEIDSTAKER_FNR)).thenReturn(true)
        Mockito.`when`(pdlConsumer.person(Fodselsnummer(ARBEIDSTAKER_FNR))).thenReturn(generatePdlHentPerson(null, null))
        Mockito.`when`(pdlConsumer.person(Fodselsnummer(LEDER_FNR))).thenReturn(generatePdlHentPerson(null, null))
        Mockito.`when`(stsConsumer.token()).thenReturn(stsToken)
    }

    @AfterEach
    fun tearDown() {
        // Verify all expectations met
        mockRestServiceServer.verify()
        mockRestServiceWithProxyServer.verify()
        loggUtAlle(contextHolder)
        mockRestServiceServer.reset()
        mockRestServiceWithProxyServer.reset()
        cacheManager.cacheNames
            .forEach(Consumer { cacheName: String ->
                val cache = cacheManager.getCache(cacheName)
                cache?.clear()
            })
        cleanDB()
    }

    @Test
    @Throws(ParseException::class)
    fun `arbeidsgiver lagrer Motebehov og Veileder henter Motebehov`() {
        mockBehandlendEnhet(ARBEIDSTAKER_FNR)
        val nyttMotebehov = arbeidsgiverLagrerMotebehov(LEDER_FNR, ARBEIDSTAKER_FNR, VIRKSOMHETSNUMMER)

        // Veileder henter møtebehov
        mockRestServiceServer.reset()
        loggInnVeilederADV2(contextHolder, VEILEDER_ID)
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
    fun `arbeidstaker lagrer Motebehov og Veileder henter Motebehov`() {
        mockBehandlendEnhet(ARBEIDSTAKER_FNR)
        val nyttMotebehov = sykmeldtLagrerMotebehov(ARBEIDSTAKER_FNR, VIRKSOMHETSNUMMER, true)

        // Veileder henter møtebehov
        mockRestServiceServer.reset()
        loggInnVeilederADV2(contextHolder, VEILEDER_ID)
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
    fun `hent Historikk`() {
        mockBehandlendEnhet(ARBEIDSTAKER_FNR)
        arbeidsgiverLagrerMotebehov(LEDER_FNR, ARBEIDSTAKER_FNR, VIRKSOMHETSNUMMER)
        mockRestServiceServer.reset()
        loggInnVeilederADV2(contextHolder, VEILEDER_ID)
        mockSvarFraSyfoTilgangskontroll(ARBEIDSTAKER_FNR, HttpStatus.OK)
        val motebehovListe = motebehovVeilederController.hentMotebehovListe(ARBEIDSTAKER_FNR)
        val motebehov = motebehovListe[0]
        mockRestServiceServer.reset()
        loggInnVeilederADV2(contextHolder, VEILEDER_ID)
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
    fun `hent ubehandlede Motebehov`() {
        mockBehandlendEnhet(ARBEIDSTAKER_FNR)
        sykmeldtLagrerMotebehov(ARBEIDSTAKER_FNR, VIRKSOMHETSNUMMER, true)
        mockRestServiceServer.reset()
        arbeidsgiverLagrerMotebehov(LEDER_FNR, ARBEIDSTAKER_FNR, VIRKSOMHETSNUMMER)
        mockRestServiceServer.reset()
        loggInnVeilederADV2(contextHolder, VEILEDER_ID)
        mockSvarFraSyfoTilgangskontroll(ARBEIDSTAKER_FNR, HttpStatus.OK)
        val motebehovListe = motebehovVeilederController.hentMotebehovListe(ARBEIDSTAKER_FNR)

        motebehovListe.forEach(Consumer { motebehovVeilederDTO ->
            Assertions.assertThat(motebehovVeilederDTO.behandletTidspunkt).isNull()
            Assertions.assertThat(motebehovVeilederDTO.behandletVeilederIdent).isNull()
        })
    }

    @Test
    @Throws(ParseException::class)
    fun `behandle kun motebehov med Motebehov`() {
        mockBehandlendEnhet(ARBEIDSTAKER_FNR)
        sykmeldtLagrerMotebehov(ARBEIDSTAKER_FNR, VIRKSOMHETSNUMMER, false)
        mockRestServiceServer.reset()
        arbeidsgiverLagrerMotebehov(LEDER_FNR, ARBEIDSTAKER_FNR, VIRKSOMHETSNUMMER)
        mockRestServiceServer.reset()
        loggInnVeilederADV2(contextHolder, VEILEDER_ID)
        mockSvarFraSyfoTilgangskontroll(ARBEIDSTAKER_FNR, HttpStatus.OK)
        motebehovVeilederController.behandleMotebehov(ARBEIDSTAKER_FNR)
        val motebehovListe = motebehovVeilederController.hentMotebehovListe(ARBEIDSTAKER_FNR)
        Assertions.assertThat(motebehovListe[0].behandletTidspunkt).isNull()
        Assertions.assertThat(motebehovListe[0].behandletVeilederIdent).isEqualTo(null)
        assertNotNull(motebehovListe[1].behandletTidspunkt)
        Assertions.assertThat(motebehovListe[1].behandletVeilederIdent).isEqualTo(VEILEDER_ID)
        Mockito.verify(kafkaTemplate, Mockito.times(2)).send(ArgumentMatchers.eq(OversikthendelseProducer.OVERSIKTHENDELSE_TOPIC), ArgumentMatchers.anyString(), ArgumentMatchers.any())
    }

    @Test
    @Throws(ParseException::class)
    fun `behandle Motebehov og ulik Veileder behandler`() {
        mockBehandlendEnhet(ARBEIDSTAKER_FNR)
        sykmeldtLagrerMotebehov(ARBEIDSTAKER_FNR, VIRKSOMHETSNUMMER, true)
        behandleMotebehov(ARBEIDSTAKER_AKTORID, VEILEDER_ID)
        mockRestServiceServer.reset()
        arbeidsgiverLagrerMotebehov(LEDER_FNR, ARBEIDSTAKER_FNR, VIRKSOMHETSNUMMER)
        mockRestServiceServer.reset()
        loggInnVeilederADV2(contextHolder, VEILEDER_2_ID)
        mockSvarFraSyfoTilgangskontroll(ARBEIDSTAKER_FNR, HttpStatus.OK)
        motebehovVeilederController.behandleMotebehov(ARBEIDSTAKER_FNR)
        val motebehovListe1 = motebehovVeilederController.hentMotebehovListe(ARBEIDSTAKER_FNR)
        assertNotNull(motebehovListe1[0].behandletTidspunkt)
        Assertions.assertThat(motebehovListe1[0].behandletVeilederIdent).isEqualTo(VEILEDER_ID)
        assertNotNull(motebehovListe1[1].behandletTidspunkt)
        Assertions.assertThat(motebehovListe1[1].behandletVeilederIdent).isEqualTo(VEILEDER_2_ID)
        Mockito.verify(kafkaTemplate, Mockito.times(3)).send(ArgumentMatchers.eq(OversikthendelseProducer.OVERSIKTHENDELSE_TOPIC), ArgumentMatchers.anyString(), ArgumentMatchers.any())
    }

    @Test
    @Throws(ParseException::class)
    fun `behandle ikkeeksisterende Motebehov`() {
        mockBehandlendEnhet(ARBEIDSTAKER_FNR)
        sykmeldtLagrerMotebehov(ARBEIDSTAKER_FNR, VIRKSOMHETSNUMMER, true)
        behandleMotebehov(ARBEIDSTAKER_AKTORID, VEILEDER_ID)
        mockRestServiceServer.reset()
        loggInnVeilederADV2(contextHolder, VEILEDER_2_ID)
        mockSvarFraSyfoTilgangskontroll(ARBEIDSTAKER_FNR, HttpStatus.OK)

        assertThrows<RuntimeException> { motebehovVeilederController.behandleMotebehov(ARBEIDSTAKER_FNR) }
    }

    private fun arbeidsgiverLagrerMotebehov(lederFnr: String, arbeidstakerFnr: String, virksomhetsnummer: String): NyttMotebehov {
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

    private fun sykmeldtLagrerMotebehov(sykmeldtFnr: String, virksomhetsnummer: String, harBehov: Boolean): NyttMotebehov {
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
        mockAndExpectBehandlendeEnhetRequest(mockRestServiceServer, behandlendeenhetUrl, fnr)
    }

    private fun cleanDB() {
        motebehovDAO.nullstillMotebehov(ARBEIDSTAKER_AKTORID)
    }
}
