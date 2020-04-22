package no.nav.syfo.oppfolgingstilfelle

import no.nav.syfo.oppfolgingstilfelle.database.OppfolgingstilfelleDAO
import no.nav.syfo.oppfolgingstilfelle.kafka.KOppfolgingstilfellePeker
import no.nav.syfo.oppfolgingstilfelle.syketilfelle.KOppfolgingstilfelle
import no.nav.syfo.oppfolgingstilfelle.syketilfelle.SyketilfelleConsumer
import no.nav.syfo.util.Metrikk
import org.springframework.stereotype.Service
import javax.inject.Inject

@Service
class OppfolgingstilfelleService @Inject constructor(
        private val metric: Metrikk,
        private val oppfolgingstilfelleDAO: OppfolgingstilfelleDAO,
        private val syketilfelleConsumer: SyketilfelleConsumer
) {
    fun receiveKOppfolgingstilfellePeker(
            oppfolgingstilfellePeker: KOppfolgingstilfellePeker
    ) {
        val oppfolgingstilfelle = syketilfelleConsumer.oppfolgingstilfelle(
                oppfolgingstilfellePeker.aktorId,
                oppfolgingstilfellePeker.orgnummer
        )
        if (oppfolgingstilfelle != null) {
            createOrUpdateOppfolgingstilfelle(oppfolgingstilfelle)
        }
    }

    fun createOrUpdateOppfolgingstilfelle(
            oppfolgingstilfelle: KOppfolgingstilfelle
    ) {
        val createNew = oppfolgingstilfelleDAO.get(oppfolgingstilfelle.aktorId, oppfolgingstilfelle.orgnummer).isEmpty();
        if (createNew) {
            oppfolgingstilfelleDAO.create(oppfolgingstilfelle)
            metric.tellHendelse(METRIC_RECEIVE_OPPFOLGINGSTILFELLE_CREATE)
        } else {
            oppfolgingstilfelleDAO.update(oppfolgingstilfelle)
            metric.tellHendelse(METRIC_RECEIVE_OPPFOLGINGSTILFELLE_UPDATE)
        }
    }

    companion object {
        private const val METRIC_RECEIVE_OPPFOLGINGSTILFELLE_BASE = "receive_oppfolgingstilfelle"
        private const val METRIC_RECEIVE_OPPFOLGINGSTILFELLE_CREATE = "${METRIC_RECEIVE_OPPFOLGINGSTILFELLE_BASE}_create"
        private const val METRIC_RECEIVE_OPPFOLGINGSTILFELLE_UPDATE = "${METRIC_RECEIVE_OPPFOLGINGSTILFELLE_BASE}_update"
    }
}
