package no.nav.syfo.motebehov.api

import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.syfo.LocalApplication
import no.nav.syfo.consumer.aktorregister.AktorregisterConsumer
import no.nav.syfo.consumer.aktorregister.domain.Fodselsnummer
import no.nav.syfo.consumer.brukertilgang.BrukertilgangConsumer
import no.nav.syfo.consumer.pdl.PdlConsumer
import no.nav.syfo.consumer.sts.StsConsumer
import no.nav.syfo.motebehov.database.MotebehovDAO
import no.nav.syfo.oversikthendelse.OversikthendelseProducer
import no.nav.syfo.testhelper.OidcTestHelper.loggInnBruker
import no.nav.syfo.testhelper.OidcTestHelper.loggUtAlle
import no.nav.syfo.testhelper.UserConstants.ARBEIDSTAKER_AKTORID
import no.nav.syfo.testhelper.UserConstants.ARBEIDSTAKER_FNR
import no.nav.syfo.testhelper.UserConstants.LEDER_AKTORID
import no.nav.syfo.testhelper.UserConstants.LEDER_FNR
import no.nav.syfo.testhelper.UserConstants.VIRKSOMHETSNUMMER
import no.nav.syfo.testhelper.generator.*
import no.nav.syfo.testhelper.mockAndExpectBehandlendeEnhetRequest
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.cache.CacheManager
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.web.client.RestTemplate
import java.util.function.Consumer
import javax.inject.Inject

@ExtendWith(SpringExtension::class)
@SpringBootTest(classes = [LocalApplication::class])
@DirtiesContext
class MotebehovComponentTest {
    @Value("\${azure.openid.config.token.endpoint}")
    private lateinit var azureTokenEndpoint: String

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
    private lateinit var pdlConsumer: PdlConsumer

    @MockBean
    private lateinit var oversikthendelseProducer: OversikthendelseProducer

    @MockBean
    private lateinit var brukertilgangConsumer: BrukertilgangConsumer

    @MockBean
    private lateinit var stsConsumer: StsConsumer

    @Inject
    @Qualifier("restTemplateWithProxy")
    private lateinit var restTemplateWithProxy: RestTemplate
    private lateinit var mockRestServiceWithProxyServer: MockRestServiceServer

    private lateinit var mockRestServiceServer: MockRestServiceServer

    private val motebehovGenerator = MotebehovGenerator()

    private val stsToken = generateStsToken().access_token

    @BeforeEach
    fun setUp() {
        Mockito.`when`(aktorregisterConsumer.getAktorIdForFodselsnummer(Fodselsnummer(ARBEIDSTAKER_FNR))).thenReturn(ARBEIDSTAKER_AKTORID)
        Mockito.`when`(aktorregisterConsumer.getAktorIdForFodselsnummer(Fodselsnummer(LEDER_FNR))).thenReturn(LEDER_AKTORID)
        Mockito.`when`(brukertilgangConsumer.hasAccessToAnsatt(ARBEIDSTAKER_FNR)).thenReturn(true)
        Mockito.`when`(pdlConsumer.person(Fodselsnummer(ARBEIDSTAKER_FNR))).thenReturn(generatePdlHentPerson(null, null))
        Mockito.`when`(stsConsumer.token()).thenReturn(stsToken)
        mockRestServiceServer = MockRestServiceServer.bindTo(restTemplate).build()
        mockRestServiceWithProxyServer = MockRestServiceServer.bindTo(restTemplateWithProxy).build()
        loggInnBruker(contextHolder, LEDER_FNR)
        cleanDB()
    }

    @AfterEach
    fun tearDown() {
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
    fun lagreOgHentMotebehovOgSendOversikthendelseVedSvarMedBehov() {
        mockAndExpectBehandlendeEnhetRequest(
            azureTokenEndpoint,
            mockRestServiceWithProxyServer,
            mockRestServiceServer,
            behandlendeenhetUrl,
            ARBEIDSTAKER_FNR
        )
        lagreOgHentMotebehovOgSendOversikthendelse(true)
    }

    @Test
    fun lagreOgHentMotebehovOgSendOversikthendelseMedSvarUtenBehov() {
        mockAndExpectBehandlendeEnhetRequest(
            azureTokenEndpoint,
            mockRestServiceWithProxyServer,
            mockRestServiceServer,
            behandlendeenhetUrl,
            ARBEIDSTAKER_FNR
        )
        lagreOgHentMotebehovOgSendOversikthendelse(false)
    }

    private fun lagreOgHentMotebehovOgSendOversikthendelse(harBehov: Boolean) {
        val motebehovSvar = motebehovGenerator.lagMotebehovSvar(harBehov)

        // Lagre
        motebehovController.lagreMotebehov(motebehovGenerator.lagNyttMotebehovFraAT().copy(
            motebehovSvar = motebehovSvar
        ))

        // Hent
        val motebehovListe = motebehovController.hentMotebehovListe(ARBEIDSTAKER_FNR, VIRKSOMHETSNUMMER)
        Assertions.assertThat(motebehovListe).size().isOne
        val motebehov = motebehovListe[0]
        Assertions.assertThat(motebehov.opprettetAv).isEqualTo(LEDER_AKTORID)
        Assertions.assertThat(motebehov.arbeidstakerFnr).isEqualTo(ARBEIDSTAKER_FNR)
        Assertions.assertThat(motebehov.virksomhetsnummer).isEqualTo(VIRKSOMHETSNUMMER)
        Assertions.assertThat(motebehov.motebehovSvar).isEqualToComparingFieldByField(motebehovSvar)
        if (harBehov) {
            Mockito.verify(oversikthendelseProducer).sendOversikthendelse(any(), any())
        } else {
            Mockito.verify(oversikthendelseProducer, Mockito.never()).sendOversikthendelse(any(), any())
        }
    }

    private fun cleanDB() {
        motebehovDAO.nullstillMotebehov(ARBEIDSTAKER_AKTORID)
    }
}

private fun <T> any(): T {
    Mockito.any<T>()
    return uninitialized()
}

private fun <T> uninitialized(): T = null as T
