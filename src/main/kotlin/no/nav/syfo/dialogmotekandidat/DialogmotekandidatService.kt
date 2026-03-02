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
        val sendVarselEllerFerdigstill =
            sendVarselEllerFerdigstill(existingKandidat, dialogmotekandidatEndring)

        if (sendVarselEllerFerdigstill)
        {
            if (dialogmotekandidatEndring.kandidat) {
                varselServiceV2.sendSvarBehovVarsel(dialogmotekandidatEndring.personIdentNumber, dialogmotekandidatEndring.uuid)
            } else {
                log.info("Ferdigstill varsel because message has kandidat=false")
                varselServiceV2.ferdigstillSvarMotebehovVarselForArbeidstaker(dialogmotekandidatEndring.personIdentNumber)
                varselServiceV2.ferdigstillSvarMotebehovVarselForNarmesteLedere(dialogmotekandidatEndring.personIdentNumber)
            }
        }

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

    private fun sendVarselEllerFerdigstill(
        existingKandidat: DialogmoteKandidatEndring?,
        dialogmotekandidatEndring: KafkaDialogmotekandidatEndring,
    ): Boolean {
        if (existingKandidat?.createdAt?.isEqualOrAfter(dialogmotekandidatEndring.localCreatedAt()) == true) {
            log.info("Skip KafkaDialogmotekandidatEndring message because newer change exists")
            return false
        }

        // Trigger side effects before persisting newest state so retries can re-run side effects on failure.
        if (existingKandidat?.kandidat == true && dialogmotekandidatEndring.kandidat) {
            log.info("Not sending varsel because person is kandidat from before")
            return false
        }
        return true
    }

    companion object {
        private val log = LoggerFactory.getLogger(DialogmotekandidatService::class.java)
    }
}
