package no.nav.syfo.varsel

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
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
import no.nav.syfo.testhelper.generator.*
import no.nav.syfo.varsel.api.VarselController
import no.nav.syfo.varsel.esyfovarsel.EsyfovarselProducer.Companion.ESYFOVARSEL_TOPIC
import no.nav.syfo.varsel.esyfovarsel.domain.EsyfovarselHendelse
import no.nav.syfo.varsel.esyfovarsel.domain.HendelseType
import no.nav.syfo.varsel.esyfovarsel.domain.NarmesteLederHendelse
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.TopicPartition
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.util.concurrent.SettableListenableFuture
import org.springframework.web.client.RestTemplate

@ExtendWith(SpringExtension::class)
@SpringBootTest(classes = [LocalApplication::class])
@DirtiesContext
class VarselLederComponentTest {

    @MockkBean
    private lateinit var aktorregisterConsumer: AktorregisterConsumer

    @MockkBean
    private lateinit var motebehovStatusService: MotebehovStatusService

    @MockkBean(relaxed = true)
    private lateinit var motebehovService: MotebehovService

    @Autowired
    private lateinit var oppfolgingstilfelleDAO: OppfolgingstilfelleDAO

    @Autowired
    private lateinit var varselController: VarselController

    @Autowired
    private lateinit var restTemplate: RestTemplate

    @MockkBean
    private lateinit var kafkaTemplate: KafkaTemplate<String, EsyfovarselHendelse>

    @MockkBean
    private lateinit var stsConsumer: StsConsumer

    private lateinit var mockRestServiceServer: MockRestServiceServer

    private val motebehovGenerator = MotebehovGenerator()

    private val motebehovsvarVarselInfo = MotebehovsvarVarselInfo(
        sykmeldtAktorId = ARBEIDSTAKER_AKTORID,
        orgnummer = VIRKSOMHETSNUMMER,
        naermesteLederFnr = LEDER_FNR,
        arbeidstakerFnr = ARBEIDSTAKER_FNR
    )

    private val stsToken = generateStsToken().access_token

