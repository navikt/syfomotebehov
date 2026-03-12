package no.nav.syfo.varsel

import no.nav.syfo.consumer.narmesteleder.NarmesteLederRelasjonDTO
import no.nav.syfo.consumer.narmesteleder.NarmesteLederService
import no.nav.syfo.dialogmote.database.DialogmoteDAO
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
class VarselServiceV2
    @Inject
    constructor(
        private val metric: Metric,
        private val motebehovService: MotebehovService,
        private val motebehovStatusHelper: MotebehovStatusHelper,
        private val oppfolgingstilfelleService: OppfolgingstilfelleService,
        private val esyfovarselService: EsyfovarselService,
        private val narmesteLederService: NarmesteLederService,
        private val dialogmoteDAO: DialogmoteDAO,
    ) {
        fun sendSvarBehovVarsel(
            ansattFnr: String,
            kandidatUuid: String,
        ) {
            val ansattesOppfolgingstilfelle =
                oppfolgingstilfelleService.getActiveOppfolgingstilfelleForArbeidstaker(ansattFnr)

            val isDialogmoteAlleredePlanlagt =
                dialogmoteDAO.getAktiveDialogmoterEtterDato(ansattFnr, ansattesOppfolgingstilfelle?.fom ?: LocalDate.now()).isNotEmpty()

            if (isDialogmoteAlleredePlanlagt) {
                logDialogmoteAlleredePlanlagt()
                return
            }

            val narmesteLederRelations = narmesteLedere(ansattFnr)

            val amountOfVirksomheter = narmesteLederRelations.distinctBy { it.virksomhetsnummer }.size

            log.info(
                "Antall unike nærmeste ledere for kandidatUuid $kandidatUuid: ${narmesteLederRelations.size}, antall virksomheter: $amountOfVirksomheter",
            )

            // We do first all API calls to determine if we should send varsel to arbeidstaker and narmesteleder before sending any varsel, to avoid sending varsel to one party and then fail before sending to the other party
            val shouldSendVarselTilArbeidstaker = shouldSendVarselTilArbeidstaker(ansattFnr, ansattesOppfolgingstilfelle)
            val narmesteLederVarsels = getNarmesteLederForVarsel(narmesteLederRelations)
            if (shouldSendVarselTilArbeidstaker) {
                sendVarselTilArbeidstaker(ansattFnr)
            }
            narmesteLederVarsels.forEach {
                sendVarselTilNaermesteLeder(it)
            }
        }

        private fun shouldSendVarselTilNaermesteLeder(
            ansattFnr: String,
            virksomhetsnummer: String,
        ): Boolean {
            val aktivtOppfolgingstilfelle =
                oppfolgingstilfelleService.getActiveOppfolgingstilfelleForArbeidsgiver(
                    ansattFnr,
                    virksomhetsnummer,
                )
            return motebehovStatusHelper.isSvarBehovVarselAvailable(
                motebehovService.hentMotebehovListeForArbeidstakerOpprettetAvLeder(
                    ansattFnr,
                    false,
                    virksomhetsnummer,
                ),
                aktivtOppfolgingstilfelle,
            )
        }

        private fun getNarmesteLederForVarsel(narmesteLederRelations: List<NarmesteLederRelasjonDTO>): List<NarmesteLederVarselInfo> =
            narmesteLederRelations
                .filter { shouldSendVarselTilNaermesteLeder(it.arbeidstakerPersonIdentNumber, it.virksomhetsnummer) }
                .map {
                    NarmesteLederVarselInfo(
                        ansattFnr = it.arbeidstakerPersonIdentNumber,
                        narmesteLederPersonIdentNumber = it.narmesteLederPersonIdentNumber,
                        virksomhetsnummer = it.virksomhetsnummer,
                    )
                }

        fun ferdigstillSvarMotebehovVarselForNarmesteLeder(
            ansattFnr: String,
            virksomhetsnummer: String,
        ) {
            narmesteLeder(ansattFnr, virksomhetsnummer)?.let {
                log.info("Ferdigstiller varsel til virksomhet ${it.virksomhetsnummer}")
                ferdigstillSvarMotebehovVarselForNarmesteLeder(ansattFnr, it.narmesteLederPersonIdentNumber, it.virksomhetsnummer)
            }
        }

        fun ferdigstillSvarMotebehovVarselForNarmesteLeder(
            ansattFnr: String,
            naermesteLederFnr: String,
            virksomhetsnummer: String,
        ) {
            esyfovarselService.ferdigstillSvarMotebehovForArbeidsgiver(naermesteLederFnr, ansattFnr, virksomhetsnummer)
        }

        fun ferdigstillSvarMotebehovVarsel(ansattFnr: String) {
            val narmesteLedere = narmesteLedere(ansattFnr)
            esyfovarselService.ferdigstillSvarMotebehovForArbeidstaker(ansattFnr)
            narmesteLedere.forEach { leder ->
                esyfovarselService.ferdigstillSvarMotebehovForArbeidsgiver(leder.narmesteLederPersonIdentNumber, ansattFnr, leder.virksomhetsnummer).also {
                    log.info("Ferdigstiller varsel til virksomhet ${leder.virksomhetsnummer}")
                }
            }
        }

        fun ferdigstillSvarMotebehovVarselForArbeidstaker(ansattFnr: String) {
            esyfovarselService.ferdigstillSvarMotebehovForArbeidstaker(ansattFnr)
        }

        private fun sendVarselTilNaermesteLeder(narmesteLederInfo: NarmesteLederVarselInfo) =
            esyfovarselService.sendSvarMotebehovVarselTilNarmesteLeder(
                narmesteLederInfo.narmesteLederPersonIdentNumber,
                narmesteLederInfo.ansattFnr,
                narmesteLederInfo.virksomhetsnummer,
            ).also { metric.tellHendelse("varsel_leder_sent") }

        private fun shouldSendVarselTilArbeidstaker(
            ansattFnr: String,
            oppfolgingstilfelle: PersonOppfolgingstilfelle?,
        ): Boolean {
            val isSvarBehovVarselAvailableForArbeidstaker =
                motebehovStatusHelper.isSvarBehovVarselAvailable(
                    motebehovService.hentMotebehovListeForOgOpprettetAvArbeidstaker(ansattFnr),
                    oppfolgingstilfelle,
                )
            if (!isSvarBehovVarselAvailableForArbeidstaker) {
                log.info(
                    "Not sending Varsel to Arbeidstaker because Møtebehov is not available for the combination of Arbeidstaker and Virksomhet",
                )
            }
            return isSvarBehovVarselAvailableForArbeidstaker
        }

        private fun sendVarselTilArbeidstaker(ansattFnr: String) =
            esyfovarselService.sendSvarMotebehovVarselTilArbeidstaker(ansattFnr).also { metric.tellHendelse("varsel_arbeidstaker_sent") }

        private fun narmesteLeder(
            ansattFnr: String,
            virksomhetsnummer: String,
        ): NarmesteLederRelasjonDTO? =
            narmesteLedere(ansattFnr).firstOrNull {
                virksomhetsnummer == it.virksomhetsnummer
            }

        private fun narmesteLedere(ansattFnr: String): List<NarmesteLederRelasjonDTO> =
            narmesteLederService.getAllNarmesteLederRelations(ansattFnr) ?: emptyList()

        private fun logDialogmoteAlleredePlanlagt() {
            metric.tellHendelse("varsel_arbeidstaker_not_sent_mote_allerede_planlagt")
            log.info("Not sending Varsel to Arbeidstaker because dialogmote er planlagt")

            metric.tellHendelse("varsel_leder_not_sent_mote_allerede_planlagt")
            log.info("Not sending Varsel to Narmeste Leder because dialogmote er planlagt")
        }

        data class NarmesteLederVarselInfo(
            val ansattFnr: String,
            val narmesteLederPersonIdentNumber: String,
            val virksomhetsnummer: String,
        )

        companion object {
            private val log = LoggerFactory.getLogger(VarselServiceV2::class.java)
        }
    }
