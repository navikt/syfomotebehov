package no.nav.syfo.motebehov.motebehovstatus

import no.nav.syfo.motebehov.Motebehov
import no.nav.syfo.motebehov.isCreatedInOppfolgingstilfelle
import no.nav.syfo.motebehov.isSvarBehovForOppfolgingstilfelle
import no.nav.syfo.motebehov.isUbehandlet
import no.nav.syfo.oppfolgingstilfelle.database.PersonOppfolgingstilfelle
import org.springframework.stereotype.Component

@Component
class MotebehovStatusHelper {

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

    fun isSvarBehovVarselAvailable(
        motebehovList: List<Motebehov>,
        oppfolgingstilfelle: PersonOppfolgingstilfelle?,
    ): Boolean {
        oppfolgingstilfelle?.let {
            val motebehovStatus = motebehovStatus(
                false,
                oppfolgingstilfelle,
                true,
                motebehovList
            )

            return getNewestMotebehovInOppfolgingstilfelle(oppfolgingstilfelle, motebehovList)
                ?.let { newestMotebehov ->
                    return motebehovStatus.isSvarBehovVarselAvailable(newestMotebehov)
                } ?: motebehovStatus.isSvarBehovVarselAvailable()
        }
        return false
    }

    fun getNewestSvarBehovMotebehovInOppfolgingstilfelle(
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

    fun getNewestMeldBehovMotebehovInOppfolgingstilfelle(
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
}
