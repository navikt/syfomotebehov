package no.nav.syfo.dialogmote

import java.time.LocalDate
import javax.inject.Inject
import no.nav.syfo.dialogmote.avro.KDialogmoteStatusEndring
import no.nav.syfo.dialogmote.database.DialogmoteDAO
import no.nav.syfo.dialogmote.database.Dialogmote
import no.nav.syfo.dialogmote.database.DialogmoteStatusEndringType
import no.nav.syfo.util.convertInstantToLocalDateTime
import no.nav.syfo.varsel.VarselServiceV2
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class DialogmoteStatusService @Inject constructor(
    private val dialogmoteDAO: DialogmoteDAO,
    private val varselService: VarselServiceV2
) {
    fun receiveKDialogmoteStatusendring(kDialogmote: KDialogmoteStatusEndring) {
        LOG.info("Received record isdialogmote statusendring")

        val fnr = kDialogmote.getPersonIdent()
        val virksomhetsnummer = kDialogmote.getVirksomhetsnummer()
        val moteExternUUID = kDialogmote.getDialogmoteUuid()
        val statusEndringTidspunkt = convertInstantToLocalDateTime(kDialogmote.getStatusEndringTidspunkt())
        val dialogmoteTidspunkt = convertInstantToLocalDateTime(kDialogmote.getDialogmoteTidspunkt())
        val statusEndringType = kDialogmote.getStatusEndringType()

        val existingDialogmoter = dialogmoteDAO.get(fnr, virksomhetsnummer, moteExternUUID)

        if (existingDialogmoter.isEmpty()) { // No previously saved meetings
            if (isMoteInnkallingEllerEndring(statusEndringType)) {
                dialogmoteDAO.create(
                    moteExternUUID,
                    dialogmoteTidspunkt,
                    statusEndringTidspunkt,
                    statusEndringType,
                    fnr,
                    virksomhetsnummer
                )
            }
        } else {
            val statusEndringTidspunktInLastSavedMeeting =
                getNewestSavedEndring(existingDialogmoter).statusEndringTidspunkt

            if (statusEndringTidspunkt.isAfter(statusEndringTidspunktInLastSavedMeeting)) {
                if (isMoteInnkallingEllerEndring(statusEndringType)) {
                    dialogmoteDAO.update(
                        fnr,
                        virksomhetsnummer,
                        moteExternUUID,
                        statusEndringType,
                        statusEndringTidspunkt
                    )
                } else {
                    dialogmoteDAO.delete(fnr, virksomhetsnummer, moteExternUUID)
                }
            }
        }

        if (DialogmoteStatusEndringType.INNKALT.name == statusEndringType) {
            varselService.ferdigstillSvarMotebehovVarselForArbeidstaker(fnr)
            varselService.ferdigstillSvarMotebehovVarselForNarmesteLeder(fnr, virksomhetsnummer)
        }
    }

    fun isDialogmotePlanlagtEtterDato(
        arbeidstakerFnr: String,
        virksomhetsnummer: String?,
        dato: LocalDate
    ): Boolean {
        return virksomhetsnummer?.let {
            dialogmoteDAO.getAktiveDialogmoterPaVirksomhetEtterDato(
                arbeidstakerFnr,
                it, dato
            ).isNotEmpty()
        } ?: dialogmoteDAO.getAktiveDialogmoterEtterDato(arbeidstakerFnr, dato).isNotEmpty()
    }

    private fun getNewestSavedEndring(existingDialogmoter: List<Dialogmote>): Dialogmote {
        return existingDialogmoter.sortedWith(compareBy { it.statusEndringTidspunkt })[0]
    }

    private fun isMoteInnkallingEllerEndring(statusEndringType: String): Boolean {
        return DialogmoteStatusEndringType.INNKALT.name.equals(statusEndringType) ||
            DialogmoteStatusEndringType.NYTT_TID_STED.name.equals(statusEndringType)
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(DialogmoteStatusService::class.java)
    }
}
