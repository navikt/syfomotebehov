package no.nav.syfo.migration

import no.nav.syfo.consumer.pdl.PdlConsumer
import no.nav.syfo.motebehov.database.MotebehovDAO
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class MigrateAktorIdComponent(
    private val motebehovDAO: MotebehovDAO,
    private val pdlConsumer: PdlConsumer
) {
    @Scheduled(cron = "0 */30 * * * *")
    fun run() {
        log.info("Running migration job")
        val motebehovUtenFnr = motebehovDAO.hentMotebehovUtenFnr()

        log.info("Antall rader uten fnr: ${motebehovUtenFnr.size}")

        motebehovUtenFnr.forEach {
            val uuid = it.uuid
            val aktorIdSm = it.aktoerId
            val aktorIdOpprettetAv = it.opprettetAv
            val fnrSm = pdlConsumer.fnr(aktorIdSm)
            val fnrOpprettetAv = pdlConsumer.fnr(aktorIdOpprettetAv)

            if (motebehovDAO.oppdaterMotebehovMedSmFnr(uuid, fnrSm) == 0)
                log.info("Klarte ikke oppdatere sykmeldt fnr for UUID $uuid")
            if (motebehovDAO.oppdaterMotebehovMedOpprettetAvFnr(uuid, fnrOpprettetAv) == 0)
                log.info("Klarte ikke oppdatere opprettet av fnr for UUID $uuid")
        }
        log.info("Finished running migration job")
    }

    companion object {
        private val log = LoggerFactory.getLogger(MigrateAktorIdComponent::class.java)
    }
}