    @BeforeEach
    fun setUp() {
        cleanDB()
        mockRestServiceServer = MockRestServiceServer.bindTo(restTemplate).build()
        every { aktorregisterConsumer.getFnrForAktorId(AktorId(ARBEIDSTAKER_AKTORID)) } returns ARBEIDSTAKER_FNR
        every { aktorregisterConsumer.getAktorIdForFodselsnummer(Fodselsnummer(ARBEIDSTAKER_FNR)) } returns ARBEIDSTAKER_AKTORID
        every { stsConsumer.token() } returns stsToken

        mockEsyfovarselHendelseFuture()
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

        every {
            motebehovStatusService.motebehovStatus(
                oppfolgingstilfelle,
                emptyList()
            )
        } returns generateMotebehovStatus.copy(motebehov = null)

        val returnertSvarFraVarselcontroller = varselController.sendVarselNaermesteLeder(motebehovsvarVarselInfo)

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

        every {
            motebehovStatusService.motebehovStatus(
                oppfolgingstilfelle,
                emptyList()
            )
        } returns generateMotebehovStatus.copy(
            visMotebehov = true,
            skjemaType = MotebehovSkjemaType.MELD_BEHOV,
            motebehov = null
        )

        val returnertSvarFraVarselcontroller = varselController.sendVarselNaermesteLeder(motebehovsvarVarselInfo)
        verify(exactly = 0) { kafkaTemplate.send(any(), any(), any()) }

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

        every {
            motebehovStatusService.motebehovStatus(
                oppfolgingstilfelle,
                emptyList()
            )
        } returns generateMotebehovStatus.copy(
            visMotebehov = true,
            skjemaType = MotebehovSkjemaType.MELD_BEHOV,
            motebehov = null
        )

        val returnertSvarFraVarselcontroller = varselController.sendVarselNaermesteLeder(motebehovsvarVarselInfo)
        verify(exactly = 0) { kafkaTemplate.send(any(), any(), any()) }

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
        every {
            motebehovService.hentMotebehovListeForArbeidstakerOpprettetAvLeder(
                Fodselsnummer(ARBEIDSTAKER_FNR),
                false,
                VIRKSOMHETSNUMMER
            )
        } returns listOf(newestMotebehov)

        every {
            motebehovStatusService.getNewestMotebehovInOppfolgingstilfelle(
                oppfolgingstilfelle,
                listOf(newestMotebehov)
            )
        } returns newestMotebehov

        every {
            motebehovStatusService.motebehovStatus(
                oppfolgingstilfelle,
                listOf(newestMotebehov)
            )
        } returns generateMotebehovStatus.copy(
            visMotebehov = true,
            skjemaType = MotebehovSkjemaType.MELD_BEHOV,
            motebehov = null
        )

        val returnertSvarFraVarselcontroller = varselController.sendVarselNaermesteLeder(motebehovsvarVarselInfo)
        verify(exactly = 0) {
            kafkaTemplate.send(any(), any(), any())
            assertEquals(HttpStatus.OK.value().toLong(), returnertSvarFraVarselcontroller.status.toLong())
        }
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
        every {
            motebehovService.hentMotebehovListeForArbeidstakerOpprettetAvLeder(
                Fodselsnummer(ARBEIDSTAKER_FNR),
                false,
                VIRKSOMHETSNUMMER
            )
        } returns listOf(newestMotebehov)

        every {
            motebehovStatusService.getNewestMotebehovInOppfolgingstilfelle(
                oppfolgingstilfelle,
                listOf(newestMotebehov)
            )
        } returns newestMotebehov

        every {
            motebehovStatusService.motebehovStatus(
                oppfolgingstilfelle,
                listOf(newestMotebehov)
            )
        } returns generateMotebehovStatus.copy(
            visMotebehov = true,
            skjemaType = MotebehovSkjemaType.MELD_BEHOV,
            motebehov = null
        )

        val returnertSvarFraVarselcontroller = varselController.sendVarselNaermesteLeder(motebehovsvarVarselInfo)
        verify(exactly = 0) { kafkaTemplate.send(any(), any(), any()) }
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
        every {
            motebehovStatusService.motebehovStatus(
                oppfolgingstilfelle,
                emptyList()
            )
        } returns generateMotebehovStatus.copy(motebehov = null)

        val returnertSvarFraVarselcontroller = varselController.sendVarselNaermesteLeder(motebehovsvarVarselInfo)
        verify(exactly = 0) { kafkaTemplate.send(any(), any(), any()) }
        assertEquals(HttpStatus.OK.value().toLong(), returnertSvarFraVarselcontroller.status.toLong())

        mockRestServiceServer.verify()
    }

    private fun cleanDB() {
        oppfolgingstilfelleDAO.nullstillOppfolgingstilfeller(Fodselsnummer(ARBEIDSTAKER_FNR))
    }

    private fun mockEsyfovarselHendelseFuture() {
        val hendelse = NarmesteLederHendelse(
            HendelseType.NL_DIALOGMOTE_SVAR_MOTEBEHOV,
            null,
            LEDER_FNR,
            ARBEIDSTAKER_FNR,
            VIRKSOMHETSNUMMER
        )
        val sendResult = SendResult<String, EsyfovarselHendelse>(
            ProducerRecord(ESYFOVARSEL_TOPIC, hendelse),
            RecordMetadata(
                TopicPartition(ESYFOVARSEL_TOPIC, 1), 0, 1, 0L, 1, 1
            )
        )
        val future = SettableListenableFuture<SendResult<String, EsyfovarselHendelse>>()
        val prodrec = ProducerRecord<String, EsyfovarselHendelse>(ESYFOVARSEL_TOPIC, hendelse)

        future.set(sendResult)
        every { kafkaTemplate.send(prodrec) } returns future
    }
}
