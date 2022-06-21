package no.nav.syfo.varsel

import javax.inject.Inject
import no.nav.syfo.LocalApplication
import no.nav.syfo.consumer.aktorregister.AktorregisterConsumer
import no.nav.syfo.consumer.aktorregister.domain.AktorId
import no.nav.syfo.consumer.aktorregister.domain.Fodselsnummer
import no.nav.syfo.consumer.sts.StsConsumer
import no.nav.syfo.motebehov.MotebehovService
import no.nav.syfo.motebehov.motebehovstatus.MotebehovSkjemaType
import no.nav.syfo.motebehov.motebehovstatus.MotebehovStatusService
import no.nav.syfo.oppfolgingstilfelle.database.OppfolgingstilfelleDAO
import no.nav.syfo.testhelper.UserConstants.ARBEIDSTAKER_AKTORID
import no.nav.syfo.testhelper.UserConstants.ARBEIDSTAKER_FNR
import no.nav.syfo.testhelper.UserConstants.LEDER_AKTORID
import no.nav.syfo.testhelper.UserConstants.LEDER_FNR
import no.nav.syfo.testhelper.UserConstants.VEILEDER_ID
import no.nav.syfo.testhelper.UserConstants.VIRKSOMHETSNUMMER
import no.nav.syfo.testhelper.generator.MotebehovGenerator
import no.nav.syfo.testhelper.generator.generateKOversikthendelsetilfelle
import no.nav.syfo.testhelper.generator.generateMotebehovStatus
import no.nav.syfo.testhelper.generator.generatePersonOppfolgingstilfelleMeldBehovFirstPeriod
import no.nav.syfo.testhelper.generator.generatePersonOppfolgingstilfelleSvarBehov
import no.nav.syfo.testhelper.generator.generateStsToken
import no.nav.syfo.varsel.api.VarselController
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.HttpStatus
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.util.concurrent.ListenableFuture
import org.springframework.web.client.RestTemplate

@ExtendWith(SpringExtension::class)
@SpringBootTest(classes = [LocalApplication::class])
@DirtiesContext
class VarselLederComponentTest {

    @MockBean
    private lateinit var aktorregisterConsumer: AktorregisterConsumer

    @MockBean
    private lateinit var motebehovStatusService: MotebehovStatusService

    @MockBean
    private lateinit var motebehovService: MotebehovService

    @Inject
    private lateinit var oppfolgingstilfelleDAO: OppfolgingstilfelleDAO

    @Inject
    private lateinit var varselController: VarselController

    @Inject
    private lateinit var restTemplate: RestTemplate

    @MockBean
    private lateinit var kafkaTemplate: KafkaTemplate<String, Any>

    @MockBean
    private lateinit var stsConsumer: StsConsumer

    private lateinit var mockRestServiceServer: MockRestServiceServer

    private val motebehovGenerator = MotebehovGenerator()

    private val motebehovsvarVarselInfo = MotebehovsvarVarselInfo(
        sykmeldtAktorId = ARBEIDSTAKER_AKTORID,
        orgnummer = VIRKSOMHETSNUMMER,
        naermesteLederFnr = LEDER_FNR,
        arbeidstakerFnr = ARBEIDSTAKER_FNR
    )
    private val argumentCaptor = ArgumentCaptor.forClass(KTredjepartsvarsel::class.java)

    private val stsToken = generateStsToken().access_token

    @BeforeEach
    fun setUp() {
        cleanDB()
        mockRestServiceServer = MockRestServiceServer.bindTo(restTemplate).build()
        `when`(aktorregisterConsumer.getFnrForAktorId(AktorId(ARBEIDSTAKER_AKTORID)))
            .thenReturn(ARBEIDSTAKER_FNR)
        `when`(aktorregisterConsumer.getAktorIdForFodselsnummer(Fodselsnummer(ARBEIDSTAKER_FNR)))
            .thenReturn(ARBEIDSTAKER_FNR)
        `when`(stsConsumer.token()).thenReturn(stsToken)
    }

    @AfterEach
    fun tearDown() {
        mockRestServiceServer.reset()
        cleanDB()
    }

