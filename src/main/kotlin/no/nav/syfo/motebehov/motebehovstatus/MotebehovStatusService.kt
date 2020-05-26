package no.nav.syfo.motebehov.motebehovstatus

import no.nav.syfo.consumer.aktorregister.AktorregisterConsumer
import no.nav.syfo.consumer.aktorregister.domain.Fodselsnummer
import no.nav.syfo.consumer.mote.MoteConsumer
import no.nav.syfo.motebehov.*
import no.nav.syfo.oppfolgingstilfelle.OppfolgingstilfelleService
import no.nav.syfo.oppfolgingstilfelle.database.PersonOppfolgingstilfelle
import org.springframework.stereotype.Service
import java.time.LocalDate
import javax.inject.Inject

const val WEEKS_START_SVAR_BEHOV = 16
const val DAYS_START_SVAR_BEHOV = WEEKS_START_SVAR_BEHOV * 7L
const val WEEKS_END_SVAR_BEHOV = 26
const val DAYS_END_SVAR_BEHOV = WEEKS_END_SVAR_BEHOV * 7L

@Service
class MotebehovStatusService @Inject constructor(
        private val aktorregisterConsumer: AktorregisterConsumer,
        private val motebehovService: MotebehovService,
        private val moteConsumer: MoteConsumer,
        private val oppfolgingstilfelleService: OppfolgingstilfelleService
) {
    fun motebehovStatusForArbeidstaker(
            arbeidstakerFnr: Fodselsnummer
    ): MotebehovStatus {
        val oppfolgingstilfelle = oppfolgingstilfelleService.getActiveOppfolgingstilfelleForArbeidstaker(arbeidstakerFnr)
        val motebehovList: List<Motebehov> = motebehovService.hentMotebehovListeForOgOpprettetAvArbeidstaker(arbeidstakerFnr)

        return motebehovStatus(oppfolgingstilfelle, motebehovList)
    }

    fun motebehovStatusForArbeidsgiver(
            arbeidstakerFnr: Fodselsnummer,
            virksomhetsnummer: String
    ): MotebehovStatus {
        val oppfolgingstilfelle = oppfolgingstilfelleService.getActiveOppfolgingstilfelleForArbeidsgiver(arbeidstakerFnr, virksomhetsnummer)
        val motebehovList = motebehovService.hentMotebehovListeForArbeidstakerOpprettetAvLeder(arbeidstakerFnr, virksomhetsnummer)

        return motebehovStatus(oppfolgingstilfelle, motebehovList)
    }

    fun motebehovStatus(
            oppfolgingstilfelle: PersonOppfolgingstilfelle?,
            motebehovList: List<Motebehov>
    ): MotebehovStatus {
        oppfolgingstilfelle?.let {
            return when (val motebehovSkjemaType = getMotebehovSkjemaType(oppfolgingstilfelle, motebehovList)) {
                MotebehovSkjemaType.SVAR_BEHOV -> {
                    MotebehovStatus(
                            true,
                            motebehovSkjemaType,
                            getNewestSvarBehovMotebehovInOppfolgingstilfelle(oppfolgingstilfelle, motebehovList)
                    )
                }
                MotebehovSkjemaType.MELD_BEHOV -> {
                    MotebehovStatus(
                            true,
                            motebehovSkjemaType,
                            getNewestMeldBehovMotebehovInOppfolgingstilfelle(oppfolgingstilfelle, motebehovList)
                    )
                }
                else -> {
                    MotebehovStatus(
                            false,
                            null,
                            null
                    )
                }
            }
        }
        return MotebehovStatus(
                false,
                null,
                null
        )
    }

    private fun getMotebehovSkjemaType(
            oppfolgingstilfelle: PersonOppfolgingstilfelle,
            motebehovList: List<Motebehov>
    ): MotebehovSkjemaType? {
        return when {
            isSvarMotebehovAvailable(oppfolgingstilfelle, motebehovList) -> {
                MotebehovSkjemaType.SVAR_BEHOV
            }
            isMeldMotebehovAvailable(oppfolgingstilfelle, motebehovList) -> {
                MotebehovSkjemaType.MELD_BEHOV
            }
            else -> {
                null
            }
        }
    }

    private fun isSvarMotebehovAvailable(
            oppfolgingstilfelle: PersonOppfolgingstilfelle,
            motebehovList: List<Motebehov>
    ): Boolean {
        return if (!isSvarBehovAvailable(oppfolgingstilfelle)) {
            false
        } else if (hasMeldMotebehovInOppfolgingstilfelle(oppfolgingstilfelle, motebehovList)) {
            true
        } else {
            val arbeidstakerAktorId = aktorregisterConsumer.getAktorIdForFodselsnummer(oppfolgingstilfelle.fnr)
            !moteConsumer.erMoteOpprettetForArbeidstakerEtterDato(arbeidstakerAktorId, oppfolgingstilfelle.fom.atStartOfDay())
        }
    }

    private fun isSvarBehovAvailable(oppfolgingstilfelle: PersonOppfolgingstilfelle): Boolean {
        val firstDateSvarBehovAvailability = oppfolgingstilfelle.fom.plusDays(DAYS_START_SVAR_BEHOV)
        val lastDateSvarBehovAvailability = oppfolgingstilfelle.fom.plusDays(DAYS_END_SVAR_BEHOV).minusDays(1)
        val today = LocalDate.now()

        return isTodayInOppfolgingstilfelle(oppfolgingstilfelle)
                && today.isAfter(firstDateSvarBehovAvailability.minusDays(1))
                && today.isBefore(lastDateSvarBehovAvailability.plusDays(1))
    }

    private fun isTodayInOppfolgingstilfelle(oppfolgingstilfelle: PersonOppfolgingstilfelle): Boolean {
        val today = LocalDate.now()
        return today.isAfter(oppfolgingstilfelle.fom.minusDays(1))
                && today.isBefore(oppfolgingstilfelle.tom.plusDays(1))
    }

    private fun isMeldMotebehovAvailable(
            oppfolgingstilfelle: PersonOppfolgingstilfelle,
            motebehovList: List<Motebehov>
    ): Boolean {
        return if (!isMeldMotebehovAvailable(oppfolgingstilfelle)) {
            false
        } else if (hasSvarMotebehovInOppfolgingstilfelle(oppfolgingstilfelle, motebehovList)) {
            true
        } else {
            !moteConsumer.isMoteplanleggerActiveInOppfolgingstilfelle(oppfolgingstilfelle)
        }
    }

    private fun isMeldMotebehovAvailable(oppfolgingstilfelle: PersonOppfolgingstilfelle): Boolean {
        val firstDateSvarBehovAvailability = oppfolgingstilfelle.fom.plusDays(DAYS_START_SVAR_BEHOV)
        val lastDateSvarBehovAvailability = oppfolgingstilfelle.fom.plusDays(DAYS_END_SVAR_BEHOV).minusDays(1)
        val today = LocalDate.now()

        val isTodayInFirstMeldMotebehovPeriod = today.isAfter(oppfolgingstilfelle.fom.minusDays(1)) && today.isBefore(firstDateSvarBehovAvailability)
        val isTodayInSecondMeldMotebehovPeriod = today.isAfter(lastDateSvarBehovAvailability) && today.isBefore(oppfolgingstilfelle.tom)

        return isTodayInOppfolgingstilfelle(oppfolgingstilfelle) &&
                (isTodayInFirstMeldMotebehovPeriod || isTodayInSecondMeldMotebehovPeriod)
    }


    private fun hasMeldMotebehovInOppfolgingstilfelle(
            oppfolgingstilfelle: PersonOppfolgingstilfelle,
            motebehovList: List<Motebehov>
    ): Boolean {
        return getNewestMeldBehovMotebehovInOppfolgingstilfelle(oppfolgingstilfelle, motebehovList) != null
    }

    private fun hasSvarMotebehovInOppfolgingstilfelle(
            oppfolgingstilfelle: PersonOppfolgingstilfelle,
            motebehovList: List<Motebehov>
    ): Boolean {
        return getNewestSvarBehovMotebehovInOppfolgingstilfelle(oppfolgingstilfelle, motebehovList) != null
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
        val motebehovListCreatedInOppfolgingstilfelle = motebehovList.filter { it.isCreatedInOppfolgingstilfelle(oppfolgingstilfelle) }
        return if (motebehovListCreatedInOppfolgingstilfelle.isNotEmpty()) {
            motebehovListCreatedInOppfolgingstilfelle.first()
        } else {
            null
        }
    }
}
