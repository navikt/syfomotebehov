package no.nav.syfo.varsel

import java.time.LocalDateTime
import javax.inject.Inject
import no.nav.syfo.consumer.aktorregister.AktorregisterConsumer
import no.nav.syfo.consumer.aktorregister.domain.AktorId
import no.nav.syfo.consumer.aktorregister.domain.Fodselsnummer
import no.nav.syfo.consumer.esyfovarsel.EsyfovarselConsumer
import no.nav.syfo.consumer.mote.MoteConsumer
import no.nav.syfo.metric.Metric
import no.nav.syfo.motebehov.Motebehov
import no.nav.syfo.motebehov.MotebehovService
import no.nav.syfo.motebehov.motebehovstatus.MotebehovStatusService
import no.nav.syfo.motebehov.motebehovstatus.isSvarBehovVarselAvailable
import no.nav.syfo.oppfolgingstilfelle.OppfolgingstilfelleService
import no.nav.syfo.oppfolgingstilfelle.database.PersonOppfolgingstilfelle
import no.nav.syfo.varsel.esyfovarsel.EsyfovarselService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class VarselService @Inject constructor(
    private val metric: Metric,
    private val aktorregisterConsumer: AktorregisterConsumer,
    private val esyfovarselConsumer: EsyfovarselConsumer,
    private val motebehovService: MotebehovService,
    private val motebehovStatusService: MotebehovStatusService,
    private val moteConsumer: MoteConsumer,
    private val oppfolgingstilfelleService: OppfolgingstilfelleService,
    private val esyfovarselService: EsyfovarselService,
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
            if (!moteConsumer.erMoteOpprettetForArbeidstakerEtterDato(
                    motebehovsvarVarselInfo.sykmeldtAktorId,
                    startDatoINyesteOppfolgingstilfelle
                )
            ) {
                metric.tellHendelse("varsel_leder_sent")
                esyfovarselService.sendSvarMotebehovVarselTilNarmesteLeder(
                    motebehovsvarVarselInfo.naermesteLederFnr,
                    motebehovsvarVarselInfo.arbeidstakerFnr,
                    motebehovsvarVarselInfo.orgnummer
                )
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

    fun has39UkerVarselBeenSent(
        arbeidtakerFnr: Fodselsnummer
    ): Boolean {
        val aktorId = aktorregisterConsumer.getAktorIdForFodselsnummer(arbeidtakerFnr)
        return esyfovarselConsumer.varsel39Sent(aktorId)
    }

    private fun isSvarBehovVarselAvailable(
        motebehovList: List<Motebehov>,
        oppfolgingstilfelle: PersonOppfolgingstilfelle?
    ): Boolean {
        oppfolgingstilfelle?.let {
            val motebehovStatus = motebehovStatusService.motebehovStatus(oppfolgingstilfelle, motebehovList)

            return motebehovStatusService.getNewestMotebehovInOppfolgingstilfelle(oppfolgingstilfelle, motebehovList)
                ?.let { newestMotebehov ->
                    return motebehovStatus.isSvarBehovVarselAvailable(newestMotebehov)
                } ?: motebehovStatus.isSvarBehovVarselAvailable()
        }
        return false
    }

    companion object {
        private val log = LoggerFactory.getLogger(VarselService::class.java)
        private const val MOTEBEHOV_VARSEL_UKER = 16
        private const val MOTEBEHOV_VARSEL_DAGER = MOTEBEHOV_VARSEL_UKER * 7
    }
}
