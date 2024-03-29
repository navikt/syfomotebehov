package no.nav.syfo.motebehov.scheduler

import no.nav.syfo.leaderelection.LeaderElectionClient
import no.nav.syfo.motebehov.MotebehovService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDate
import javax.inject.Inject

@Component
class BehandleUbehandleteMotebehovScheduler @Inject constructor(
    private val leaderElectionClient: LeaderElectionClient,
    private val motebehovService: MotebehovService,
) {
    @Scheduled(cron = "0 0 11 * * *")
    fun runCleanupJob() {
        if (leaderElectionClient.isLeader()) {
            val dato = LocalDate.of(2023, 6, 1)
            val fakeVeilederIdent = "X000000"

            log.info("Running BehandleUbehandleteMotebehovScheduler job. Behandler ubehandlede møtebehov opprettet tidligere enn $dato")
            val updatedCount = motebehovService.behandleUbehandledeMotebehovOpprettetTidligereEnnDato(dato, fakeVeilederIdent)
            log.info("Behandlet $updatedCount ubehandlede møtebehov")
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(BehandleUbehandleteMotebehovScheduler::class.java)
    }
}
