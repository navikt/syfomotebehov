package no.nav.syfo.varsel

import no.nav.syfo.consumer.aktorregister.AktorregisterConsumer
import no.nav.syfo.consumer.aktorregister.domain.AktorId
import no.nav.syfo.consumer.aktorregister.domain.Fodselsnummer
import no.nav.syfo.consumer.esyfovarsel.EsyfovarselConsumer
import no.nav.syfo.dialogmote.DialogmoteStatusService
import no.nav.syfo.dialogmotekandidat.DialogmotekandidatService
import no.nav.syfo.metric.Metric
import no.nav.syfo.motebehov.Motebehov
import no.nav.syfo.motebehov.MotebehovService
import no.nav.syfo.motebehov.motebehovstatus.MotebehovStatusServiceV2
import no.nav.syfo.motebehov.motebehovstatus.isSvarBehovVarselAvailable
import no.nav.syfo.oppfolgingstilfelle.OppfolgingstilfelleService
import no.nav.syfo.oppfolgingstilfelle.database.PersonOppfolgingstilfelle
import no.nav.syfo.varsel.esyfovarsel.EsyfovarselService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate
import javax.inject.Inject

@Service
class VarselServiceV2 @Inject constructor(
    private val metric: Metric,
    private val aktorregisterConsumer: AktorregisterConsumer,
    private val esyfovarselConsumer: EsyfovarselConsumer,
    private val motebehovService: MotebehovService,
    private val motebehovStatusServiceV2: MotebehovStatusServiceV2,
    private val oppfolgingstilfelleService: OppfolgingstilfelleService,
    private val esyfovarselService: EsyfovarselService,
    private val dialogmoteStatusService: DialogmoteStatusService,
    private val dialogmotekandidatService: DialogmotekandidatService
) {
    fun sendVarselTilNaermesteLeder(motebehovsvarVarselInfo: MotebehovsvarVarselInfo) {
        val arbeidstakerFnr = aktorregisterConsumer.getFnrForAktorId(AktorId(motebehovsvarVarselInfo.sykmeldtAktorId))
        val isDialogmoteAlleredePlanlagt = dialogmoteStatusService.isDialogmotePlanlagtEtterDato(
            Fodselsnummer(arbeidstakerFnr),
            motebehovsvarVarselInfo.orgnummer, LocalDate.now()
        )

        if (!isDialogmoteAlleredePlanlagt) {
            val isSvarBehovVarselAvailableForLeder = isSvarBehovVarselAvailableArbeidsgiver(
                Fodselsnummer(arbeidstakerFnr),
                motebehovsvarVarselInfo.orgnummer
            )
            if (isSvarBehovVarselAvailableForLeder) {
                metric.tellHendelse("varsel_leder_sent")
                esyfovarselService.sendSvarMotebehovVarselTilNarmesteLeder(
                    motebehovsvarVarselInfo.naermesteLederFnr,
                    motebehovsvarVarselInfo.arbeidstakerFnr,
                    motebehovsvarVarselInfo.orgnummer
                )
            } else {
                metric.tellHendelse("varsel_leder_not_sent_motebehov_not_available")
                log.info("Not sending Varsel to Narmeste Leder because Møtebehov is not available for the combination of Arbeidstaker and Virksomhet")
            }
        } else {
            metric.tellHendelse("varsel_leder_not_sent_mote_allerede_planlagt")
            log.info("Not sending Varsel to Narmeste Leder because dialogmote er planlagt")
        }
    }

    fun sendVarselTilArbeidstaker(motebehovsvarVarselInfo: MotebehovsvarSykmeldtVarselInfo) {
        val isDialogmoteAlleredePlanlagt = dialogmoteStatusService.isDialogmotePlanlagtEtterDato(
            Fodselsnummer(motebehovsvarVarselInfo.arbeidstakerFnr),
            motebehovsvarVarselInfo.orgnummer, LocalDate.now()
        )

        if (!isDialogmoteAlleredePlanlagt) {
            val isSvarBehovVarselAvailableForArbeidstaker = isSvarBehovVarselAvailableArbeidstaker(
                Fodselsnummer(motebehovsvarVarselInfo.arbeidstakerFnr),
            )
            if (isSvarBehovVarselAvailableForArbeidstaker) {
                metric.tellHendelse("varsel_arbeidstaker_sent")
                esyfovarselService.sendSvarMotebehovVarselTilArbeidstaker(motebehovsvarVarselInfo.arbeidstakerFnr)
            } else {
                metric.tellHendelse("varsel_arbeidstaker_not_sent_motebehov_not_available")
                log.info("Not sending Varsel to Arbeidstaker because Møtebehov is not available for the combination of Arbeidstaker and Virksomhet")
            }
        } else {
            metric.tellHendelse("varsel_arbeidstaker_not_sent_mote_allerede_planlagt")
            log.info("Not sending Varsel to Arbeidstaker because dialogmote er planlagt")
        }
    }

    fun isSvarBehovVarselAvailableArbeidstaker(arbeidstakerFnr: Fodselsnummer): Boolean {
        return isSvarBehovVarselAvailable(
            motebehovService.hentMotebehovListeForOgOpprettetAvArbeidstaker(arbeidstakerFnr),
            oppfolgingstilfelleService.getActiveOppfolgingstilfelleForArbeidstaker(arbeidstakerFnr),
            dialogmotekandidatService.getDialogmotekandidatStatus(arbeidstakerFnr)?.kandidat == true
        )
    }

    fun isSvarBehovVarselAvailableArbeidsgiver(
        arbeidstakerFnr: Fodselsnummer,
        virksomhetsnummer: String
    ): Boolean {
        return isSvarBehovVarselAvailable(
            motebehovService.hentMotebehovListeForArbeidstakerOpprettetAvLeder(
                arbeidstakerFnr,
                false,
                virksomhetsnummer
            ),
            oppfolgingstilfelleService.getActiveOppfolgingstilfelleForArbeidsgiver(arbeidstakerFnr, virksomhetsnummer),
            dialogmotekandidatService.getDialogmotekandidatStatus(arbeidstakerFnr)?.kandidat == true
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
        oppfolgingstilfelle: PersonOppfolgingstilfelle?,
        isDialogmoteKandidat: Boolean,
    ): Boolean {
        oppfolgingstilfelle?.let {
            val motebehovStatus = motebehovStatusServiceV2.motebehovStatus(
                false,
                oppfolgingstilfelle,
                isDialogmoteKandidat,
                motebehovList
            )

            return motebehovStatusServiceV2.getNewestMotebehovInOppfolgingstilfelle(oppfolgingstilfelle, motebehovList)
                ?.let { newestMotebehov ->
                    return motebehovStatus.isSvarBehovVarselAvailable(newestMotebehov)
                } ?: motebehovStatus.isSvarBehovVarselAvailable()
        }
        return false
    }

    companion object {
        private val log = LoggerFactory.getLogger(VarselServiceV2::class.java)
    }
}
