package no.nav.syfo.varsel

import no.nav.syfo.consumer.aktorregister.AktorregisterConsumer
import no.nav.syfo.consumer.aktorregister.domain.AktorId
import no.nav.syfo.consumer.aktorregister.domain.Fodselsnummer
import no.nav.syfo.consumer.mote.MoteConsumer
import no.nav.syfo.metric.Metric
import no.nav.syfo.motebehov.Motebehov
import no.nav.syfo.motebehov.MotebehovService
import no.nav.syfo.motebehov.motebehovstatus.MotebehovStatusService
import no.nav.syfo.motebehov.motebehovstatus.isSvarBehovVarselAvailable
import no.nav.syfo.oppfolgingstilfelle.OppfolgingstilfelleService
import no.nav.syfo.oppfolgingstilfelle.database.PersonOppfolgingstilfelle
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.*
import javax.inject.Inject

@Service
class VarselService @Inject constructor(
        private val metric: Metric,
        private val aktorregisterConsumer: AktorregisterConsumer,
        private val motebehovService: MotebehovService,
        private val motebehovStatusService: MotebehovStatusService,
        private val moteConsumer: MoteConsumer,
        private val oppfolgingstilfelleService: OppfolgingstilfelleService,
        private val tredjepartsvarselProducer: TredjepartsvarselProducer
) {
    fun sendVarselTilNaermesteLeder(motebehovsvarVarselInfo: MotebehovsvarVarselInfo) {
        val arbeidstakerFnr = aktorregisterConsumer.getFnrForAktorId(AktorId(motebehovsvarVarselInfo.sykmeldtAktorId))
        val isSvarBehovVarselAvailableForLeder = isSvarBehovVarselAvailableArbeidsgiver(
                Fodselsnummer(arbeidstakerFnr),
                motebehovsvarVarselInfo.orgnummer
        )
        if (!isSvarBehovVarselAvailableForLeder) {
            metric.tellHendelse("varsel_leder_not_sent_motebehov_not_available")
            log.info("Not sending Varsel to Narmeste Leder because Møtebehov is not available for the combination of Arbeidstaker and Virksomhet")
        } else {
            val startDatoINyesteOppfolgingstilfelle = LocalDateTime.now().minusDays(MOTEBEHOV_VARSEL_DAGER.toLong())
            if (!moteConsumer.erMoteOpprettetForArbeidstakerEtterDato(motebehovsvarVarselInfo.sykmeldtAktorId, startDatoINyesteOppfolgingstilfelle)) {
                metric.tellHendelse("varsel_leder_sent")
                val kTredjepartsvarsel = mapTilKTredjepartsvarsel(motebehovsvarVarselInfo)
                tredjepartsvarselProducer.sendTredjepartsvarselvarsel(kTredjepartsvarsel)
            } else {
                metric.tellHendelse("varsel_leder_not_sent_moteplanlegger_used_oppfolgingstilfelle")
                log.info("Sender ikke varsel til naermeste leder fordi moteplanleggeren er brukt i oppfolgingstilfellet")
            }
        }
    }

    fun isSvarBehovVarselAvailableArbeidstaker(arbeidstakerFnr: Fodselsnummer): Boolean {
        return isSvarBehovVarselAvailable(
                motebehovService.hentMotebehovListeForOgOpprettetAvArbeidstaker(arbeidstakerFnr),
                oppfolgingstilfelleService.getActiveOppfolgingstilfelleForArbeidstaker(arbeidstakerFnr)
        )
    }

    fun isSvarBehovVarselAvailableArbeidsgiver(
            arbeidstakerFnr: Fodselsnummer,
            virksomhetsnummer: String
    ): Boolean {
        return isSvarBehovVarselAvailable(
                motebehovService.hentMotebehovListeForArbeidstakerOpprettetAvLeder(arbeidstakerFnr, virksomhetsnummer),
                oppfolgingstilfelleService.getActiveOppfolgingstilfelleForArbeidsgiver(arbeidstakerFnr, virksomhetsnummer)
        )
    }

    private fun isSvarBehovVarselAvailable(
            motebehovList: List<Motebehov>,
            oppfolgingstilfelle: PersonOppfolgingstilfelle?
    ): Boolean {
        oppfolgingstilfelle?.let {
            val motebehovStatus = motebehovStatusService.motebehovStatus(oppfolgingstilfelle, motebehovList)

            return motebehovStatusService.getNewestMotebehovInOppfolgingstilfelle(oppfolgingstilfelle, motebehovList)?.let { newestMotebehov ->
                return motebehovStatus.isSvarBehovVarselAvailable(newestMotebehov)
            } ?: true
        }
        return false
    }

    private fun mapTilKTredjepartsvarsel(motebehovsvarVarselInfo: MotebehovsvarVarselInfo): KTredjepartsvarsel {
        return KTredjepartsvarsel(
                type = VarselType.NAERMESTE_LEDER_SVAR_MOTEBEHOV.name,
                ressursId = UUID.randomUUID().toString(),
                aktorId = motebehovsvarVarselInfo.sykmeldtAktorId,
                orgnummer = motebehovsvarVarselInfo.orgnummer,
                utsendelsestidspunkt = LocalDateTime.now()
        )
    }

    companion object {
        private val log = LoggerFactory.getLogger(VarselService::class.java)
        private const val MOTEBEHOV_VARSEL_UKER = 16
        private const val MOTEBEHOV_VARSEL_DAGER = MOTEBEHOV_VARSEL_UKER * 7
    }
}
