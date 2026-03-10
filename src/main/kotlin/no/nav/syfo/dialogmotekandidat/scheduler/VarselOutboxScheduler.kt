package no.nav.syfo.dialogmotekandidat.scheduler

import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import no.nav.syfo.dialogmotekandidat.database.DialogmotekandidatDAO
import no.nav.syfo.dialogmotekandidat.database.VarselOutboxDao
import no.nav.syfo.dialogmotekandidat.database.VarselOutboxStatus
import no.nav.syfo.dialogmotekandidat.kafka.KafkaDialogmotekandidatEndring
import no.nav.syfo.dialogmotekandidat.kafka.configuredJacksonMapper
import no.nav.syfo.leaderelection.LeaderElectionClient
import no.nav.syfo.util.toNorwegianLocalDateTime
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

@Component
class VarselOutboxScheduler @Inject constructor(
    private val leaderElectionClient: LeaderElectionClient,
    private val varselOutboxDao: VarselOutboxDao,
    private val varselOutboxRecipientService: VarselOutboxRecipientService,
    private val dialogmotekandidatDAO: DialogmotekandidatDAO,
    meterRegistry: MeterRegistry,
) {
    private val stuckPendingGauge = AtomicInteger(0)

    init {
        Gauge.builder("varsel_outbox_pending_stuck_count", stuckPendingGauge) { it.get().toDouble() }
            .description("Number of PENDING varsel outbox entries older than $STUCK_ALERT_HOURS hours")
            .register(meterRegistry)
    }

    @Scheduled(fixedDelay = 60_000)
    fun run() {
        if (!leaderElectionClient.isLeader()) return
        processPendingOutboxEntries()
        varselOutboxRecipientService.sendPendingRecipients()
        updateStuckGauge()
    }

    private fun updateStuckGauge() {
        val stuckEntries = varselOutboxDao.getPending()
            .filter { it.createdAt.isBefore(LocalDateTime.now().minusHours(STUCK_ALERT_HOURS)) }
        stuckPendingGauge.set(stuckEntries.size)
        if (stuckEntries.isNotEmpty()) {
            log.warn(
                "${stuckEntries.size} PENDING varsel outbox entries er eldre enn $STUCK_ALERT_HOURS timer — " +
                    "mulig stuck entries: ${stuckEntries.map { it.uuid }}"
            )
        }
    }

    private fun processPendingOutboxEntries() {
        varselOutboxDao.getPending().forEach { entry ->
            try {
                processOutboxEntry(entry)
            } catch (e: Exception) {
                log.error("Feil ved prosessering av outbox-entry ${entry.uuid}, prøver igjen neste kjøring", e)
            }
        }
    }

    private fun processOutboxEntry(entry: no.nav.syfo.dialogmotekandidat.database.VarselOutboxEntry) {
        if (entry.createdAt.isBefore(LocalDateTime.now().minusDays(MAX_AGE_DAYS))) {
            log.warn("Outbox-entry ${entry.uuid} er eldre enn $MAX_AGE_DAYS dager, setter til SKIPPED")
            varselOutboxDao.updateStatus(entry.uuid, VarselOutboxStatus.SKIPPED)
            return
        }

        val endring = objectMapper.readValue(entry.payload, KafkaDialogmotekandidatEndring::class.java)

        val staleReason = staleReason(endring)
        if (staleReason != null) {
            log.info("Outbox-entry ${entry.uuid} er utdatert — $staleReason, setter til SKIPPED")
            varselOutboxDao.updateStatus(entry.uuid, VarselOutboxStatus.SKIPPED)
            return
        }

        varselOutboxRecipientService.expandAndSaveRecipients(entry, endring)
        varselOutboxDao.updateStatus(entry.uuid, VarselOutboxStatus.PROCESSED)
    }

    private fun staleReason(endring: KafkaDialogmotekandidatEndring): String? {
        val current = dialogmotekandidatDAO.get(endring.personIdentNumber)
        val endringCreatedAt = endring.createdAt.toNorwegianLocalDateTime()
        return when {
            endring.kandidat && (current == null || !current.kandidat) ->
                "person er ikke lenger kandidat"
            !endring.kandidat && current != null && current.kandidat ->
                "person er kandidat igjen"
            !endring.kandidat && current != null && !current.kandidat && current.createdAt.isAfter(endringCreatedAt) ->
                "nyere ferdigstill-hendelse er allerede behandlet"
            else -> null
        }
    }

    companion object {
        private const val MAX_AGE_DAYS = 7L
        private const val STUCK_ALERT_HOURS = 24L
        private val log = LoggerFactory.getLogger(VarselOutboxScheduler::class.java)
        private val objectMapper = configuredJacksonMapper()
    }
}
