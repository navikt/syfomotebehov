package no.nav.syfo.varsel

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import no.nav.syfo.LocalApplication
import no.nav.syfo.api.VarselController
import no.nav.syfo.domain.rest.MotebehovsvarVarselInfo
import no.nav.syfo.kafka.producer.model.KTredjepartsvarsel
import no.nav.syfo.mote.MoterService
import no.nav.syfo.sts.StsConsumer
import no.nav.syfo.testhelper.UserConstants
import no.nav.syfo.testhelper.mockAndExpectSTSService
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
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
import javax.inject.Inject

@RunWith(SpringRunner::class)
@SpringBootTest(classes = [LocalApplication::class])
@DirtiesContext
class VarselComponentTest {
    @Value("\${security.token.service.rest.url}")
    private lateinit var stsUrl: String

    @Value("\${srv.username}")
    private lateinit var srvUsername: String

    @Value("\${srv.password}")
    private lateinit var srvPassword: String

    @Inject
    private lateinit var stsConsumer: StsConsumer

    @Inject
    private lateinit var varselController: VarselController

    @Inject
    private lateinit var restTemplate: RestTemplate

    @Inject
    private lateinit var moterService: MoterService

    @Inject
    private lateinit var varselService: VarselService

    @Inject
    private lateinit var tredjepartsvarselProducer: TredjepartsvarselProducer

    @MockBean
    private lateinit var kafkaTemplate: KafkaTemplate<String, Any>

    private lateinit var mockRestServiceServer: MockRestServiceServer

    private val objectMapper = ObjectMapper()
            .registerModule(JavaTimeModule())
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
    private val motebehovsvarVarselInfo = MotebehovsvarVarselInfo()
            .sykmeldtAktorId(UserConstants.ARBEIDSTAKER_AKTORID)
            .orgnummer(UserConstants.VIRKSOMHETSNUMMER)
    private val argumentCaptor = ArgumentCaptor.forClass(KTredjepartsvarsel::class.java)

    @Before
    fun setUp() {
        mockRestServiceServer = MockRestServiceServer.bindTo(restTemplate).build()
    }

    @After
    fun tearDown() {
        // Verify all expectations met
        mockRestServiceServer.verify()
    }

    @Test
    @Throws(Exception::class)
    fun sendVarselNaermesteLeder_skal_sende_varsel_til_NL_hvis_ikke_mote() {
        mockSTS()
        mockSvarFraSyfomoteadmin(false)
        Mockito.`when`(kafkaTemplate.send(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.any(KTredjepartsvarsel::class.java))).thenReturn(Mockito.mock(ListenableFuture::class.java) as ListenableFuture<SendResult<String, Any>>?)
        val returnertSvarFraVarselcontroller = varselController.sendVarselNaermesteLeder(motebehovsvarVarselInfo)
        Mockito.verify(kafkaTemplate).send(ArgumentMatchers.eq(TredjepartsvarselProducer.TREDJEPARTSVARSEL_TOPIC), ArgumentMatchers.anyString(), argumentCaptor.capture())
        val sendtKTredjepartsvarsel = argumentCaptor.value
        verifySendtKtredjepartsvarsel(sendtKTredjepartsvarsel)
        Assert.assertEquals(HttpStatus.OK.value().toLong(), returnertSvarFraVarselcontroller.status.toLong())
    }

    @Test
    @Throws(Exception::class)
    fun sendVarselNaermesteLeder_skal_ikke_sende_varsel_til_NL_hvis_mote_finnes() {
        mockSTS()
        mockSvarFraSyfomoteadmin(true)
        val returnertSvarFraVarselcontroller = varselController.sendVarselNaermesteLeder(motebehovsvarVarselInfo)
        Mockito.verify(kafkaTemplate, Mockito.never()).send(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
        Assert.assertEquals(HttpStatus.OK.value().toLong(), returnertSvarFraVarselcontroller.status.toLong())
    }

    private fun mockSTS() {
        if (!stsConsumer.isTokenCached()) {
            mockAndExpectSTSService(mockRestServiceServer, stsUrl, srvUsername, srvPassword)
        }
    }

    @Throws(Exception::class)
    private fun mockSvarFraSyfomoteadmin(harAktivtMote: Boolean) {
        val svarFraSyfomoteadminJson = objectMapper.writeValueAsString(harAktivtMote)
        val url = UriComponentsBuilder.fromHttpUrl(MoterService.SYFOMOTEADMIN_BASEURL)
                .pathSegment("system", UserConstants.ARBEIDSTAKER_AKTORID, "harAktivtMote")
                .toUriString()
        mockRestServiceServer.expect(ExpectedCount.once(), MockRestRequestMatchers.requestTo(url))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andRespond(MockRestResponseCreators.withSuccess(svarFraSyfomoteadminJson, MediaType.APPLICATION_JSON))
    }

    private fun verifySendtKtredjepartsvarsel(kTredjepartsvarsel: KTredjepartsvarsel) {
        Assert.assertEquals(kTredjepartsvarsel.type(), VarselType.NAERMESTE_LEDER_SVAR_MOTEBEHOV.name)
        Assert.assertNotNull(kTredjepartsvarsel.ressursId())
        Assert.assertEquals(kTredjepartsvarsel.aktorId(), UserConstants.ARBEIDSTAKER_AKTORID)
        Assert.assertEquals(kTredjepartsvarsel.orgnummer(), UserConstants.VIRKSOMHETSNUMMER)
        Assert.assertNotNull(kTredjepartsvarsel.utsendelsestidspunkt())
    }
}