    @Test
    @Throws(Exception::class)
    fun sendVarselNaermesteLeder_skal_sende_varsel_til_NL_hvis_ikke_mote() {
        val oppfolgingstilfelle = generatePersonOppfolgingstilfelleSvarBehov
        oppfolgingstilfelleDAO.create(
            generateKOversikthendelsetilfelle.copy(
                fom = oppfolgingstilfelle.fom,
                tom = oppfolgingstilfelle.tom
            )
        )
        `when`(motebehovStatusService.motebehovStatus(oppfolgingstilfelle, emptyList()))
            .thenReturn(generateMotebehovStatus.copy(motebehov = null))

        `when`(
            kafkaTemplate.send(
                ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString(),
                ArgumentMatchers.any(KTredjepartsvarsel::class.java)
            )
        ).thenReturn(Mockito.mock(ListenableFuture::class.java) as ListenableFuture<SendResult<String, Any>>?)
        val returnertSvarFraVarselcontroller = varselController.sendVarselNaermesteLeder(motebehovsvarVarselInfo)
        Mockito.verify(kafkaTemplate).send(
            ArgumentMatchers.eq(TredjepartsvarselProducer.TREDJEPARTSVARSEL_TOPIC),
            ArgumentMatchers.anyString(),
            argumentCaptor.capture()
        )
        val sendtKTredjepartsvarsel = argumentCaptor.value
        verifySendtKtredjepartsvarsel(sendtKTredjepartsvarsel)
        assertEquals(HttpStatus.OK.value().toLong(), returnertSvarFraVarselcontroller.status.toLong())
    }

