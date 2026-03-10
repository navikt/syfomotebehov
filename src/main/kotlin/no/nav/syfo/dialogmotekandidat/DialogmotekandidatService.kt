package no.nav.syfo.dialogmotekandidat

import no.nav.syfo.dialogmotekandidat.database.DialogmoteKandidatEndring
import no.nav.syfo.dialogmotekandidat.database.DialogmotekandidatDAO
import no.nav.syfo.dialogmotekandidat.database.VarselOutboxDao
import no.nav.syfo.dialogmotekandidat.kafka.KafkaDialogmotekandidatEndring
import no.nav.syfo.util.isEqualOrAfter
import no.nav.syfo.util.toNorwegianLocalDateTime
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import javax.inject.Inject

@Service
class DialogmotekandidatService @Inject constructor(
    private val dialogmotekandidatDAO: DialogmotekandidatDAO,
    private val varselOutboxDao: VarselOutboxDao,
) {
    @Transactional
    fun receiveDialogmotekandidatEndring(endring: KafkaDialogmotekandidatEndring) {
        log.info("Mottok kandidatmelding: kandidat=${endring.kandidat}, arsak=${endring.arsak}")
        val existing = dialogmotekandidatDAO.get(endring.personIdentNumber)

        if (isOutdated(existing, endring)) {
            log.info("Hopper over utdatert melding — nyere endring finnes allerede i databasen")
            varselOutboxDao.createSkipped(endring)
            return
        }

        val wasKandidatBefore = wasKandidatBefore(existing)
        persistKandidatEndring(existing, endring)

        if (shouldSkipOutbox(wasKandidatBefore, endring.kandidat)) {
            log.info("Oppretter skipped outbox-entry: person var allerede kandidat")
            varselOutboxDao.createSkipped(endring)
        } else {
            log.info("Oppretter pending outbox-entry: kandidat=${endring.kandidat}")
            varselOutboxDao.createPending(endring)
        }
    }

    private fun wasKandidatBefore(existing: DialogmoteKandidatEndring?): Boolean =
        existing?.kandidat ?: false

    private fun shouldSkipOutbox(wasKandidatBefore: Boolean, isKandidatNow: Boolean): Boolean =
        isKandidatNow && wasKandidatBefore

    private fun isOutdated(
        existing: DialogmoteKandidatEndring?,
        endring: KafkaDialogmotekandidatEndring,
    ): Boolean = existing != null &&
        existing.createdAt.isEqualOrAfter(endring.createdAt.toNorwegianLocalDateTime())

    private fun persistKandidatEndring(
        existing: DialogmoteKandidatEndring?,
        endring: KafkaDialogmotekandidatEndring,
    ) {
        val createdAt = endring.createdAt.toNorwegianLocalDateTime()
        if (existing == null) {
            dialogmotekandidatDAO.create(
                dialogmotekandidatExternalUUID = endring.uuid,
                createdAt = createdAt,
                fnr = endring.personIdentNumber,
                kandidat = endring.kandidat,
                arsak = endring.arsak,
            )
        } else {
            dialogmotekandidatDAO.update(
                dialogmotekandidatExternalUUID = endring.uuid,
                createdAt = createdAt,
                fnr = endring.personIdentNumber,
                kandidat = endring.kandidat,
                arsak = endring.arsak,
            )
        }
    }

    fun getDialogmotekandidatStatus(arbeidstakerFnr: String): DialogmoteKandidatEndring? =
        dialogmotekandidatDAO.get(arbeidstakerFnr)

    companion object {
        private val log = LoggerFactory.getLogger(DialogmotekandidatService::class.java)
    }
}
