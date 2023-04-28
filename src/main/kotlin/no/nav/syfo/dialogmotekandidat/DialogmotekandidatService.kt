package no.nav.syfo.dialogmotekandidat

import no.nav.syfo.dialogmotekandidat.database.DialogmoteKandidatEndring
import no.nav.syfo.dialogmotekandidat.database.DialogmotekandidatDAO
import no.nav.syfo.dialogmotekandidat.kafka.KafkaDialogmotekandidatEndring
import no.nav.syfo.util.isEqualOrAfter
import no.nav.syfo.util.toNorwegianLocalDateTime
import no.nav.syfo.varsel.VarselServiceV2
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import javax.inject.Inject

@Service
class DialogmotekandidatService @Inject constructor(
    private val dialogmotekandidatDAO: DialogmotekandidatDAO,
    private val varselServiceV2: VarselServiceV2
) {
    fun receiveDialogmotekandidatEndring(dialogmotekandidatEndring: KafkaDialogmotekandidatEndring) {
        log.info("Mottok kandidatmelding med kandidatstatus ${dialogmotekandidatEndring.kandidat} og arsak ${dialogmotekandidatEndring.arsak}")
        val ansattFnr = dialogmotekandidatEndring.personIdentNumber

        val existingKandidat = dialogmotekandidatDAO.get(ansattFnr)

        // Store latest kandidat-info
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

        // Send svar behov varsel if no kandidat==true exists from before
        val isKandidatFromBefore = existingKandidat != null && existingKandidat.kandidat

        if (!dialogmotekandidatEndring.kandidat) {
            log.info("Ferdigstill varsel because message has kandidat=false")
            varselServiceV2.ferdigstillSvarMotebehovVarsel(ansattFnr, dialogmotekandidatEndring.uuid)
        } else if (isKandidatFromBefore) {
            log.info("Not sending varsel because person is kandidat from before")
            return
        } else {
            varselServiceV2.sendSvarBehovVarsel(ansattFnr, dialogmotekandidatEndring.uuid)
        }
    }

    fun getDialogmotekandidatStatus(arbeidstakerFnr: String): DialogmoteKandidatEndring? {
        return dialogmotekandidatDAO.get(arbeidstakerFnr)
    }

    companion object {
        private val log = LoggerFactory.getLogger(DialogmotekandidatService::class.java)
    }
}
