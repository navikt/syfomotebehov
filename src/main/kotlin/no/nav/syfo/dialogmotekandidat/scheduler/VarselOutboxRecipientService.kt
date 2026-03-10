package no.nav.syfo.dialogmotekandidat.scheduler

import no.nav.syfo.dialogmotekandidat.database.VarselOutboxRecipientDao
import no.nav.syfo.dialogmotekandidat.database.VarselOutboxRecipientEntry
import no.nav.syfo.dialogmotekandidat.database.VarselOutboxRecipientStatus
import no.nav.syfo.varsel.esyfovarsel.EsyfovarselProducer
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import javax.inject.Inject

@Service
class VarselOutboxRecipientService @Inject constructor(
    private val varselOutboxRecipientDao: VarselOutboxRecipientDao,
    private val esyfovarselProducer: EsyfovarselProducer,
) {
    fun sendPendingRecipients() {
        varselOutboxRecipientDao.getPending().forEach { recipient: VarselOutboxRecipientEntry ->
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
        private val log = LoggerFactory.getLogger(VarselOutboxRecipientService::class.java)
    }
}
