package no.nav.syfo.motebehov.scheduler

import no.nav.syfo.leaderelection.LeaderElectionService
import no.nav.syfo.motebehov.MotebehovService
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDate
import javax.inject.Inject

@Component
@Profile("behandle_ubehandlede_motebehov")
class BehandleUbehandleteMotebehovScheduler @Inject constructor(
    private val leaderElectionService: LeaderElectionService,
    private val motebehovService: MotebehovService,
) {
    @Scheduled(cron = "0 15 9 3 OCT ?")
    fun runCleanupJob() {
        log.info("Running BehandleUbehandleteMotebehovScheduler job")
        if (leaderElectionService.isLeader()) {
            val dato = LocalDate.of(2023, 6, 1)
            val fakeVeilederIdent = "X000000"

            log.info("Behandler ubehandlede møtebehov opprettet tidligere enn $dato")
            motebehovService.behandleUbehandledeMotebehovOpprettetTidligereEnnDato(dato, fakeVeilederIdent)
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(BehandleUbehandleteMotebehovScheduler::class.java)
    }
}
