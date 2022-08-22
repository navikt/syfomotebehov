package no.nav.syfo.dialogmotekandidat

import no.nav.syfo.consumer.aktorregister.domain.Fodselsnummer
import no.nav.syfo.dialogmotekandidat.database.DialogmoteKandidatEndring
import no.nav.syfo.dialogmotekandidat.database.DialogmotekandidatDAO
import no.nav.syfo.dialogmotekandidat.kafka.KafkaDialogmotekandidatEndring
import no.nav.syfo.util.toNorwegianLocalDateTime
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import javax.inject.Inject

@Service
class DialogmotekandidatService @Inject constructor(
    private val dialogmotekandidatDAO: DialogmotekandidatDAO
) {
    fun receiveDialogmotekandidatEndring(dialogmotekandidatEndring: KafkaDialogmotekandidatEndring) {
        val fnr = Fodselsnummer(dialogmotekandidatEndring.personIdentNumber)

        val existingKandidat = dialogmotekandidatDAO.get(fnr)

        when {
            existingKandidat == null -> {
                dialogmotekandidatDAO.create(
                    dialogmotekandidatExternalUUID = dialogmotekandidatEndring.uuid,
                    createdAt = dialogmotekandidatEndring.createdAt.toNorwegianLocalDateTime(),
                    fnr = fnr,
                    kandidat = dialogmotekandidatEndring.kandidat,
                    arsak = dialogmotekandidatEndring.arsak
                )
            }
            existingKandidat.createdAt.isAfter(dialogmotekandidatEndring.createdAt.toNorwegianLocalDateTime()) -> {
                LOG.info("Skip KafkaDialogmotekandidatEndring message because newer change exists")
            }
            else -> {
                dialogmotekandidatDAO.update(
                    dialogmotekandidatExternalUUID = dialogmotekandidatEndring.uuid,
                    createdAt = dialogmotekandidatEndring.createdAt.toNorwegianLocalDateTime(),
                    fnr = fnr,
                    kandidat = dialogmotekandidatEndring.kandidat,
                    arsak = dialogmotekandidatEndring.arsak
                )
            }
        }
    }

    fun getDialogmotekandidatStatus(arbeidstakerFnr: Fodselsnummer): DialogmoteKandidatEndring? {
        return dialogmotekandidatDAO.get(arbeidstakerFnr)
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(DialogmotekandidatService::class.java)
    }
}
