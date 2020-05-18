package no.nav.syfo.varsel

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import no.nav.syfo.LocalApplication
import no.nav.syfo.consumer.aktorregister.AktorregisterConsumer
import no.nav.syfo.consumer.aktorregister.domain.AktorId
import no.nav.syfo.consumer.aktorregister.domain.Fodselsnummer
import no.nav.syfo.consumer.mote.MoteConsumer
import no.nav.syfo.consumer.sts.StsConsumer
import no.nav.syfo.motebehov.motebehovstatus.MotebehovStatusService
import no.nav.syfo.testhelper.UserConstants.ARBEIDSTAKER_AKTORID
import no.nav.syfo.testhelper.UserConstants.ARBEIDSTAKER_FNR
import no.nav.syfo.testhelper.UserConstants.VIRKSOMHETSNUMMER
import no.nav.syfo.testhelper.generator.generateMotebehovStatus
import no.nav.syfo.testhelper.mockAndExpectSTSService
import no.nav.syfo.varsel.api.VarselController
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.mockito.Mockito.`when`
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

    @MockBean
    private lateinit var aktorregisterConsumer: AktorregisterConsumer

    @MockBean
    private lateinit var motebehovStatusService: MotebehovStatusService

    @Inject
    private lateinit var stsConsumer: StsConsumer

    @Inject
    private lateinit var varselController: VarselController

    @Inject
    private lateinit var restTemplate: RestTemplate

    @Inject
    private lateinit var moteConsumer: MoteConsumer

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
    private val motebehovsvarVarselInfo = MotebehovsvarVarselInfo(
            sykmeldtAktorId = ARBEIDSTAKER_AKTORID,
            orgnummer = VIRKSOMHETSNUMMER
    )
    private val argumentCaptor = ArgumentCaptor.forClass(KTredjepartsvarsel::class.java)

    @Before
    fun setUp() {
        mockRestServiceServer = MockRestServiceServer.bindTo(restTemplate).build()
        `when`(aktorregisterConsumer.getFnrForAktorId(AktorId(ARBEIDSTAKER_AKTORID)))
                .thenReturn(ARBEIDSTAKER_FNR)
        `when`(motebehovStatusService.motebehovStatusForArbeidsgiver(Fodselsnummer(ARBEIDSTAKER_FNR), VIRKSOMHETSNUMMER))
                .thenReturn(generateMotebehovStatus)
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
        val url = UriComponentsBuilder.fromHttpUrl(MoteConsumer.SYFOMOTEADMIN_BASEURL)
                .pathSegment("system", ARBEIDSTAKER_AKTORID, "harAktivtMote")
                .toUriString()
        mockRestServiceServer.expect(ExpectedCount.once(), MockRestRequestMatchers.requestTo(url))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andRespond(MockRestResponseCreators.withSuccess(svarFraSyfomoteadminJson, MediaType.APPLICATION_JSON))
    }

    private fun verifySendtKtredjepartsvarsel(kTredjepartsvarsel: KTredjepartsvarsel) {
        Assert.assertEquals(kTredjepartsvarsel.type, VarselType.NAERMESTE_LEDER_SVAR_MOTEBEHOV.name)
        Assert.assertNotNull(kTredjepartsvarsel.ressursId)
        Assert.assertEquals(kTredjepartsvarsel.aktorId, ARBEIDSTAKER_AKTORID)
        Assert.assertEquals(kTredjepartsvarsel.orgnummer, VIRKSOMHETSNUMMER)
        Assert.assertNotNull(kTredjepartsvarsel.utsendelsestidspunkt)
    }
}