    @Test
    @Throws(Exception::class)
    fun sendVarselNaermesteLeder_no_varsel_oppfolgingstilfelle_meld_behov_no_meeting() {
        val oppfolgingstilfelle = generatePersonOppfolgingstilfelleMeldBehovFirstPeriod
        oppfolgingstilfelleDAO.create(
            generateKOversikthendelsetilfelle.copy(
                fom = oppfolgingstilfelle.fom,
                tom = oppfolgingstilfelle.tom
            )
        )
        `when`(motebehovStatusService.motebehovStatus(oppfolgingstilfelle, emptyList()))
            .thenReturn(
                generateMotebehovStatus.copy(
                    visMotebehov = true,
                    skjemaType = MotebehovSkjemaType.MELD_BEHOV,
                    motebehov = null
                )
            )

        val returnertSvarFraVarselcontroller = varselController.sendVarselNaermesteLeder(motebehovsvarVarselInfo)
        Mockito.verify(kafkaTemplate, Mockito.never())
            .send(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
        assertEquals(HttpStatus.OK.value().toLong(), returnertSvarFraVarselcontroller.status.toLong())
    }

    @Test
    @Throws(Exception::class)
    fun sendVarselNaermesteLeder_no_varsel_oppfolgingstilfelle_meld_behov() {
        val oppfolgingstilfelle = generatePersonOppfolgingstilfelleMeldBehovFirstPeriod
        oppfolgingstilfelleDAO.create(
            generateKOversikthendelsetilfelle.copy(
                fom = oppfolgingstilfelle.fom,
                tom = oppfolgingstilfelle.tom
            )
        )
        `when`(motebehovStatusService.motebehovStatus(oppfolgingstilfelle, emptyList()))
            .thenReturn(
                generateMotebehovStatus.copy(
                    visMotebehov = true,
                    skjemaType = MotebehovSkjemaType.MELD_BEHOV,
                    motebehov = null
                )
            )

        val returnertSvarFraVarselcontroller = varselController.sendVarselNaermesteLeder(motebehovsvarVarselInfo)
        Mockito.verify(kafkaTemplate, Mockito.never())
            .send(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
        assertEquals(HttpStatus.OK.value().toLong(), returnertSvarFraVarselcontroller.status.toLong())
    }

    @Test
    @Throws(Exception::class)
    fun sendVarselNaermesteLeder_no_varsel_oppfolgingstilfelle_svar_behov_motebehov_behandlet() {
        val oppfolgingstilfelle = generatePersonOppfolgingstilfelleSvarBehov
        oppfolgingstilfelleDAO.create(
            generateKOversikthendelsetilfelle.copy(
                fom = oppfolgingstilfelle.fom,
                tom = oppfolgingstilfelle.tom
            )
        )
        val newestMotebehov = motebehovGenerator.generateMotebehov().copy(
            opprettetAv = LEDER_AKTORID,
            behandletVeilederIdent = VEILEDER_ID
        )
        `when`(
            motebehovService.hentMotebehovListeForArbeidstakerOpprettetAvLeder(
                Fodselsnummer(ARBEIDSTAKER_FNR),
                VIRKSOMHETSNUMMER
            )
        )
            .thenReturn(listOf(newestMotebehov))
        `when`(
            motebehovStatusService.getNewestMotebehovInOppfolgingstilfelle(
                oppfolgingstilfelle,
                listOf(newestMotebehov)
            )
        )
            .thenReturn(newestMotebehov)
        `when`(motebehovStatusService.motebehovStatus(oppfolgingstilfelle, listOf(newestMotebehov)))
            .thenReturn(
                generateMotebehovStatus.copy(
                    visMotebehov = true,
                    skjemaType = MotebehovSkjemaType.MELD_BEHOV,
                    motebehov = null
                )
            )

        val returnertSvarFraVarselcontroller = varselController.sendVarselNaermesteLeder(motebehovsvarVarselInfo)
        Mockito.verify(kafkaTemplate, Mockito.never())
            .send(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
        assertEquals(HttpStatus.OK.value().toLong(), returnertSvarFraVarselcontroller.status.toLong())
    }

    @Test
    @Throws(Exception::class)
    fun sendVarselNaermesteLeder_no_varsel_oppfolgingstilfelle_svar_behov_motebehov_ubehandlet() {
        val oppfolgingstilfelle = generatePersonOppfolgingstilfelleSvarBehov
        oppfolgingstilfelleDAO.create(
            generateKOversikthendelsetilfelle.copy(
                fom = oppfolgingstilfelle.fom,
                tom = oppfolgingstilfelle.tom
            )
        )
        val newestMotebehov = motebehovGenerator.generateMotebehov().copy(
            opprettetAv = LEDER_AKTORID,
            behandletVeilederIdent = null
        )
        `when`(
            motebehovService.hentMotebehovListeForArbeidstakerOpprettetAvLeder(
                Fodselsnummer(ARBEIDSTAKER_FNR),
                VIRKSOMHETSNUMMER
            )
        )
            .thenReturn(listOf(newestMotebehov))
        `when`(
            motebehovStatusService.getNewestMotebehovInOppfolgingstilfelle(
                oppfolgingstilfelle,
                listOf(newestMotebehov)
            )
        )
            .thenReturn(newestMotebehov)
        `when`(motebehovStatusService.motebehovStatus(oppfolgingstilfelle, listOf(newestMotebehov)))
            .thenReturn(
                generateMotebehovStatus.copy(
                    visMotebehov = true,
                    skjemaType = MotebehovSkjemaType.MELD_BEHOV,
                    motebehov = null
                )
            )

        val returnertSvarFraVarselcontroller = varselController.sendVarselNaermesteLeder(motebehovsvarVarselInfo)
        Mockito.verify(kafkaTemplate, Mockito.never())
            .send(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
        assertEquals(HttpStatus.OK.value().toLong(), returnertSvarFraVarselcontroller.status.toLong())
    }

    @Test
    @Throws(Exception::class)
    fun sendVarselNaermesteLeder_skal_ikke_sende_varsel_til_NL_hvis_mote_finnes() {
        val oppfolgingstilfelle = generatePersonOppfolgingstilfelleSvarBehov
        oppfolgingstilfelleDAO.create(
            generateKOversikthendelsetilfelle.copy(
                fom = oppfolgingstilfelle.fom,
                tom = oppfolgingstilfelle.tom
            )
        )
        `when`(motebehovStatusService.motebehovStatus(oppfolgingstilfelle, emptyList()))
            .thenReturn(generateMotebehovStatus.copy(motebehov = null))

        val returnertSvarFraVarselcontroller = varselController.sendVarselNaermesteLeder(motebehovsvarVarselInfo)
        Mockito.verify(kafkaTemplate, Mockito.never())
            .send(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
        assertEquals(HttpStatus.OK.value().toLong(), returnertSvarFraVarselcontroller.status.toLong())

        mockRestServiceServer.verify()
    }

    private fun verifySendtKtredjepartsvarsel(kTredjepartsvarsel: KTredjepartsvarsel) {
        assertEquals(kTredjepartsvarsel.type, VarselType.NAERMESTE_LEDER_SVAR_MOTEBEHOV.name)
        assertNotNull(kTredjepartsvarsel.ressursId)
        assertEquals(kTredjepartsvarsel.aktorId, ARBEIDSTAKER_AKTORID)
        assertEquals(kTredjepartsvarsel.orgnummer, VIRKSOMHETSNUMMER)
        assertNotNull(kTredjepartsvarsel.utsendelsestidspunkt)
    }

    private fun cleanDB() {
        oppfolgingstilfelleDAO.nullstillOppfolgingstilfeller(Fodselsnummer(ARBEIDSTAKER_FNR))
    }
}
