package no.nav.syfo.oversikthendelse

import no.nav.syfo.domain.rest.NyttMotebehov
import no.nav.syfo.kafka.producer.model.KOversikthendelse
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import javax.inject.Inject

@Service
class OversikthendelseService @Inject constructor(
        private val oversikthendelseProducer: OversikthendelseProducer
) {
    fun sendOversikthendelse(nyttMotebehov: NyttMotebehov) {
        val kOversikthendelse = nyttMotebehov2KOversikthendelse(nyttMotebehov)
        oversikthendelseProducer.sendOversikthendelse(kOversikthendelse)
    }

    fun sendOversikthendelse(fnr: String, tildeltEnhet: String) {
        val kOversikthendelse = KOversikthendelse()
        kOversikthendelse.fnr = fnr
        kOversikthendelse.hendelseId = OversikthendelseType.MOTEBEHOV_SVAR_BEHANDLET.name
        kOversikthendelse.enhetId = tildeltEnhet
        kOversikthendelse.tidspunkt = LocalDateTime.now()

        oversikthendelseProducer.sendOversikthendelse(kOversikthendelse)
    }

    private fun nyttMotebehov2KOversikthendelse(nyttMotebehov: NyttMotebehov): KOversikthendelse {
        val kOversikthendelse = KOversikthendelse()
        kOversikthendelse.fnr = nyttMotebehov.arbeidstakerFnr
        kOversikthendelse.hendelseId = OversikthendelseType.MOTEBEHOV_SVAR_MOTTATT.name
        kOversikthendelse.enhetId = nyttMotebehov.tildeltEnhet
        kOversikthendelse.tidspunkt = LocalDateTime.now()
        return kOversikthendelse
    }
}
