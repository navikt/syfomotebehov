package no.nav.syfo.dialogmotekandidat

import no.nav.syfo.dialogmotekandidat.database.DialogmoteKandidatEndring
import no.nav.syfo.dialogmotekandidat.database.DialogmotekandidatDAO
import no.nav.syfo.dialogmotekandidat.kafka.KafkaDialogmotekandidatEndring
import no.nav.syfo.dialogmotekandidat.kafka.localCreatedAt
import no.nav.syfo.util.isEqualOrAfter
import no.nav.syfo.varsel.VarselServiceV2
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import javax.inject.Inject

@Service
class DialogmotekandidatService @Inject constructor(
    private val dialogmotekandidatDAO: DialogmotekandidatDAO,
    private val varselServiceV2: VarselServiceV2,
) {
    fun receiveDialogmotekandidatEndring(dialogmotekandidatEndring: KafkaDialogmotekandidatEndring) {
        log.info("Mottok kandidatmelding med kandidatstatus ${dialogmotekandidatEndring.kandidat} og arsak ${dialogmotekandidatEndring.arsak}")
        val existingKandidat = dialogmotekandidatDAO.get(dialogmotekandidatEndring.personIdentNumber)
        val sendVarselOrFerdigstill =
            sendVarselOrFerdigstill(existingKandidat, dialogmotekandidatEndring)

        if (sendVarselOrFerdigstill) {
            if (dialogmotekandidatEndring.kandidat) {
                varselServiceV2.sendSvarBehovVarsel(dialogmotekandidatEndring.personIdentNumber, dialogmotekandidatEndring.uuid)
            } else {
                log.info("Ferdigstill varsel because message has kandidat=false")
                varselServiceV2.ferdigstillSvarMotebehovVarselForArbeidstaker(dialogmotekandidatEndring.personIdentNumber)
                varselServiceV2.ferdigstillSvarMotebehovVarselForNarmesteLedere(dialogmotekandidatEndring.personIdentNumber)
            }
        }

        upsertWhenNeverKandidate(existingKandidat, dialogmotekandidatEndring)
    }

    private fun upsertWhenNeverKandidate(
        existingKandidat: DialogmoteKandidatEndring?,
        dialogmotekandidatEndring: KafkaDialogmotekandidatEndring
    ) {
        when {
            existingKandidat == null -> {
                log.info("Lagrer ny kandidat i databasen")
                dialogmotekandidatDAO.create(
                    dialogmotekandidatExternalUUID = dialogmotekandidatEndring.uuid,
                    createdAt = dialogmotekandidatEndring.localCreatedAt(),
                    fnr = dialogmotekandidatEndring.personIdentNumber,
                    kandidat = dialogmotekandidatEndring.kandidat,
                    arsak = dialogmotekandidatEndring.arsak
                )
            }

            newerKandidateExists(existingKandidat, dialogmotekandidatEndring) -> {
                return
            }

            else -> {
                log.info("Oppdaterer eksisterende kandidat i databasen")
                dialogmotekandidatDAO.update(
                    dialogmotekandidatExternalUUID = dialogmotekandidatEndring.uuid,
                    createdAt = dialogmotekandidatEndring.localCreatedAt(),
                    fnr = dialogmotekandidatEndring.personIdentNumber,
                    kandidat = dialogmotekandidatEndring.kandidat,
                    arsak = dialogmotekandidatEndring.arsak
                )
            }
        }
    }

    fun getDialogmotekandidatStatus(arbeidstakerFnr: String): DialogmoteKandidatEndring? {
        return dialogmotekandidatDAO.get(arbeidstakerFnr)
    }

    private fun sendVarselOrFerdigstill(
        existingKandidat: DialogmoteKandidatEndring?,
        dialogmotekandidatEndring: KafkaDialogmotekandidatEndring,
    ): Boolean {
        val newerChangeExists = newerKandidateExists(existingKandidat, dialogmotekandidatEndring)
        val wasKandidateBefore = wasKandidateBefore(existingKandidat, dialogmotekandidatEndring)

        return when {
            newerChangeExists -> false.also { log.info("Skip KafkaDialogmotekandidatEndring message because newer change exists") }
            wasKandidateBefore -> false.also { log.info("Not sending varsel because person is kandidat from before") }
            else -> true
        }
    }

    private fun wasKandidateBefore(
        existingKandidat: DialogmoteKandidatEndring?,
        dialogmotekandidatEndring: KafkaDialogmotekandidatEndring
    ): Boolean = existingKandidat?.kandidat == true && dialogmotekandidatEndring.kandidat

    private fun newerKandidateExists(
        existingKandidat: DialogmoteKandidatEndring?,
        dialogmotekandidatEndring: KafkaDialogmotekandidatEndring
    ): Boolean = existingKandidat?.createdAt?.isEqualOrAfter(dialogmotekandidatEndring.localCreatedAt()) == true

    companion object {
        private val log = LoggerFactory.getLogger(DialogmotekandidatService::class.java)
    }
}
