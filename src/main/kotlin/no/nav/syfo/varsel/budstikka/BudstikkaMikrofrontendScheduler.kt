package no.nav.syfo.varsel.budstikka

import no.nav.syfo.leaderelection.LeaderElectionClient
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
@Profile("remote")
class BudstikkaMikrofrontendScheduler(
    private val leaderElectionClient: LeaderElectionClient,
    private val budstikkaMikrofrontendService: BudstikkaMikrofrontendService,
) {
    @Scheduled(fixedDelay = 10_000L)
    fun run() {
        if (!leaderElectionClient.isLeader()) return

        budstikkaMikrofrontendService.sendVentendeKommandoer()
    }

    @Scheduled(fixedDelay = 3_600_000L)
    fun runCleanUp() {
        if (!leaderElectionClient.isLeader()) return

        budstikkaMikrofrontendService.ryddOpp()
    }
}
