package no.nav.syfo.dialogmotekandidat

import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.syfo.dialogmotekandidat.database.DialogmoteKandidatEndring
import no.nav.syfo.dialogmotekandidat.database.DialogmotekandidatDAO
import no.nav.syfo.dialogmotekandidat.database.DialogmotekandidatVarselStatusDao
import no.nav.syfo.dialogmotekandidat.database.DialogmotekandidatVarselType
import no.nav.syfo.dialogmotekandidat.kafka.KafkaDialogmotekandidatEndring
import no.nav.syfo.util.isEqualOrAfter
import no.nav.syfo.util.toNorwegianLocalDateTime
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import javax.inject.Inject

@Service
class DialogmotekandidatService @Inject constructor(
    private val dialogmotekandidatDAO: DialogmotekandidatDAO,
    private val dialogmotekandidatVarselStatusDao: DialogmotekandidatVarselStatusDao,
) {
    @Transactional
    fun receiveDialogmotekandidatEndring(dialogmotekandidatEndring: KafkaDialogmotekandidatEndring) {
        log.info("Mottok kandidatmelding med kandidatstatus ${dialogmotekandidatEndring.kandidat} og arsak ${dialogmotekandidatEndring.arsak}")
        val ansattFnr = dialogmotekandidatEndring.personIdentNumber
        val kafkaCreatedAt = dialogmotekandidatEndring.createdAt.toNorwegianLocalDateTime()

        val existingKandidat = dialogmotekandidatDAO.get(ansattFnr)

        if (existingKandidat?.createdAt?.isEqualOrAfter(kafkaCreatedAt) == true) {
            log.info(
                "Ignoring dialogmotekandidat message",
                kv("event", "dialogmotekandidat.ignored"),
                kv("reason", "newer_change_exists"),
            )
            return
        }

        saveKandidat(existingKandidat, dialogmotekandidatEndring, kafkaCreatedAt, ansattFnr)

        val varselType = resolveVarselType(dialogmotekandidatEndring.kandidat, existingKandidat)
            ?: run {
                log.info(
                    "Ignoring dialogmotekandidat message",
                    kv("event", "dialogmotekandidat.ignored"),
                    kv("reason", "already_kandidat"),
                )
                return
            }

        dialogmotekandidatVarselStatusDao.create(
            kafkaMeldingUuid = dialogmotekandidatEndring.uuid,
            fnr = ansattFnr,
            type = varselType,
        )
    }

    private fun resolveVarselType(
        kandidat: Boolean,
        existingKandidat: DialogmoteKandidatEndring?,
    ): DialogmotekandidatVarselType? = when {
        !kandidat -> DialogmotekandidatVarselType.FERDIGSTILL
        existingKandidat?.kandidat == true -> null
        else -> DialogmotekandidatVarselType.VARSEL
    }

    private fun saveKandidat(
        existingKandidat: DialogmoteKandidatEndring?,
        dialogmotekandidatEndring: KafkaDialogmotekandidatEndring,
        kafkaCreatedAt: LocalDateTime,
        ansattFnr: String
    ) {
        when {
            existingKandidat == null -> {
                log.info("Lagrer ny kandidat i databasen")
                dialogmotekandidatDAO.create(
                    dialogmotekandidatExternalUUID = dialogmotekandidatEndring.uuid,
                    createdAt = kafkaCreatedAt,
                    fnr = ansattFnr,
                    kandidat = dialogmotekandidatEndring.kandidat,
                    arsak = dialogmotekandidatEndring.arsak,
                )
            }

            else -> {
                log.info("Oppdaterer eksisterende kandidat i databasen")
                dialogmotekandidatDAO.update(
                    dialogmotekandidatExternalUUID = dialogmotekandidatEndring.uuid,
                    createdAt = kafkaCreatedAt,
                    fnr = ansattFnr,
                    kandidat = dialogmotekandidatEndring.kandidat,
                    arsak = dialogmotekandidatEndring.arsak,
                )
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
