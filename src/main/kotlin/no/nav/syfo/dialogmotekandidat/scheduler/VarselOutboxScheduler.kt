package no.nav.syfo.dialogmotekandidat.scheduler

import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import no.nav.syfo.consumer.narmesteleder.NarmesteLederRelasjonDTO
import no.nav.syfo.consumer.narmesteleder.NarmesteLederService
import no.nav.syfo.dialogmote.database.DialogmoteDAO
import no.nav.syfo.dialogmotekandidat.database.*
import no.nav.syfo.dialogmotekandidat.database.DialogmotekandidatDAO
import no.nav.syfo.dialogmotekandidat.database.VarselOutboxStatus
import no.nav.syfo.dialogmotekandidat.kafka.KafkaDialogmotekandidatEndring
import no.nav.syfo.dialogmotekandidat.kafka.configuredJacksonMapper
import no.nav.syfo.leaderelection.LeaderElectionClient
import no.nav.syfo.util.toNorwegianLocalDateTime
import no.nav.syfo.motebehov.MotebehovService
import no.nav.syfo.motebehov.motebehovstatus.MotebehovStatusHelper
import no.nav.syfo.oppfolgingstilfelle.OppfolgingstilfelleService
import no.nav.syfo.varsel.esyfovarsel.EsyfovarselProducer
import no.nav.syfo.varsel.esyfovarsel.domain.*
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

