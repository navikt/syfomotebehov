package no.nav.syfo.varsel

import no.nav.syfo.consumer.aktorregister.domain.Fodselsnummer
import no.nav.syfo.consumer.aktorregister.domain.Virksomhetsnummer
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
    fun sendSvarBehovVarsel(ansattFnr: Fodselsnummer) {
        val ansattesOppfolgingstilfelle =
            oppfolgingstilfelleService.getActiveOppfolgingstilfelleForArbeidstaker(ansattFnr)

        val isDialogmoteAlleredePlanlagt = dialogmoteStatusService.isDialogmotePlanlagtEtterDato(
            ansattFnr,
            null, ansattesOppfolgingstilfelle?.fom ?: LocalDate.now()
        )

        if (isDialogmoteAlleredePlanlagt) {
            logDialogmoteAlleredePlanlagt()
            return
        }

        log.info("Testing: Henter nærmeste ledere..")
        val narmesteLederRelations = narmesteLederService.getAllNarmesteLederRelations(ansattFnr)

        log.info("Testing: Sender varsel til arbeidstaker")
        sendVarselTilArbeidstaker(ansattFnr, ansattesOppfolgingstilfelle)

        narmesteLederRelations?.forEach {
            log.info("Testing: Sender varsel til virksomhet ${it.virksomhetsnummer}")
            sendVarselTilNaermesteLeder(
                ansattFnr,
                Fodselsnummer(it.narmesteLederPersonIdentNumber),
                Virksomhetsnummer(it.virksomhetsnummer)
            )
        }
    }

    private fun sendVarselTilNaermesteLeder(
        ansattFnr: Fodselsnummer,
        naermesteLederFnr: Fodselsnummer,
        virksomhetsnummer: Virksomhetsnummer
    ) {
        val aktivtOppfolgingstilfelle =
            oppfolgingstilfelleService.getActiveOppfolgingstilfelleForArbeidsgiver(
                ansattFnr,
                virksomhetsnummer.value
            )

        val isSvarBehovVarselAvailableForLeder = motebehovStatusHelper.isSvarBehovVarselAvailable(
            motebehovService.hentMotebehovListeForArbeidstakerOpprettetAvLeder(
                ansattFnr,
                false,
                virksomhetsnummer.value
            ),
            aktivtOppfolgingstilfelle,
        )
        if (isSvarBehovVarselAvailableForLeder) {
            metric.tellHendelse("varsel_leder_sent")
            esyfovarselService.sendSvarMotebehovVarselTilNarmesteLeder(
                naermesteLederFnr.value,
                ansattFnr.value,
                virksomhetsnummer.value
            )
        } else {
            metric.tellHendelse("varsel_leder_not_sent_motebehov_not_available")
            log.info("Not sending Varsel to Narmeste Leder because Møtebehov is not available for the combination of Arbeidstaker and Virksomhet")
        }
    }

    private fun sendVarselTilArbeidstaker(ansattFnr: Fodselsnummer, oppfolgingstilfelle: PersonOppfolgingstilfelle?) {
        val isSvarBehovVarselAvailableForArbeidstaker = motebehovStatusHelper.isSvarBehovVarselAvailable(
            motebehovService.hentMotebehovListeForOgOpprettetAvArbeidstaker(ansattFnr),
            oppfolgingstilfelle,
        )
        if (isSvarBehovVarselAvailableForArbeidstaker) {
            metric.tellHendelse("varsel_arbeidstaker_sent")
            esyfovarselService.sendSvarMotebehovVarselTilArbeidstaker(ansattFnr.value)
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
