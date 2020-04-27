package no.nav.syfo.oversikthendelse

import no.nav.syfo.aktorregister.domain.Fodselsnummer
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import javax.inject.Inject

@Service
class OversikthendelseService @Inject constructor(
        private val oversikthendelseProducer: OversikthendelseProducer
) {
    fun sendOversikthendelseMottatt(arbeidstakerFnr: Fodselsnummer, tildeltEnhet: String) {
        sendOversikthendelse(arbeidstakerFnr, tildeltEnhet, OversikthendelseType.MOTEBEHOV_SVAR_MOTTATT)
    }

    fun sendOversikthendelseBehandlet(arbeidstakerFnr: Fodselsnummer, tildeltEnhet: String) {
        sendOversikthendelse(arbeidstakerFnr, tildeltEnhet, OversikthendelseType.MOTEBEHOV_SVAR_BEHANDLET)
    }

    private fun sendOversikthendelse(fnr: Fodselsnummer, tildeltEnhet: String, oversikthendelseType: OversikthendelseType) {
        val kOversikthendelse = map2KOversikthendelse(fnr, tildeltEnhet, oversikthendelseType)
        oversikthendelseProducer.sendOversikthendelse(kOversikthendelse)
    }

    private fun map2KOversikthendelse(fnr: Fodselsnummer, tildeltEnhet: String, oversikthendelseType: OversikthendelseType): KOversikthendelse {
        return KOversikthendelse(
                fnr = fnr.value,
                hendelseId = oversikthendelseType.name,
                enhetId = tildeltEnhet,
                tidspunkt = LocalDateTime.now()
        )
    }
}
