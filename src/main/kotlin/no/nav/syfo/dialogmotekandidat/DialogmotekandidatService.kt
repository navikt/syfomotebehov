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
    fun receiveDialogmotekandidatEndring(dialogmotekandidatEndring: KafkaDialogmotekandidatEndring) {
        log.info("Mottok kandidatmelding med kandidatstatus ${dialogmotekandidatEndring.kandidat} og arsak ${dialogmotekandidatEndring.arsak}")
        val ansattFnr = dialogmotekandidatEndring.personIdentNumber

        val existingKandidat = dialogmotekandidatDAO.get(ansattFnr)

        when {
            existingKandidat == null -> {
                log.info("Lagrer ny kandidat i databasen")
                dialogmotekandidatDAO.create(
                    dialogmotekandidatExternalUUID = dialogmotekandidatEndring.uuid,
                    createdAt = dialogmotekandidatEndring.createdAt.toNorwegianLocalDateTime(),
                    fnr = ansattFnr,
                    kandidat = dialogmotekandidatEndring.kandidat,
                    arsak = dialogmotekandidatEndring.arsak
                )
            }

            existingKandidat.createdAt.isEqualOrAfter(dialogmotekandidatEndring.createdAt.toNorwegianLocalDateTime()) -> {
                log.info("Skip KafkaDialogmotekandidatEndring message because newer change exists")
                varselOutboxDao.createSkipped(dialogmotekandidatEndring)
                return
            }

            else -> {
                log.info("Oppdaterer eksisterende kandidat i databasen")
                dialogmotekandidatDAO.update(
                    dialogmotekandidatExternalUUID = dialogmotekandidatEndring.uuid,
                    createdAt = dialogmotekandidatEndring.createdAt.toNorwegianLocalDateTime(),
                    fnr = ansattFnr,
                    kandidat = dialogmotekandidatEndring.kandidat,
                    arsak = dialogmotekandidatEndring.arsak
                )
            }
        }

        // isKandidatFromBefore er lest FØR DB-oppdateringen ovenfor
        val isKandidatFromBefore = existingKandidat != null && existingKandidat.kandidat

        when {
            !dialogmotekandidatEndring.kandidat -> {
                log.info("Oppretter outbox-entry for ferdigstilling av varsel (kandidat=false)")
                varselOutboxDao.createPending(dialogmotekandidatEndring)
            }
            isKandidatFromBefore -> {
                log.info("Oppretter skipped outbox-entry: person var allerede kandidat")
                varselOutboxDao.createSkipped(dialogmotekandidatEndring)
            }
            else -> {
                log.info("Oppretter outbox-entry for utsending av svar behov varsel")
                varselOutboxDao.createPending(dialogmotekandidatEndring)
            }
        }
    }

    fun getDialogmotekandidatStatus(arbeidstakerFnr: String): DialogmoteKandidatEndring? {
        return dialogmotekandidatDAO.get(arbeidstakerFnr)
    }

    companion object {
        private val log = LoggerFactory.getLogger(DialogmotekandidatService::class.java)
    }
}