@Component
class VarselOutboxScheduler @Inject constructor(
    private val leaderElectionClient: LeaderElectionClient,
    private val varselOutboxDao: VarselOutboxDao,
    private val varselOutboxRecipientDao: VarselOutboxRecipientDao,
    private val dialogmotekandidatDAO: DialogmotekandidatDAO,
    private val narmesteLederService: NarmesteLederService,
    private val oppfolgingstilfelleService: OppfolgingstilfelleService,
    private val dialogmoteDAO: DialogmoteDAO,
    private val motebehovService: MotebehovService,
    private val motebehovStatusHelper: MotebehovStatusHelper,
    private val esyfovarselProducer: EsyfovarselProducer,
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
        sendPendingRecipients()
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

    private fun processOutboxEntry(entry: VarselOutboxEntry) {
        if (entry.createdAt.isBefore(LocalDateTime.now().minusDays(MAX_AGE_DAYS))) {
            log.warn("Outbox-entry ${entry.uuid} er eldre enn $MAX_AGE_DAYS dager, setter til SKIPPED")
            varselOutboxDao.updateStatus(entry.uuid, VarselOutboxStatus.SKIPPED)
            return
        }

        val endring = objectMapper.readValue(entry.payload, KafkaDialogmotekandidatEndring::class.java)
        val ansattFnr = endring.personIdentNumber

        val staleReason = staleReason(endring)
        if (staleReason != null) {
            log.info("Outbox-entry ${entry.uuid} er utdatert — $staleReason, setter til SKIPPED")
            varselOutboxDao.updateStatus(entry.uuid, VarselOutboxStatus.SKIPPED)
            return
        }

        val narmesteLedere = narmesteLederService.getAllNarmesteLederRelations(ansattFnr) ?: emptyList()

        if (endring.kandidat) {
            expandSend(entry, ansattFnr, narmesteLedere)
        } else {
            expandFerdigstill(entry, ansattFnr, narmesteLedere)
        }

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

    private fun expandSend(
        entry: VarselOutboxEntry,
        ansattFnr: String,
        narmesteLedere: List<NarmesteLederRelasjonDTO>,
    ) {
        val oppfolgingstilfelle = oppfolgingstilfelleService.getActiveOppfolgingstilfelleForArbeidstaker(ansattFnr)

        val isDialogmoteAlleredePlanlagt = dialogmoteDAO.getAktiveDialogmoterEtterDato(
            ansattFnr,
            oppfolgingstilfelle?.fom ?: LocalDate.now(),
        ).isNotEmpty()

        if (isDialogmoteAlleredePlanlagt) {
            log.info("Oppretter ingen mottakere for ${entry.uuid} — dialogmøte er allerede planlagt")
            return
        }

        val isSvarBehovAvailableForAT = motebehovStatusHelper.isSvarBehovVarselAvailable(
            motebehovService.hentMotebehovListeForOgOpprettetAvArbeidstaker(ansattFnr),
            oppfolgingstilfelle,
        )
        if (isSvarBehovAvailableForAT) {
            varselOutboxRecipientDao.createRecipient(
                outboxUuid = entry.uuid,
                mottakerFnr = ansattFnr,
                hendelse = ArbeidstakerHendelse(
                    type = HendelseType.SM_DIALOGMOTE_SVAR_MOTEBEHOV,
                    ferdigstill = false,
                    data = null,
                    arbeidstakerFnr = ansattFnr,
                    orgnummer = null,
                ),
            )
        }

        narmesteLedere.forEach { nl ->
            val oppfolgingstilfelleForLeder = oppfolgingstilfelleService.getActiveOppfolgingstilfelleForArbeidsgiver(
                ansattFnr,
                nl.virksomhetsnummer,
            )
            val isSvarBehovAvailableForNL = motebehovStatusHelper.isSvarBehovVarselAvailable(
                motebehovService.hentMotebehovListeForArbeidstakerOpprettetAvLeder(ansattFnr, false, nl.virksomhetsnummer),
                oppfolgingstilfelleForLeder,
            )
            if (isSvarBehovAvailableForNL) {
                varselOutboxRecipientDao.createRecipient(
                    outboxUuid = entry.uuid,
                    mottakerFnr = nl.narmesteLederPersonIdentNumber,
                    hendelse = NarmesteLederHendelse(
                        type = HendelseType.NL_DIALOGMOTE_SVAR_MOTEBEHOV,
                        ferdigstill = false,
                        data = null,
                        narmesteLederFnr = nl.narmesteLederPersonIdentNumber,
                        arbeidstakerFnr = ansattFnr,
                        orgnummer = nl.virksomhetsnummer,
                    ),
                )
            }
        }
    }

    private fun expandFerdigstill(
        entry: VarselOutboxEntry,
        ansattFnr: String,
        narmesteLedere: List<NarmesteLederRelasjonDTO>,
    ) {
        varselOutboxRecipientDao.createRecipient(
            outboxUuid = entry.uuid,
            mottakerFnr = ansattFnr,
            hendelse = ArbeidstakerHendelse(
                type = HendelseType.SM_DIALOGMOTE_SVAR_MOTEBEHOV,
                ferdigstill = true,
                data = null,
                arbeidstakerFnr = ansattFnr,
                orgnummer = null,
            ),
        )

        narmesteLedere.forEach { nl ->
            varselOutboxRecipientDao.createRecipient(
                outboxUuid = entry.uuid,
                mottakerFnr = nl.narmesteLederPersonIdentNumber,
                hendelse = NarmesteLederHendelse(
                    type = HendelseType.NL_DIALOGMOTE_SVAR_MOTEBEHOV,
                    ferdigstill = true,
                    data = null,
                    narmesteLederFnr = nl.narmesteLederPersonIdentNumber,
                    arbeidstakerFnr = ansattFnr,
                    orgnummer = nl.virksomhetsnummer,
                ),
            )
        }
    }

    private fun sendPendingRecipients() {
        varselOutboxRecipientDao.getPending().forEach { recipient ->
            try {
                esyfovarselProducer.sendVarselTilEsyfovarsel(recipient.hendelse)
                varselOutboxRecipientDao.updateStatus(recipient.uuid, VarselOutboxRecipientStatus.SENT)
                log.info("Varsel sendt for mottaker ${recipient.uuid}")
            } catch (e: Exception) {
                log.error("Feil ved sending av varsel for mottaker ${recipient.uuid}, prøver igjen neste kjøring", e)
            }
        }
    }

    companion object {
        private const val MAX_AGE_DAYS = 7L
        private const val STUCK_ALERT_HOURS = 24L
        private val log = LoggerFactory.getLogger(VarselOutboxScheduler::class.java)
        private val objectMapper = configuredJacksonMapper()
    }
}
