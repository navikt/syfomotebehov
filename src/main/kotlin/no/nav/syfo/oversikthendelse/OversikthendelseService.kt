package no.nav.syfo.oversikthendelse

import no.nav.syfo.domain.rest.NyttMotebehov
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
        val kOversikthendelse = KOversikthendelse(
                fnr = fnr,
                hendelseId = OversikthendelseType.MOTEBEHOV_SVAR_BEHANDLET.name,
                enhetId = tildeltEnhet,
                tidspunkt = LocalDateTime.now()
        )
        oversikthendelseProducer.sendOversikthendelse(kOversikthendelse)
    }

    private fun nyttMotebehov2KOversikthendelse(nyttMotebehov: NyttMotebehov): KOversikthendelse {
        val kOversikthendelse = KOversikthendelse(
                fnr = nyttMotebehov.arbeidstakerFnr,
                hendelseId = OversikthendelseType.MOTEBEHOV_SVAR_MOTTATT.name,
                enhetId = nyttMotebehov.tildeltEnhet,
                tidspunkt = LocalDateTime.now()
        )
        return kOversikthendelse
    }
}
