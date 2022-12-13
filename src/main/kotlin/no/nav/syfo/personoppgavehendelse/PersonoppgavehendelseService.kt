package no.nav.syfo.personoppgavehendelse

import java.util.*
import javax.inject.Inject
import no.nav.syfo.personoppgavehendelse.domain.KPersonoppgavehendelse
import no.nav.syfo.personoppgavehendelse.domain.PersonoppgavehendelseType
import org.springframework.stereotype.Service

@Service
class PersonoppgavehendelseService @Inject constructor(
    private val personoppgavehendelseProducer: PersonoppgavehendelseProducer
) {
    fun sendPersonoppgaveHendelseMottatt(
        motebehovUUID: UUID,
        arbeidstakerFnr: String,
    ) {
        sendPersonoppgaveHendelse(motebehovUUID, arbeidstakerFnr, PersonoppgavehendelseType.MOTEBEHOV_SVAR_MOTTATT)
    }

    fun sendPersonoppgaveHendelseBehandlet(
        motebehovUUID: UUID,
        arbeidstakerFnr: String,
    ) {
        sendPersonoppgaveHendelse(motebehovUUID, arbeidstakerFnr, PersonoppgavehendelseType.MOTEBEHOV_SVAR_BEHANDLET)
    }

    private fun sendPersonoppgaveHendelse(
        motebehovUUID: UUID,
        fnr: String,
        personoppgaveHendelseType: PersonoppgavehendelseType
    ) {
        val hendelse = mapToKPersonoppgavehendelse(fnr, personoppgaveHendelseType)
        personoppgavehendelseProducer.sendPersonoppgavehendelse(motebehovUUID, hendelse)
    }

    private fun mapToKPersonoppgavehendelse(
        fnr: String,
        personoppgaveHendelseType: PersonoppgavehendelseType
    ) = KPersonoppgavehendelse(
        personident = fnr,
        hendelsetype = personoppgaveHendelseType.name,
    )
}
