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
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.TransactionTemplate
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
        transactionManager: PlatformTransactionManager,
        meterRegistry: MeterRegistry,
    ) {
        private val varselPendingOver1Day = AtomicLong(0)
        private val ferdigstillPendingOver1Day = AtomicLong(0)
        private val transactionTemplate =
            TransactionTemplate(transactionManager).apply {
                propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW
            }

        init {
            meterRegistry.gauge(METRIC_PENDING_OVER_1D, Tags.of("type", "VARSEL"), varselPendingOver1Day)
            meterRegistry.gauge(METRIC_PENDING_OVER_1D, Tags.of("type", "FERDIGSTILL"), ferdigstillPendingOver1Day)
        }

        @Scheduled(fixedDelay = 60_000L)
        fun run() {
            if (!leaderElectionClient.isLeader()) return

            sendPendingVarsler()
            ferdigstillPendingVarsler()
            logGivenUpRows()
            updateGauges()
        }

        @Scheduled(fixedDelay = 3_600_000L)
        fun runCleanUp() {
            if (!leaderElectionClient.isLeader()) return

            cleanUp()
        }

        internal fun sendPendingVarsler() {
            processPendingVarsler(DialogmotekandidatVarselType.VARSEL) { row ->
                if (varselStatusDao.hasPendingFerdigstillForFnr(row.fnr)) {
                    log.info(
                        "Skipper varsel fordi pending ferdigstill finnes",
                        kv("event", "dialogmotekandidat.varsel.skipped_has_ferdigstill"),
                        kv("id", row.id),
                        kv("messageId", row.kafkaMeldingUuid),
                    )
                    return@processPendingVarsler
                }
                varselServiceV2.sendSvarBehovVarsel(row.fnr, row.kafkaMeldingUuid)
            }
        }

        internal fun ferdigstillPendingVarsler() {
            processPendingVarsler(DialogmotekandidatVarselType.FERDIGSTILL) { row ->
                varselServiceV2.ferdigstillSvarMotebehovVarsel(row.fnr)
            }
        }

        private fun processPendingVarsler(
            type: DialogmotekandidatVarselType,
            action: (DialogmotekandidatVarselStatus) -> Unit,
        ) {
            var processedCount = 0
            while (true) {
                if (processedCount >= MAX_ROWS_PER_TICK) {
                    log.info(
                        "Nådde maks antall rader per tick",
                        kv("event", "dialogmotekandidat.varsel.batch_limit_reached"),
                        kv("type", type.name),
                        kv("count", processedCount),
                    )
                    return
                }
                val processed =
                    transactionTemplate.execute<Boolean> {
                        val row = varselStatusDao.getPendingByType(type, limit = 1).firstOrNull() ?: return@execute false
                        runCatching {
                            action(row)
                            if (varselStatusDao.updateStatusToSent(row.id)) {
                                log.info(
                                    when (type) {
                                        DialogmotekandidatVarselType.VARSEL -> "Varsel sendt"
                                        DialogmotekandidatVarselType.FERDIGSTILL -> "Ferdigstilt"
                                    },
                                    kv(
                                        "event",
                                        when (type) {
                                            DialogmotekandidatVarselType.VARSEL -> "dialogmotekandidat.varsel.sent"
                                            DialogmotekandidatVarselType.FERDIGSTILL -> "dialogmotekandidat.ferdigstill.sent"
                                        },
                                    ),
                                    kv("id", row.id),
                                    kv("messageId", row.kafkaMeldingUuid),
                                )
                            }
                        }.onFailure { e ->
                            varselStatusDao.incrementRetryCount(row.id)
                            log.warn(
                                when (type) {
                                    DialogmotekandidatVarselType.VARSEL -> "Feil ved sending av varsel"
                                    DialogmotekandidatVarselType.FERDIGSTILL -> "Feil ved ferdigstilling"
                                },
                                kv(
                                    "event",
                                    when (type) {
                                        DialogmotekandidatVarselType.VARSEL -> "dialogmotekandidat.varsel.retry"
                                        DialogmotekandidatVarselType.FERDIGSTILL -> "dialogmotekandidat.ferdigstill.retry"
                                    },
                                ),
                                kv("id", row.id),
                                kv("messageId", row.kafkaMeldingUuid),
                                kv("retryCount", row.retryCount + 1),
                                e,
                            )
                        }
                        true
                    } == true

                if (!processed) {
                    return
                }
                processedCount++
            }
        }

        internal fun updateGauges() {
            val cutoff = LocalDateTime.now().minusDays(1)
            varselPendingOver1Day.set(varselStatusDao.countPendingOlderThan(DialogmotekandidatVarselType.VARSEL, cutoff).toLong())
            ferdigstillPendingOver1Day.set(varselStatusDao.countPendingOlderThan(DialogmotekandidatVarselType.FERDIGSTILL, cutoff).toLong())
        }

        internal fun logGivenUpRows() {
            val givenUpCount = varselStatusDao.countGivenUp()
            if (givenUpCount > 0) {
                log.error(
                    "PENDING-rader har overskredet maks antall retries",
                    kv("event", "dialogmotekandidat.varsel.given_up"),
                    kv("count", givenUpCount),
                )
            }
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
            private const val MAX_ROWS_PER_TICK = 100
            private const val METRIC_PENDING_OVER_1D = "dialogkandidat_varsel_pending_over_1d_total"
        }
    }
