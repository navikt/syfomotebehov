package no.nav.syfo.dialogmotekandidat

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tags
import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.syfo.dialogmotekandidat.database.DialogmotekandidatVarselStatus
import no.nav.syfo.dialogmotekandidat.database.DialogmotekandidatVarselStatusDao
import no.nav.syfo.dialogmotekandidat.database.DialogmotekandidatVarselType
import no.nav.syfo.leaderelection.LeaderElectionClient
import no.nav.syfo.varsel.VarselServiceV2
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject

@Component
@Profile("remote")
class DialogmotekandidatVarselScheduler
    @Inject
    constructor(
        private val leaderElectionClient: LeaderElectionClient,
        private val varselStatusDao: DialogmotekandidatVarselStatusDao,
        private val varselServiceV2: VarselServiceV2,
        meterRegistry: MeterRegistry,
    ) {
        private val varselPendingOver1Day = AtomicLong(0)
        private val ferdigstillPendingOver1Day = AtomicLong(0)

        init {
            meterRegistry.gauge(METRIC_PENDING_OVER_1D, Tags.of("type", "VARSEL"), varselPendingOver1Day)
            meterRegistry.gauge(METRIC_PENDING_OVER_1D, Tags.of("type", "FERDIGSTILL"), ferdigstillPendingOver1Day)
        }

        @Scheduled(fixedDelay = 60_000L)
        fun run() {
            if (!leaderElectionClient.isLeader()) return

            sendPendingVarsler()
            ferdigstillPendingVarsler()
            updateGauges()
        }

        @Scheduled(fixedDelay = 3_600_000L)
        fun runCleanUp() {
            if (!leaderElectionClient.isLeader()) return

            cleanUp()
        }

        internal fun sendPendingVarsler() {
            varselStatusDao
                .getPendingByType(DialogmotekandidatVarselType.VARSEL)
                .forEach { processPendingVarsler(it) }
        }

        private fun processPendingVarsler(row: DialogmotekandidatVarselStatus) {
            runCatching {
                varselServiceV2.sendSvarBehovVarsel(row.fnr, row.kafkaMeldingUuid)
                varselStatusDao.updateStatusToSent(row.id)
            }.onSuccess {
                log.info(
                    "Varsel sendt",
                    kv("event", "dialogmotekandidat.varsel.sent"),
                    kv("id", row.id),
                    kv("messageId", row.kafkaMeldingUuid),
                )
            }.onFailure { e ->
                varselStatusDao.incrementRetryCount(row.id)
                log.warn(
                    "Feil ved sending av varsel",
                    kv("event", "dialogmotekandidat.varsel.retry"),
                    kv("id", row.id),
                    kv("messageId", row.kafkaMeldingUuid),
                    kv("retryCount", row.retryCount + 1),
                    e,
                )
            }
        }

        internal fun ferdigstillPendingVarsler() {
            varselStatusDao
                .getPendingByType(DialogmotekandidatVarselType.FERDIGSTILL)
                .forEach { processFerdigstillVarsler(it) }
        }

        private fun processFerdigstillVarsler(row: DialogmotekandidatVarselStatus) {
            runCatching {
                varselServiceV2.ferdigstillSvarMotebehovVarsel(row.fnr)
                varselStatusDao.updateStatusToSent(row.id)
            }.onSuccess {
                log.info(
                    "Ferdigstilt",
                    kv("event", "dialogmotekandidat.ferdigstill.sent"),
                    kv("id", row.id),
                    kv("messageId", row.kafkaMeldingUuid),
                )
            }.onFailure { e ->
                varselStatusDao.incrementRetryCount(row.id)
                log.warn(
                    "Feil ved ferdigstilling",
                    kv("event", "dialogmotekandidat.ferdigstill.retry"),
                    kv("id", row.id),
                    kv("messageId", row.kafkaMeldingUuid),
                    kv("retryCount", row.retryCount + 1),
                    e,
                )
            }
        }

        internal fun updateGauges() {
            val cutoff = LocalDateTime.now().minusDays(1)
            varselPendingOver1Day.set(varselStatusDao.countPendingOlderThan(DialogmotekandidatVarselType.VARSEL, cutoff).toLong())
            ferdigstillPendingOver1Day.set(varselStatusDao.countPendingOlderThan(DialogmotekandidatVarselType.FERDIGSTILL, cutoff).toLong())
        }

        internal fun cleanUp() {
            val now = LocalDateTime.now()
            val sentDeleted = varselStatusDao.deleteSentOlderThan(now.minusMonths(1))
            val pendingDeleted = varselStatusDao.deletePendingOlderThan(now.minusWeeks(2))
            log.info(
                "Ryddet opp",
                kv("event", "dialogmotekandidat.cleanup"),
                kv("sentDeleted", sentDeleted),
                kv("pendingDeleted", pendingDeleted),
            )
        }

        companion object {
            private val log = LoggerFactory.getLogger(DialogmotekandidatVarselScheduler::class.java)
            private const val METRIC_PENDING_OVER_1D = "dialogkandidat_varsel_pending_over_1d_total"
        }
    }
