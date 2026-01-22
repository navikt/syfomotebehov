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
    private val fakeVeilederIdent = "X000000"

    @Scheduled(cron = "0 0 11 * * *")
    fun runCleanupJobForOldMotebehov() {
        if (leaderElectionClient.isLeader()) {
            log.info(
                "Running BehandleUbehandleteMotebehovScheduler job. " +
                    "Behandler ubehandlede møtebehov opprettet tidligere enn $cutoffDatoBehandleTidligere"
            )
            val updatedCount = motebehovService.behandleUbehandledeMotebehovOpprettetTidligereEnnDato(
                cutoffDatoBehandleTidligere,
                fakeVeilederIdent
            )
            log.info("Behandlet $updatedCount ubehandlede møtebehov")
        }
    }

//  @Scheduled(cron = "0 0 11 * * *", scheduler = Scheduled.CRON_DISABLED)
    @Scheduled(cron = "0 40 10 * * *")
    fun runCleanupJobForSpecificMotebehovIds() {
        if (leaderElectionClient.isLeader()) {
            log.info(
                "Running BehandleUbehandleteMotebehovScheduler job. Behandler ubehandlede møtebehov med bestemte IDer."
            )

            val updatedCount = motebehovIdsToBehandle.count { motebehovId ->
                motebehovService.behandleUbehandletMotebehovMedId(motebehovId, fakeVeilederIdent)
            }

            log.info("Behandlet $updatedCount ubehandlede møtebehov")
        }
    }

    companion object {
        // Behandle ubehandlede møtebehov opprettet før denne datoen
        private val cutoffDatoBehandleTidligere = LocalDate.of(2023, 6, 1)

        // Behandle ubehandlede møtebehov med disse IDene
        private val motebehovIdsToBehandle = listOf<String>(
            "d9c3c95e-305b-4a15-85f9-09093159fc18",
        )

        private val log = LoggerFactory.getLogger(BehandleUbehandleteMotebehovScheduler::class.java)
    }
}
