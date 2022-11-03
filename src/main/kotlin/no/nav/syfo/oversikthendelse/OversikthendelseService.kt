package no.nav.syfo.oversikthendelse

import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.*
import javax.inject.Inject

@Service
class OversikthendelseService @Inject constructor(
    private val oversikthendelseProducer: OversikthendelseProducer
) {
    fun sendOversikthendelseMottatt(
        motebehovUUID: UUID,
        arbeidstakerFnr: String,
        tildeltEnhet: String
    ) {
        sendOversikthendelse(motebehovUUID, arbeidstakerFnr, tildeltEnhet, OversikthendelseType.MOTEBEHOV_SVAR_MOTTATT)
    }

    fun sendOversikthendelseBehandlet(
        motebehovUUID: UUID,
        arbeidstakerFnr: String,
        tildeltEnhet: String
    ) {
        sendOversikthendelse(motebehovUUID, arbeidstakerFnr, tildeltEnhet, OversikthendelseType.MOTEBEHOV_SVAR_BEHANDLET)
    }

    private fun sendOversikthendelse(
        motebehovUUID: UUID,
        fnr: String,
        tildeltEnhet: String,
        oversikthendelseType: OversikthendelseType
    ) {
        val kOversikthendelse = map2KOversikthendelse(fnr, tildeltEnhet, oversikthendelseType)
        oversikthendelseProducer.sendOversikthendelse(motebehovUUID, kOversikthendelse)
    }

    private fun map2KOversikthendelse(
        fnr: String,
        tildeltEnhet: String,
        oversikthendelseType: OversikthendelseType
    ) = KOversikthendelse(
        fnr = fnr,
        hendelseId = oversikthendelseType.name,
        enhetId = tildeltEnhet,
        tidspunkt = LocalDateTime.now()
    )
}
