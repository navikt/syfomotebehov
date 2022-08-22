package no.nav.syfo.motebehov.motebehovstatus

import no.nav.syfo.consumer.aktorregister.domain.Fodselsnummer
import no.nav.syfo.dialogmote.DialogmoteStatusService
import no.nav.syfo.dialogmotekandidat.DialogmotekandidatService
import no.nav.syfo.motebehov.*
import no.nav.syfo.oppfolgingstilfelle.OppfolgingstilfelleService
import no.nav.syfo.oppfolgingstilfelle.database.PersonOppfolgingstilfelle
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate
import javax.inject.Inject

@Service
class MotebehovStatusServiceV2 @Inject constructor(
    private val motebehovService: MotebehovService,
    private val dialogmotekandidatService: DialogmotekandidatService,
    private val dialogmoteStatusService: DialogmoteStatusService,
    private val oppfolgingstilfelleService: OppfolgingstilfelleService
) {

    fun motebehovStatusForArbeidstaker(
        arbeidstakerFnr: Fodselsnummer
    ): MotebehovStatus {
        LOG.info("Henter møtebehovstatus for arbeidstaker")

        val hasUpcomingDialogmote: Boolean =
            dialogmoteStatusService.isDialogmotePlanlagtEtterDato(arbeidstakerFnr, null, LocalDate.now())
        val oppfolgingstilfelle =
            oppfolgingstilfelleService.getActiveOppfolgingstilfelleForArbeidstaker(arbeidstakerFnr)
        val motebehovList: List<Motebehov> =
            motebehovService.hentMotebehovListeForOgOpprettetAvArbeidstaker(arbeidstakerFnr)
        val isDialogmoteKandidat: Boolean =
            dialogmotekandidatService.getDialogmotekandidatStatus(arbeidstakerFnr)?.kandidat == true

        return motebehovStatus(hasUpcomingDialogmote, oppfolgingstilfelle, isDialogmoteKandidat, motebehovList)
    }

    fun motebehovStatusForArbeidsgiver(
        arbeidstakerFnr: Fodselsnummer,
        isOwnLeader: Boolean,
        virksomhetsnummer: String
    ): MotebehovStatus {
        LOG.info("Henter møtebehovstatus for arbeidsgiver")

        val hasUpcomingDialogmote: Boolean =
            dialogmoteStatusService.isDialogmotePlanlagtEtterDato(arbeidstakerFnr, virksomhetsnummer, LocalDate.now())
        val oppfolgingstilfelle =
            oppfolgingstilfelleService.getActiveOppfolgingstilfelleForArbeidsgiver(arbeidstakerFnr, virksomhetsnummer)
        val isDialogmoteKandidat: Boolean =
            dialogmotekandidatService.getDialogmotekandidatStatus(arbeidstakerFnr)?.kandidat == true
        val motebehovList =
            motebehovService.hentMotebehovListeForArbeidstakerOpprettetAvLeder(
                arbeidstakerFnr,
                isOwnLeader,
                virksomhetsnummer
            )

        return motebehovStatus(hasUpcomingDialogmote, oppfolgingstilfelle, isDialogmoteKandidat, motebehovList)
    }

    fun motebehovStatus(
        hasUpcomingDialogmote: Boolean,
        oppfolgingstilfelle: PersonOppfolgingstilfelle?,
        isDialogmoteKandidat: Boolean,
        motebehovList: List<Motebehov>
    ): MotebehovStatus {
        if (hasUpcomingDialogmote || oppfolgingstilfelle == null) {
            return MotebehovStatus(
                false,
                null,
                null,
            )
        } else if (isDialogmoteKandidat) {
            return MotebehovStatus(
                true,
                MotebehovSkjemaType.SVAR_BEHOV,
                getNewestSvarBehovMotebehovInOppfolgingstilfelle(oppfolgingstilfelle, motebehovList)
            )
        } else {
            return MotebehovStatus(
                true,
                MotebehovSkjemaType.MELD_BEHOV,
                getNewestMeldBehovMotebehovInOppfolgingstilfelle(oppfolgingstilfelle, motebehovList)
            )
        }
    }

    private fun getNewestSvarBehovMotebehovInOppfolgingstilfelle(
        oppfolgingstilfelle: PersonOppfolgingstilfelle,
        motebehovList: List<Motebehov>
    ): Motebehov? {
        getNewestMotebehovInOppfolgingstilfelle(
            oppfolgingstilfelle,
            motebehovList
        )?.let {
            if (it.isSvarBehovForOppfolgingstilfelle(oppfolgingstilfelle) || it.isUbehandlet()) {
                return it
            }
        }
        return null
    }

    private fun getNewestMeldBehovMotebehovInOppfolgingstilfelle(
        oppfolgingstilfelle: PersonOppfolgingstilfelle,
        motebehovList: List<Motebehov>
    ): Motebehov? {
        getNewestMotebehovInOppfolgingstilfelle(
            oppfolgingstilfelle,
            motebehovList
        )?.let {
            if (it.isUbehandlet()) {
                return it
            }
        }
        return null
    }

    fun getNewestMotebehovInOppfolgingstilfelle(
        oppfolgingstilfelle: PersonOppfolgingstilfelle,
        motebehovList: List<Motebehov>
    ): Motebehov? {
        val motebehovListCreatedInOppfolgingstilfelle =
            motebehovList.filter { it.isCreatedInOppfolgingstilfelle(oppfolgingstilfelle) }
        return if (motebehovListCreatedInOppfolgingstilfelle.isNotEmpty()) {
            motebehovListCreatedInOppfolgingstilfelle.first()
        } else {
            null
        }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(MotebehovStatusServiceV2::class.java)
    }
}
