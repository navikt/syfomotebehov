package no.nav.syfo.varsel

import no.nav.syfo.consumer.narmesteleder.NarmesteLederService
import no.nav.syfo.dialogmote.DialogmoteStatusService
import no.nav.syfo.metric.Metric
import no.nav.syfo.motebehov.MotebehovService
import no.nav.syfo.motebehov.motebehovstatus.MotebehovStatusHelper
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
    private val motebehovService: MotebehovService,
    private val motebehovStatusHelper: MotebehovStatusHelper,
    private val oppfolgingstilfelleService: OppfolgingstilfelleService,
    private val esyfovarselService: EsyfovarselService,
    private val dialogmoteStatusService: DialogmoteStatusService,
    private val narmesteLederService: NarmesteLederService
) {
    fun sendSvarBehovVarsel(ansattFnr: String, kandidatUuid: String) {
        val ansattesOppfolgingstilfelle =
            oppfolgingstilfelleService.getActiveOppfolgingstilfelleForArbeidstaker(ansattFnr)

        val isDialogmoteAlleredePlanlagt = dialogmoteStatusService.isDialogmotePlanlagtEtterDato(
            ansattFnr,
            null,
            ansattesOppfolgingstilfelle?.fom ?: LocalDate.now()
        )

        if (isDialogmoteAlleredePlanlagt) {
            logDialogmoteAlleredePlanlagt()
            log.info("[$ansattFnr] Dialogmote allerde planlagt")
            return
        }

        log.info("[$ansattFnr] Henter nærmeste ledere..")
        val narmesteLederRelations = narmesteLederService.getAllNarmesteLederRelations(ansattFnr)

        val amountOfVirksomheter = narmesteLederRelations?.distinctBy { it.virksomhetsnummer }?.size ?: 0

        log.info("[$ansattFnr] Antall unike nærmeste ledere for kandidatUuid $kandidatUuid: ${narmesteLederRelations?.size ?: 0}, antall virksomheter: $amountOfVirksomheter")

        log.info("[$ansattFnr] Sender varsel til arbeidstaker")
        sendVarselTilArbeidstaker(ansattFnr, ansattesOppfolgingstilfelle)

        narmesteLederRelations?.forEach {
            log.info("[$ansattFnr] Sender varsel til virksomhet ${it.virksomhetsnummer}")
            sendVarselTilNaermesteLeder(
                ansattFnr,
                it.narmesteLederPersonIdentNumber,
                it.virksomhetsnummer
            )
        }
    }

    private fun sendVarselTilNaermesteLeder(
        ansattFnr: String,
        naermesteLederFnr: String,
        virksomhetsnummer: String
    ) {
        val aktivtOppfolgingstilfelle =
            oppfolgingstilfelleService.getActiveOppfolgingstilfelleForArbeidsgiver(
                ansattFnr,
                virksomhetsnummer
            )

        val isSvarBehovVarselAvailableForLeder = motebehovStatusHelper.isSvarBehovVarselAvailable(
            motebehovService.hentMotebehovListeForArbeidstakerOpprettetAvLeder(
                ansattFnr,
                false,
                virksomhetsnummer
            ),
            aktivtOppfolgingstilfelle
        )
        log.info("[$ansattFnr] AVAILABLE: $isSvarBehovVarselAvailableForLeder")
        if (isSvarBehovVarselAvailableForLeder) {
            metric.tellHendelse("varsel_leder_sent")
            log.info("[$ansattFnr] varsel sent to $naermesteLederFnr @ $virksomhetsnummer")
            esyfovarselService.sendSvarMotebehovVarselTilNarmesteLeder(
                naermesteLederFnr,
                ansattFnr,
                virksomhetsnummer
            )
        } else {
            metric.tellHendelse("varsel_leder_not_sent_motebehov_not_available")
            log.info("[$ansattFnr] Not sending Varsel to $naermesteLederFnr @ $virksomhetsnummer because Møtebehov is not available for the combination of Arbeidstaker and Virksomhet")
        }
    }

    private fun sendVarselTilArbeidstaker(ansattFnr: String, oppfolgingstilfelle: PersonOppfolgingstilfelle?) {
        val isSvarBehovVarselAvailableForArbeidstaker = motebehovStatusHelper.isSvarBehovVarselAvailable(
            motebehovService.hentMotebehovListeForOgOpprettetAvArbeidstaker(ansattFnr),
            oppfolgingstilfelle
        )
        if (isSvarBehovVarselAvailableForArbeidstaker) {
            metric.tellHendelse("varsel_arbeidstaker_sent")
            esyfovarselService.sendSvarMotebehovVarselTilArbeidstaker(ansattFnr)
        } else {
            metric.tellHendelse("varsel_arbeidstaker_not_sent_motebehov_not_available")
            log.info("Not sending Varsel to Arbeidstaker because Møtebehov is not available for the combination of Arbeidstaker and Virksomhet")
        }
    }

    private fun logDialogmoteAlleredePlanlagt() {
        metric.tellHendelse("varsel_arbeidstaker_not_sent_mote_allerede_planlagt")
        log.info("Not sending Varsel to Arbeidstaker because dialogmote er planlagt")

        metric.tellHendelse("varsel_leder_not_sent_mote_allerede_planlagt")
        log.info("Not sending Varsel to Narmeste Leder because dialogmote er planlagt")
    }

    companion object {
        private val log = LoggerFactory.getLogger(VarselServiceV2::class.java)
    }
}
