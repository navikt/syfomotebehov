package no.nav.syfo.batch

import no.nav.syfo.consumer.aktorregister.AktorregisterConsumer
import no.nav.syfo.consumer.aktorregister.domain.AktorId
import no.nav.syfo.consumer.aktorregister.domain.Fodselsnummer
import no.nav.syfo.motebehov.MotebehovService
import no.nav.syfo.motebehov.database.MotebehovDAO
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import javax.inject.Inject

const val ONE_HOUR_MILLISECONDS: Long = 30 * 60 * 1000

@Service
class BehandleMotebehovBatch @Inject constructor(
    private val leaderElectionService: LeaderElectionService,
    private val aktorregisterConsumer: AktorregisterConsumer,
    private val motebehovService: MotebehovService,
    private val motebehovDAO: MotebehovDAO
) {
    @Scheduled(fixedDelay = ONE_HOUR_MILLISECONDS)
    fun processUnprocessMotebehov() {
        if (leaderElectionService.isLeader()) {
            log.info("FIX-BATCH-TRACE: Is leader and starting to process")
            val systemVeilederIdent = "X000000"
            val motebehovListe = motebehovDAO.hentUbehandledeMotebehovSvar2019()
            var counter = 1
            log.info("FIX-BATCH-TRACE: found list of ${motebehovListe.size} motebehov to process")
            motebehovListe.forEach {
                val arbeidstakerFnr = Fodselsnummer(
                    aktorregisterConsumer.getFnrForAktorId(AktorId(it.aktoerId))
                )
                motebehovService.behandleUbehandledeMotebehovBatch(
                    arbeidstakerFnr = arbeidstakerFnr,
                    veilederIdent = systemVeilederIdent
                )
                log.info("FIX-BATCH-TRACE: progress $counter / ${motebehovListe.size} motebehov processed")
                counter++
            }
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(BehandleMotebehovBatch::class.java)
    }
}
