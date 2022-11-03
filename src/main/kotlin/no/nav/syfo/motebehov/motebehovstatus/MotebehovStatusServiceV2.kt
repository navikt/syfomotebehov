package no.nav.syfo.motebehov.motebehovstatus

import no.nav.syfo.dialogmote.DialogmoteStatusService
import no.nav.syfo.dialogmotekandidat.DialogmotekandidatService
import no.nav.syfo.motebehov.*
import no.nav.syfo.oppfolgingstilfelle.OppfolgingstilfelleService
import no.nav.syfo.oppfolgingstilfelle.database.PersonOppfolgingstilfelle
import org.springframework.stereotype.Service
import javax.inject.Inject

@Service
class MotebehovStatusServiceV2 @Inject constructor(
    private val motebehovService: MotebehovService,
    private val dialogmotekandidatService: DialogmotekandidatService,
    private val dialogmoteStatusService: DialogmoteStatusService,
    private val oppfolgingstilfelleService: OppfolgingstilfelleService,
    private val motebehovStatusHelper: MotebehovStatusHelper
) {

    fun motebehovStatusForArbeidstaker(
        arbeidstakerFnr: String
    ): MotebehovStatus {
        val oppfolgingstilfelle =
            oppfolgingstilfelleService.getActiveOppfolgingstilfelleForArbeidstaker(arbeidstakerFnr)
        val hasUpcomingDialogmote: Boolean = hasUpcomingDialogmote(arbeidstakerFnr, null, oppfolgingstilfelle)
        val motebehovList: List<Motebehov> =
            motebehovService.hentMotebehovListeForOgOpprettetAvArbeidstaker(arbeidstakerFnr)
        val isDialogmoteKandidat: Boolean =
            dialogmotekandidatService.getDialogmotekandidatStatus(arbeidstakerFnr)?.kandidat == true

        return motebehovStatusHelper.motebehovStatus(hasUpcomingDialogmote, oppfolgingstilfelle, isDialogmoteKandidat, motebehovList)
    }

    fun motebehovStatusForArbeidsgiver(
        arbeidstakerFnr: String,
        isOwnLeader: Boolean,
        virksomhetsnummer: String
    ): MotebehovStatus {
        val oppfolgingstilfelle =
            oppfolgingstilfelleService.getActiveOppfolgingstilfelleForArbeidsgiver(arbeidstakerFnr, virksomhetsnummer)
        val hasUpcomingDialogmote: Boolean =
            hasUpcomingDialogmote(arbeidstakerFnr, virksomhetsnummer, oppfolgingstilfelle)
        val isDialogmoteKandidat: Boolean =
            dialogmotekandidatService.getDialogmotekandidatStatus(arbeidstakerFnr)?.kandidat == true
        val motebehovList =
            motebehovService.hentMotebehovListeForArbeidstakerOpprettetAvLeder(
                arbeidstakerFnr,
                isOwnLeader,
                virksomhetsnummer
            )

        return motebehovStatusHelper.motebehovStatus(hasUpcomingDialogmote, oppfolgingstilfelle, isDialogmoteKandidat, motebehovList)
    }

    fun hasUpcomingDialogmote(
        arbeidstakerFnr: String,
        virksomhetsnummer: String?,
        oppfolgingstilfelle: PersonOppfolgingstilfelle?
    ): Boolean {
        return if (oppfolgingstilfelle != null) {
            dialogmoteStatusService.isDialogmotePlanlagtEtterDato(
                arbeidstakerFnr,
                virksomhetsnummer,
                oppfolgingstilfelle.fom
            )
        } else false
    }
}
