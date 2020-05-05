package no.nav.syfo.oppfolgingstilfelle

import no.nav.syfo.consumer.aktorregister.domain.Fodselsnummer
import no.nav.syfo.metric.Metric
import no.nav.syfo.oppfolgingstilfelle.database.OppfolgingstilfelleDAO
import no.nav.syfo.oppfolgingstilfelle.database.PPersonOppfolgingstilfelle
import no.nav.syfo.oppfolgingstilfelle.database.PersonOppfolgingstilfelle
import no.nav.syfo.oppfolgingstilfelle.kafka.KOversikthendelsetilfelle
import org.springframework.stereotype.Service
import javax.inject.Inject

@Service
class OppfolgingstilfelleService @Inject constructor(
        private val metric: Metric,
        private val oppfolgingstilfelleDAO: OppfolgingstilfelleDAO
) {
    fun receiveKOversikthendelsetilfelle(
            oversikthendelsetilfelle: KOversikthendelsetilfelle
    ) {
        val createNew = oppfolgingstilfelleDAO.get(Fodselsnummer(oversikthendelsetilfelle.fnr), oversikthendelsetilfelle.virksomhetsnummer).isEmpty();
        if (createNew) {
            oppfolgingstilfelleDAO.create(oversikthendelsetilfelle)
            metric.tellHendelse(METRIC_RECEIVE_OPPFOLGINGSTILFELLE_CREATE)
        } else {
            oppfolgingstilfelleDAO.update(oversikthendelsetilfelle)
            metric.tellHendelse(METRIC_RECEIVE_OPPFOLGINGSTILFELLE_UPDATE)
        }
    }

    fun getOppfolgingstilfeller(
            arbeidstakerFnr: Fodselsnummer,
            orgnummer: String
    ): List<PersonOppfolgingstilfelle> {
        return oppfolgingstilfelleDAO.get(arbeidstakerFnr, orgnummer).map {
            mapToPersonOppfolgingstilfelle(it)
        }
    }

    fun getOppfolgingstilfeller(
            arbeidstakerFnr: Fodselsnummer
    ): List<PersonOppfolgingstilfelle> {
        return oppfolgingstilfelleDAO.get(arbeidstakerFnr).map {
            mapToPersonOppfolgingstilfelle(it)
        }
    }

    private fun mapToPersonOppfolgingstilfelle(
            pPersonOppfolgingstilfelle: PPersonOppfolgingstilfelle
    ): PersonOppfolgingstilfelle {
        return PersonOppfolgingstilfelle(
                fnr = pPersonOppfolgingstilfelle.fnr,
                virksomhetsnummer = pPersonOppfolgingstilfelle.virksomhetsnummer,
                fom = pPersonOppfolgingstilfelle.fom,
                tom = pPersonOppfolgingstilfelle.tom
        )
    }

    companion object {
        private const val METRIC_RECEIVE_OPPFOLGINGSTILFELLE_BASE = "receive_oppfolgingstilfelle"
        private const val METRIC_RECEIVE_OPPFOLGINGSTILFELLE_CREATE = "${METRIC_RECEIVE_OPPFOLGINGSTILFELLE_BASE}_create"
        private const val METRIC_RECEIVE_OPPFOLGINGSTILFELLE_UPDATE = "${METRIC_RECEIVE_OPPFOLGINGSTILFELLE_BASE}_update"
    }
}
