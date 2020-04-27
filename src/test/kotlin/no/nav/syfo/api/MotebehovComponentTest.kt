package no.nav.syfo.api

import no.nav.security.oidc.context.OIDCRequestContextHolder
import no.nav.syfo.LocalApplication
import no.nav.syfo.aktorregister.AktorregisterConsumer
import no.nav.syfo.aktorregister.domain.Fodselsnummer
import no.nav.syfo.kafka.producer.model.KOversikthendelse
import no.nav.syfo.oversikthendelse.OversikthendelseProducer
import no.nav.syfo.pdl.PdlConsumer
import no.nav.syfo.repository.dao.MotebehovDAO
import no.nav.syfo.sts.StsConsumer
import no.nav.syfo.testhelper.*
import no.nav.syfo.testhelper.OidcTestHelper.loggInnBruker
import no.nav.syfo.testhelper.OidcTestHelper.loggUtAlle
import no.nav.syfo.testhelper.UserConstants.ARBEIDSTAKER_AKTORID
import no.nav.syfo.testhelper.UserConstants.ARBEIDSTAKER_FNR
import no.nav.syfo.testhelper.UserConstants.LEDER_AKTORID
import no.nav.syfo.testhelper.UserConstants.LEDER_FNR
import no.nav.syfo.testhelper.UserConstants.VIRKSOMHETSNUMMER
import org.assertj.core.api.Assertions
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.cache.CacheManager
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.web.client.RestTemplate
import java.util.function.Consumer
import javax.inject.Inject

@RunWith(SpringRunner::class)
@SpringBootTest(classes = [LocalApplication::class])
@DirtiesContext
class MotebehovComponentTest {
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
    private lateinit var oidcRequestContextHolder: OIDCRequestContextHolder

    @Inject
    private lateinit var motebehovDAO: MotebehovDAO

    @Inject
    private lateinit var cacheManager: CacheManager

    @Inject
    private lateinit var stsConsumer: StsConsumer

    @Inject
    private lateinit var restTemplate: RestTemplate

    @MockBean
    private lateinit var aktorregisterConsumer: AktorregisterConsumer

    @MockBean
    private lateinit var pdlConsumer: PdlConsumer

    @MockBean
    private lateinit var oversikthendelseProducer: OversikthendelseProducer

    private lateinit var mockRestServiceServer: MockRestServiceServer

    private val motebehovGenerator = MotebehovGenerator()

    @Before
    fun setUp() {
        Mockito.`when`(aktorregisterConsumer.getAktorIdForFodselsnummer(Fodselsnummer(ARBEIDSTAKER_FNR))).thenReturn(ARBEIDSTAKER_AKTORID)
        Mockito.`when`(aktorregisterConsumer.getAktorIdForFodselsnummer(Fodselsnummer(LEDER_FNR))).thenReturn(LEDER_AKTORID)
        Mockito.`when`(pdlConsumer.person(Fodselsnummer(ARBEIDSTAKER_FNR))).thenReturn(generatePdlHentPerson(null, null))
        mockRestServiceServer = MockRestServiceServer.bindTo(restTemplate).build()
        loggInnBruker(oidcRequestContextHolder, LEDER_FNR)
        cleanDB()
        mockAndExpectBrukertilgangRequest(mockRestServiceServer, brukertilgangUrl, ARBEIDSTAKER_FNR)
    }

    @After
    fun tearDown() {
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
    fun lagreOgHentMotebehovOgSendOversikthendelseVedSvarMedBehov() {
        mockSTS()
        mockAndExpectBehandlendeEnhetRequest(mockRestServiceServer, behandlendeenhetUrl, ARBEIDSTAKER_FNR)
        lagreOgHentMotebehovOgSendOversikthendelse(true)
    }

    @Test
    fun lagreOgHentMotebehovOgSendOversikthendelseMedSvarUtenBehov() {
        mockSTS()
        mockAndExpectBehandlendeEnhetRequest(mockRestServiceServer, behandlendeenhetUrl, ARBEIDSTAKER_FNR)
        lagreOgHentMotebehovOgSendOversikthendelse(false)
    }

    private fun mockSTS() {
        if (!stsConsumer.isTokenCached()) {
            mockAndExpectSTSService(mockRestServiceServer, stsUrl, srvUsername, srvPassword)
        }
    }

    private fun lagreOgHentMotebehovOgSendOversikthendelse(harBehov: Boolean) {
        val motebehovSvar = motebehovGenerator.lagMotebehovSvar(harBehov)

        // Lagre
        motebehovController.lagreMotebehov(motebehovGenerator.lagNyttMotebehovFraAT())

        // Hent
        val motebehovListe = motebehovController.hentMotebehovListe(ARBEIDSTAKER_FNR, VIRKSOMHETSNUMMER)
        Assertions.assertThat(motebehovListe).size().isOne
        val motebehov = motebehovListe[0]
        Assertions.assertThat(motebehov.opprettetAv).isEqualTo(LEDER_AKTORID)
        Assertions.assertThat(motebehov.arbeidstakerFnr).isEqualTo(ARBEIDSTAKER_FNR)
        Assertions.assertThat(motebehov.virksomhetsnummer).isEqualTo(VIRKSOMHETSNUMMER)
        Assertions.assertThat(motebehov.motebehovSvar).isEqualToComparingFieldByField(motebehovSvar)
        if (harBehov) {
            Mockito.verify(oversikthendelseProducer).sendOversikthendelse(any())
        } else {
            Mockito.verify(oversikthendelseProducer, Mockito.never()).sendOversikthendelse(any())
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