package no.nav.syfo.oversikthendelse

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.syfo.LocalApplication
import no.nav.syfo.testhelper.UserConstants.ARBEIDSTAKER_FNR
import no.nav.syfo.testhelper.UserConstants.NAV_ENHET
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.util.concurrent.ListenableFuture
import java.time.LocalDateTime
import java.util.*

@ExtendWith(SpringExtension::class)
@SpringBootTest(classes = [LocalApplication::class])
@DirtiesContext
class OversikthendelseProducerTest {
    @MockkBean(relaxed = true)
    private lateinit var kafkaTemplate: KafkaTemplate<String, Any>

    @Autowired
    private lateinit var oversikthendelseProducer: OversikthendelseProducer

    @Test
    fun sendOversikthendelse() {
        every { kafkaTemplate.send(any(), any(), any()) } returns mockk<ListenableFuture<SendResult<String, Any>>>(
            relaxed = true
        )
        val kOversikthendelse = KOversikthendelse(
            fnr = ARBEIDSTAKER_FNR,
            hendelseId = OversikthendelseType.MOTEBEHOV_SVAR_MOTTATT.name,
            enhetId = NAV_ENHET,
            tidspunkt = LocalDateTime.now()
        )
        oversikthendelseProducer.sendOversikthendelse(UUID.randomUUID(), kOversikthendelse)
        verify { kafkaTemplate.send(any(), any(), any()) }
    }

    @Test
    fun sendOversikthendelseMotebehovSvarBehandlet() {
        every { kafkaTemplate.send(any(), any(), any()) } returns mockk<ListenableFuture<SendResult<String, Any>>>(
            relaxed = true
        )
        val kOversikthendelse = KOversikthendelse(
            fnr = ARBEIDSTAKER_FNR,
            hendelseId = OversikthendelseType.MOTEBEHOV_SVAR_BEHANDLET.name,
            enhetId = NAV_ENHET,
            tidspunkt = LocalDateTime.now()
        )
        oversikthendelseProducer.sendOversikthendelse(UUID.randomUUID(), kOversikthendelse)
        verify { kafkaTemplate.send(any(), any(), any()) }
    }
}
