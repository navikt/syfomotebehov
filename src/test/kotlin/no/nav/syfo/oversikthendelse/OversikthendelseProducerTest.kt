package no.nav.syfo.oversikthendelse

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.*
import no.nav.syfo.LocalApplication
import no.nav.syfo.personoppgavehendelse.PersonoppgavehendelseProducer
import no.nav.syfo.personoppgavehendelse.domain.KPersonoppgavehendelse
import no.nav.syfo.personoppgavehendelse.domain.PersonoppgavehendelseType
import no.nav.syfo.testhelper.UserConstants.ARBEIDSTAKER_FNR
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.util.concurrent.ListenableFuture

@ExtendWith(SpringExtension::class)
@SpringBootTest(classes = [LocalApplication::class])
@DirtiesContext
class OversikthendelseProducerTest {
    @MockkBean(relaxed = true)
    private lateinit var kafkaTemplate: KafkaTemplate<String, Any>

    @Autowired
    private lateinit var personoppgavehendelseProducer: PersonoppgavehendelseProducer

    @Test
    fun sendOversikthendelse() {
        every { kafkaTemplate.send(any(), any(), any()) } returns mockk<ListenableFuture<SendResult<String, Any>>>(
            relaxed = true
        )
        val kPersonoppgavehendelse = KPersonoppgavehendelse(
            personident = ARBEIDSTAKER_FNR,
            hendelsetype = PersonoppgavehendelseType.MOTEBEHOV_SVAR_MOTTATT.name
        )
        personoppgavehendelseProducer.sendPersonoppgavehendelse(kPersonoppgavehendelse, UUID.randomUUID())
        verify { kafkaTemplate.send(any(), any(), any()) }
    }

    @Test
    fun sendPersonoppgavehendelseMotebehovSvarBehandlet() {
        every { kafkaTemplate.send(any(), any(), any()) } returns mockk<ListenableFuture<SendResult<String, Any>>>(
            relaxed = true
        )
        val kPersonoppgavehendelse = KPersonoppgavehendelse(
            personident = ARBEIDSTAKER_FNR,
            hendelsetype = PersonoppgavehendelseType.MOTEBEHOV_SVAR_BEHANDLET.name
        )
        personoppgavehendelseProducer.sendPersonoppgavehendelse(kPersonoppgavehendelse, UUID.randomUUID())
        verify { kafkaTemplate.send(any(), any(), any()) }
    }
}
