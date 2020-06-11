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
        val createNew = oppfolgingstilfelleDAO.get(Fodselsnummer(oversikthendelsetilfelle.fnr), oversikthendelsetilfelle.virksomhetsnummer).isEmpty()
        if (createNew) {
            oppfolgingstilfelleDAO.create(oversikthendelsetilfelle)
            metric.tellHendelse(METRIC_RECEIVE_OPPFOLGINGSTILFELLE_CREATE)
        } else {
            oppfolgingstilfelleDAO.update(oversikthendelsetilfelle)
            metric.tellHendelse(METRIC_RECEIVE_OPPFOLGINGSTILFELLE_UPDATE)
        }
    }

    fun getActiveOppfolgingstilfeller(
        arbeidstakerFnr: Fodselsnummer
    ): List<PersonVirksomhetOppfolgingstilfelle> {
        return getPOppfolgingstilfellerInActiveOppfolgingstilfelle(arbeidstakerFnr).filter {
            it.isDateInOppfolgingstilfelle(LocalDate.now())
        }.map {
            it.mapToPersonVirksomhetOppfolgingstilfelle()
        }
    }

    fun getActiveOppfolgingstilfelleForArbeidsgiver(
        arbeidstakerFnr: Fodselsnummer,
        virksomhetsnummer: String
    ): PersonOppfolgingstilfelle? {
        val oppfolgingstilfelleList = getPOppfolgingstilfellerInActiveOppfolgingstilfelle(arbeidstakerFnr)
        val oppfolgingstilfelleVirksomhet = oppfolgingstilfelleList.find { it.virksomhetsnummer == virksomhetsnummer }
        return if (oppfolgingstilfelleVirksomhet != null && oppfolgingstilfelleVirksomhet.isDateInOppfolgingstilfelle(LocalDate.now())) {
            getActiveOppfolgingstilfelle(arbeidstakerFnr, oppfolgingstilfelleList)
        } else {
            null
        }
    }

    fun getActiveOppfolgingstilfelleForArbeidstaker(
        arbeidstakerFnr: Fodselsnummer
    ): PersonOppfolgingstilfelle? {
        return getActiveOppfolgingstilfelle(arbeidstakerFnr, getPOppfolgingstilfellerInActiveOppfolgingstilfelle(arbeidstakerFnr))
    }

    private fun getPOppfolgingstilfellerInActiveOppfolgingstilfelle(
        arbeidstakerFnr: Fodselsnummer
    ): List<PPersonOppfolgingstilfelle> {
        val oppfolgingstilfelleList = oppfolgingstilfelleDAO.get(arbeidstakerFnr)

        val activeOppfolgingstilfelleList = oppfolgingstilfelleList.filter {
            it.isDateInOppfolgingstilfelle(LocalDate.now())
        }
        val expiredOppfolgingstilfelleList = oppfolgingstilfelleList.filterNot {
            it.isDateInOppfolgingstilfelle(LocalDate.now())
        }
        return when {
            activeOppfolgingstilfelleList.isEmpty() -> {
                emptyList()
            }
            expiredOppfolgingstilfelleList.isEmpty() -> {
                activeOppfolgingstilfelleList
            }
            else -> {
                val expiredOverlappingOppfolgingstilfelleList = expiredOppfolgingstilfelleList.filter { expiredOppfolgingstilfelle ->
                    expiredOppfolgingstilfelle.tom.isAfter(activeOppfolgingstilfelleList.minBy { it.fom }!!.fom.minusDays(1))
                }
                activeOppfolgingstilfelleList.plus(expiredOverlappingOppfolgingstilfelleList)
            }
        }
    }

    private fun getActiveOppfolgingstilfelle(
        arbeidstakerFnr: Fodselsnummer,
        oppfolgingstilfelleList: List<PPersonOppfolgingstilfelle>
    ): PersonOppfolgingstilfelle? {
        val activeOppfolgingstilfeller = oppfolgingstilfelleList.map {
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
