package no.nav.syfo.dialogmotekandidat.scheduler

import no.nav.syfo.consumer.narmesteleder.NarmesteLederRelasjonDTO
import no.nav.syfo.consumer.narmesteleder.NarmesteLederService
import no.nav.syfo.dialogmote.database.DialogmoteDAO
import no.nav.syfo.dialogmotekandidat.database.RecipientSpec
import no.nav.syfo.dialogmotekandidat.database.VarselOutboxEntry
import no.nav.syfo.dialogmotekandidat.database.VarselOutboxRecipientDao
import no.nav.syfo.dialogmotekandidat.database.VarselOutboxRecipientEntry
import no.nav.syfo.dialogmotekandidat.database.VarselOutboxRecipientStatus
import no.nav.syfo.dialogmotekandidat.kafka.KafkaDialogmotekandidatEndring
import no.nav.syfo.motebehov.MotebehovService
import no.nav.syfo.motebehov.motebehovstatus.MotebehovStatusHelper
import no.nav.syfo.oppfolgingstilfelle.OppfolgingstilfelleService
import no.nav.syfo.varsel.esyfovarsel.EsyfovarselProducer
import no.nav.syfo.varsel.esyfovarsel.domain.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate
import javax.inject.Inject

@Service
class VarselOutboxRecipientService @Inject constructor(
    private val varselOutboxRecipientDao: VarselOutboxRecipientDao,
    private val narmesteLederService: NarmesteLederService,
    private val oppfolgingstilfelleService: OppfolgingstilfelleService,
    private val dialogmoteDAO: DialogmoteDAO,
    private val motebehovService: MotebehovService,
    private val motebehovStatusHelper: MotebehovStatusHelper,
    private val esyfovarselProducer: EsyfovarselProducer,
) {
    fun expandAndSaveRecipients(entry: VarselOutboxEntry, endring: KafkaDialogmotekandidatEndring) {
        val ansattFnr = endring.personIdentNumber
        val narmesteLedere = narmesteLederService.getAllNarmesteLederRelations(ansattFnr) ?: emptyList()

        val recipients = if (endring.kandidat) {
            buildRecipientsForSend(entry, ansattFnr, narmesteLedere)
        } else {
            buildRecipientsForFerdigstill(ansattFnr, narmesteLedere)
        }

        varselOutboxRecipientDao.createRecipients(entry.uuid, recipients)
    }

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

    private fun buildRecipientsForSend(
        entry: VarselOutboxEntry,
        ansattFnr: String,
        narmesteLedere: List<NarmesteLederRelasjonDTO>,
    ): List<RecipientSpec> {
        val oppfolgingstilfelle = oppfolgingstilfelleService.getActiveOppfolgingstilfelleForArbeidstaker(ansattFnr)

        val isDialogmoteAlleredePlanlagt = dialogmoteDAO.getAktiveDialogmoterEtterDato(
            ansattFnr,
            oppfolgingstilfelle?.fom ?: LocalDate.now(),
        ).isNotEmpty()

        if (isDialogmoteAlleredePlanlagt) {
            log.info("Oppretter ingen mottakere for ${entry.uuid} — dialogmøte er allerede planlagt")
            return emptyList()
        }

        val recipients = mutableListOf<RecipientSpec>()

        val isSvarBehovAvailableForAT = motebehovStatusHelper.isSvarBehovVarselAvailable(
            motebehovService.hentMotebehovListeForOgOpprettetAvArbeidstaker(ansattFnr),
            oppfolgingstilfelle,
        )
        if (isSvarBehovAvailableForAT) {
            recipients += RecipientSpec(
                mottakerFnr = ansattFnr,
                hendelse = ArbeidstakerHendelse(
                    type = HendelseType.SM_DIALOGMOTE_SVAR_MOTEBEHOV,
                    ferdigstill = false,
                    data = null,
                    arbeidstakerFnr = ansattFnr,
                    orgnummer = null,
                ),
            )
        }

        narmesteLedere.forEach { nl ->
            val oppfolgingstilfelleForLeder = oppfolgingstilfelleService.getActiveOppfolgingstilfelleForArbeidsgiver(
                ansattFnr,
                nl.virksomhetsnummer,
            )
            val isSvarBehovAvailableForNL = motebehovStatusHelper.isSvarBehovVarselAvailable(
                motebehovService.hentMotebehovListeForArbeidstakerOpprettetAvLeder(ansattFnr, false, nl.virksomhetsnummer),
                oppfolgingstilfelleForLeder,
            )
            if (isSvarBehovAvailableForNL) {
                recipients += RecipientSpec(
                    mottakerFnr = nl.narmesteLederPersonIdentNumber,
                    hendelse = NarmesteLederHendelse(
                        type = HendelseType.NL_DIALOGMOTE_SVAR_MOTEBEHOV,
                        ferdigstill = false,
                        data = null,
                        narmesteLederFnr = nl.narmesteLederPersonIdentNumber,
                        arbeidstakerFnr = ansattFnr,
                        orgnummer = nl.virksomhetsnummer,
                    ),
                )
            }
        }

        return recipients
    }

    private fun buildRecipientsForFerdigstill(
        ansattFnr: String,
        narmesteLedere: List<NarmesteLederRelasjonDTO>,
    ): List<RecipientSpec> {
        val recipients = mutableListOf<RecipientSpec>()

        recipients += RecipientSpec(
            mottakerFnr = ansattFnr,
            hendelse = ArbeidstakerHendelse(
                type = HendelseType.SM_DIALOGMOTE_SVAR_MOTEBEHOV,
                ferdigstill = true,
                data = null,
                arbeidstakerFnr = ansattFnr,
                orgnummer = null,
            ),
        )

        narmesteLedere.forEach { nl ->
            recipients += RecipientSpec(
                mottakerFnr = nl.narmesteLederPersonIdentNumber,
                hendelse = NarmesteLederHendelse(
                    type = HendelseType.NL_DIALOGMOTE_SVAR_MOTEBEHOV,
                    ferdigstill = true,
                    data = null,
                    narmesteLederFnr = nl.narmesteLederPersonIdentNumber,
                    arbeidstakerFnr = ansattFnr,
                    orgnummer = nl.virksomhetsnummer,
                ),
            )
        }

        return recipients
    }

    companion object {
        private val log = LoggerFactory.getLogger(VarselOutboxRecipientService::class.java)
    }
}
