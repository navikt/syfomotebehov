package no.nav.syfo.varsel

import no.nav.syfo.consumer.esyfovarsel.EsyfovarselConsumer
import no.nav.syfo.consumer.pdl.PdlConsumer
import no.nav.syfo.dialogmote.DialogmoteStatusService
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
import java.time.LocalDate
import javax.inject.Inject

@Service
class VarselService @Inject constructor(
    private val metric: Metric,
    private val pdlConsumer: PdlConsumer,
    private val esyfovarselConsumer: EsyfovarselConsumer,
    private val motebehovService: MotebehovService,
    private val motebehovStatusService: MotebehovStatusService,
    private val oppfolgingstilfelleService: OppfolgingstilfelleService,
    private val esyfovarselService: EsyfovarselService,
    private val dialogmoteStatusService: DialogmoteStatusService,
) {
    fun sendVarselTilNaermesteLeder(motebehovsvarVarselInfo: MotebehovsvarVarselInfo) {
        val arbeidstakerFnr = pdlConsumer.fnr(motebehovsvarVarselInfo.sykmeldtAktorId)
        val aktivtOppfolgingstilfelleForArbeidsgiver =
            oppfolgingstilfelleService.getActiveOppfolgingstilfelleForArbeidsgiver(
                arbeidstakerFnr,
                motebehovsvarVarselInfo.orgnummer
            )
        val isDialogmoteAlleredePlanlagt = dialogmoteStatusService.isDialogmotePlanlagtEtterDato(
            arbeidstakerFnr,
            motebehovsvarVarselInfo.orgnummer, aktivtOppfolgingstilfelleForArbeidsgiver?.fom ?: LocalDate.now()
        )

        if (!isDialogmoteAlleredePlanlagt) {
            val isSvarBehovVarselAvailableForLeder = isSvarBehovVarselAvailable(
                motebehovService.hentMotebehovListeForArbeidstakerOpprettetAvLeder(
                    arbeidstakerFnr,
                    false,
                    motebehovsvarVarselInfo.orgnummer
                ),
                aktivtOppfolgingstilfelleForArbeidsgiver
            )
            if (!isSvarBehovVarselAvailableForLeder) {
                metric.tellHendelse("varsel_leder_not_sent_motebehov_not_available")
                log.info("Not sending Varsel to Narmeste Leder because Møtebehov is not available for the combination of Arbeidstaker and Virksomhet")
            } else {
                metric.tellHendelse("varsel_leder_sent")
                esyfovarselService.sendSvarMotebehovVarselTilNarmesteLeder(
                    motebehovsvarVarselInfo.naermesteLederFnr,
                    motebehovsvarVarselInfo.arbeidstakerFnr,
                    motebehovsvarVarselInfo.orgnummer
                )
            }
        } else {
            metric.tellHendelse("varsel_leder_not_sent_mote_allerede_planlagt")
            log.info("Not sending Varsel to Narmeste Leder because dialogmote er planlagt")
        }
    }

    fun sendVarselTilArbeidstaker(motebehovsvarVarselInfo: MotebehovsvarSykmeldtVarselInfo) {
        val aktivtOppfolgingstilfelleForArbeidstaker =
            oppfolgingstilfelleService.getActiveOppfolgingstilfelleForArbeidstaker(motebehovsvarVarselInfo.arbeidstakerFnr)

        val isDialogmoteAlleredePlanlagt = dialogmoteStatusService.isDialogmotePlanlagtEtterDato(
            motebehovsvarVarselInfo.arbeidstakerFnr,
            motebehovsvarVarselInfo.orgnummer, aktivtOppfolgingstilfelleForArbeidstaker?.fom ?: LocalDate.now()
        )

        if (!isDialogmoteAlleredePlanlagt) {
            val isSvarBehovVarselAvailableForArbeidstaker = isSvarBehovVarselAvailable(
                motebehovService.hentMotebehovListeForOgOpprettetAvArbeidstaker(motebehovsvarVarselInfo.arbeidstakerFnr),
                aktivtOppfolgingstilfelleForArbeidstaker
            )
            if (!isSvarBehovVarselAvailableForArbeidstaker) {
                metric.tellHendelse("varsel_arbeidstaker_not_sent_motebehov_not_available")
                log.info("Not sending Varsel to Arbeidstaker because Møtebehov is not available for the combination of Arbeidstaker and Virksomhet")
            } else {
                metric.tellHendelse("varsel_arbeidstaker_sent")
                esyfovarselService.sendSvarMotebehovVarselTilArbeidstaker(motebehovsvarVarselInfo.arbeidstakerFnr)
            }
        } else {
            metric.tellHendelse("varsel_arbeidstaker_not_sent_mote_allerede_planlagt")
            log.info("Not sending Varsel to Arbeidstaker because dialogmote er planlagt")
        }
    }

    fun has39UkerVarselBeenSent(
        arbeidtakerFnr: String
    ): Boolean {
        val aktorId = pdlConsumer.aktorid(arbeidtakerFnr)
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
    }
}
