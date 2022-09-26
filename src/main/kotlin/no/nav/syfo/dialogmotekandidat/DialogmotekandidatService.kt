package no.nav.syfo.dialogmotekandidat

import no.nav.syfo.consumer.aktorregister.domain.Fodselsnummer
import no.nav.syfo.dialogmotekandidat.database.DialogmoteKandidatEndring
import no.nav.syfo.dialogmotekandidat.database.DialogmotekandidatDAO
import no.nav.syfo.dialogmotekandidat.kafka.KafkaDialogmotekandidatEndring
import no.nav.syfo.util.isEqualOrAfter
import no.nav.syfo.util.toNorwegianLocalDateTime
import no.nav.syfo.varsel.VarselServiceV2
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import javax.inject.Inject

@Service
class DialogmotekandidatService @Inject constructor(
    private val dialogmotekandidatDAO: DialogmotekandidatDAO,
    private val varselServiceV2: VarselServiceV2,
    @Value("\${toggle.kandidatlista}")
    private val useKandidatlista: Boolean,
) {
    fun receiveDialogmotekandidatEndring(dialogmotekandidatEndring: KafkaDialogmotekandidatEndring) {
        val ansattFnr = Fodselsnummer(dialogmotekandidatEndring.personIdentNumber)

        val existingKandidat = dialogmotekandidatDAO.get(ansattFnr)

        // Store latest kandidat-info
        when {
            existingKandidat == null -> {
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
        val isNotKandidatFromBefore = existingKandidat == null || !existingKandidat.kandidat
        if (useKandidatlista && dialogmotekandidatEndring.kandidat && isNotKandidatFromBefore) {
            varselServiceV2.sendSvarBehovVarsel(ansattFnr)
        }
    }

    fun getDialogmotekandidatStatus(arbeidstakerFnr: Fodselsnummer): DialogmoteKandidatEndring? {
        return dialogmotekandidatDAO.get(arbeidstakerFnr)
    }

    companion object {
        private val log = LoggerFactory.getLogger(DialogmotekandidatService::class.java)
    }
}
