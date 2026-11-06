package no.nav.syfo.varsel.budstikka

import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.budstikka.contract.Budstikka
import no.nav.budstikka.contract.EventId
import no.nav.budstikka.contract.PersonIdentifier
import no.nav.syfo.dialogmote.database.DialogmoteDAO
import no.nav.syfo.metric.Metric
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.TransactionTemplate
import java.time.LocalDateTime

/**
 * Viser og skjuler kortet syfo-dialog («Dialogmøte med Nav») på Min side via Budstikka.
 *
 * Kortet deles med isdialogmote, og i Budstikka vinner siste kommando per person. Kortet viser ingenting
 * når ingenting er aktuelt, så en overflødig enable er ufarlig. Den eneste farlige feilen er å skjule kortet
 * mens et dialogmøte er aktivt. Derfor sendes aldri disable når personen har et aktivt møte.
 */
@Service
class BudstikkaMikrofrontendService(
    private val utsendingDAO: BudstikkaMikrofrontendDAO,
    private val dialogmoteDAO: DialogmoteDAO,
    private val producer: BudstikkaProducer,
    private val metric: Metric,
    transactionManager: PlatformTransactionManager,
    @Value("\${toggle.motebehov.mikrofrontend.via.budstikka:false}") private val viaBudstikka: Boolean,
) {
    private val transactionTemplate =
        TransactionTemplate(transactionManager).apply {
            propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW
        }

    fun vis(fnr: String) {
        if (!viaBudstikka) return
        utsendingDAO.create(fnr, MikrofrontendKommando.ENABLE)
    }

    fun skjulHvisIngenAktivtDialogmote(fnr: String) {
        if (!viaBudstikka) return
        if (dialogmoteDAO.harAktivtDialogmote(fnr)) {
            metric.tellHendelse(METRIC_DISABLE_SKIPPED)
            log.info("Skjuler ikke syfo-dialog fordi personen har et aktivt dialogmøte")
            return
        }
        utsendingDAO.create(fnr, MikrofrontendKommando.DISABLE)
    }

    /**
     * Sender ventende kommandoer, én per transaksjon. En disable sjekkes på nytt rett før sending, fordi en
     * innkalling kan ha kommet etter at kommandoen ble lagt i kø. Stopper ved første feil.
     */
    fun sendVentendeKommandoer() {
        repeat(MAX_ROWS_PER_TICK) {
            val fortsett =
                transactionTemplate.execute {
                    val utsending = utsendingDAO.getNextPending() ?: return@execute false
                    runCatching { send(utsending) }
                        .onFailure { e ->
                            utsendingDAO.incrementRetryCount(utsending.id)
                            log.warn(
                                "Feil ved sending av mikrofrontend-kommando til Budstikka",
                                kv("event", "budstikka.mikrofrontend.retry"),
                                kv("id", utsending.id),
                                kv("eventId", utsending.eventId),
                                kv("retryCount", utsending.retryCount + 1),
                                e,
                            )
                        }.isSuccess
                } == true
            if (!fortsett) return
        }
    }

    fun ryddOpp() {
        val givenUp = utsendingDAO.countGivenUp()
        if (givenUp > 0) {
            log.error(
                "Mikrofrontend-kommandoer til Budstikka har overskredet maks antall retries",
                kv("event", "budstikka.mikrofrontend.given_up"),
                kv("event_type", "budstikka_mikrofrontend_exhausted"),
                kv("error_code", "RETRIES_EXHAUSTED"),
                kv("count", givenUp),
            )
        }
        val deleted = utsendingDAO.deleteOlderThan(LocalDateTime.now().minusMonths(1))
        log.info("Ryddet opp", kv("event", "budstikka.mikrofrontend.cleanup"), kv("deleted", deleted))
    }

    private fun send(utsending: MikrofrontendUtsending) {
        if (utsending.kommando == MikrofrontendKommando.DISABLE && dialogmoteDAO.harAktivtDialogmote(utsending.fnr)) {
            utsendingDAO.updateStatus(utsending.id, MikrofrontendUtsendingStatus.SKIPPED)
            metric.tellHendelse(METRIC_DISABLE_SKIPPED)
            log.info(
                "Skjuler ikke syfo-dialog fordi personen har fått et aktivt dialogmøte",
                kv("event", "budstikka.mikrofrontend.disable_skipped"),
                kv("id", utsending.id),
            )
            return
        }
        val eventId = EventId(utsending.eventId)
        val reference = utsending.id.toString()
        val sykmeldt = PersonIdentifier(utsending.fnr)
        val dispatch =
            when (utsending.kommando) {
                MikrofrontendKommando.ENABLE -> Budstikka.microfrontendEnable(eventId, reference, sykmeldt, SYFO_DIALOG)
                MikrofrontendKommando.DISABLE -> Budstikka.microfrontendDisable(eventId, reference, sykmeldt, SYFO_DIALOG)
            }
        producer.send(dispatch)
        utsendingDAO.updateStatus(utsending.id, MikrofrontendUtsendingStatus.SENT)
        metric.tellHendelse("budstikka_mikrofrontend_${utsending.kommando.name.lowercase()}_sent")
        log.info(
            "Sendte mikrofrontend-kommando til Budstikka",
            kv("event", "budstikka.mikrofrontend.sent"),
            kv("kommando", utsending.kommando.name),
            kv("id", utsending.id),
            kv("eventId", utsending.eventId),
        )
    }

    companion object {
        const val SYFO_DIALOG = "syfo-dialog"
        private const val MAX_ROWS_PER_TICK = 100
        private const val METRIC_DISABLE_SKIPPED = "budstikka_mikrofrontend_disable_skipped_aktivt_mote"
        private val log = LoggerFactory.getLogger(BudstikkaMikrofrontendService::class.java)
    }
}
