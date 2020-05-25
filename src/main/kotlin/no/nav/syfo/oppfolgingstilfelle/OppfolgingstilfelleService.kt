package no.nav.syfo.oppfolgingstilfelle

import no.nav.syfo.consumer.aktorregister.domain.Fodselsnummer
import no.nav.syfo.metric.Metric
import no.nav.syfo.oppfolgingstilfelle.database.*
import no.nav.syfo.oppfolgingstilfelle.kafka.KOversikthendelsetilfelle
import org.springframework.stereotype.Service
import java.time.LocalDate
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

    private fun getActivePOppfolgingstilfeller(
            arbeidstakerFnr: Fodselsnummer
    ): List<PPersonOppfolgingstilfelle> {
        return oppfolgingstilfelleDAO.get(arbeidstakerFnr).filter {
            it.isDateInOppfolgingstilfelle(LocalDate.now())
        }
    }

    fun getActiveOppfolgingstilfeller(
            arbeidstakerFnr: Fodselsnummer
    ): List<PersonVirksomhetOppfolgingstilfelle> {
        return getActivePOppfolgingstilfeller(arbeidstakerFnr).map {
            it.mapToPersonVirksomhetOppfolgingstilfelle()
        }
    }

    fun getActiveOppfolgingstilfelle(
            arbeidstakerFnr: Fodselsnummer
    ): PersonOppfolgingstilfelle? {
        val activeOppfolgingstilfeller = getActivePOppfolgingstilfeller(arbeidstakerFnr).map {
            it.mapToPersonOppfolgingstilfelle()
        }
        return if (activeOppfolgingstilfeller.isNotEmpty()) {
            if (activeOppfolgingstilfeller.size > 1) {
                val minFom = activeOppfolgingstilfeller.minBy { it.fom }!!.fom
                val maxTom = activeOppfolgingstilfeller.maxBy { it.tom }!!.tom
                PersonOppfolgingstilfelle(
                        fnr = arbeidstakerFnr,
                        fom = minFom,
                        tom = maxTom
                )
            } else {
                activeOppfolgingstilfeller[0]
            }
        } else {
            null
        }
    }

    companion object {
        private const val METRIC_RECEIVE_OPPFOLGINGSTILFELLE_BASE = "receive_oppfolgingstilfelle"
        private const val METRIC_RECEIVE_OPPFOLGINGSTILFELLE_CREATE = "${METRIC_RECEIVE_OPPFOLGINGSTILFELLE_BASE}_create"
        private const val METRIC_RECEIVE_OPPFOLGINGSTILFELLE_UPDATE = "${METRIC_RECEIVE_OPPFOLGINGSTILFELLE_BASE}_update"
    }
}
