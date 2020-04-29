package no.nav.syfo.motebehov.motebehovstatus

import no.nav.syfo.consumer.aktorregister.AktorregisterConsumer
import no.nav.syfo.consumer.aktorregister.domain.AktorId
import no.nav.syfo.consumer.aktorregister.domain.Fodselsnummer
import no.nav.syfo.consumer.mote.MoteConsumer
import no.nav.syfo.motebehov.Motebehov
import no.nav.syfo.motebehov.MotebehovService
import no.nav.syfo.oppfolgingstilfelle.OppfolgingstilfelleService
import no.nav.syfo.oppfolgingstilfelle.database.PersonOppfolgingstilfelle
import org.springframework.stereotype.Service
import java.time.LocalDate
import javax.inject.Inject

const val WEEKS_START_DIALOGMOTE2 = 16
const val DAYS_START_DIALOGMOTE2 = WEEKS_START_DIALOGMOTE2 * 7L
const val WEEKS_END_DIALOGMOTE2 = 26
const val DAYS_END_DIALOGMOTE2 = WEEKS_END_DIALOGMOTE2 * 7L

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
        val arbeidstakerAktorId = AktorId(aktorregisterConsumer.getAktorIdForFodselsnummer(arbeidstakerFnr))
        val oppfolgingstilfeller: List<PersonOppfolgingstilfelle> = oppfolgingstilfelleService.getOppfolgingstilfeller(arbeidstakerAktorId)
        val motebehovList: List<Motebehov> = motebehovService.hentMotebehovListeForOgOpprettetAvArbeidstaker(arbeidstakerFnr)

        return motebehovStatus(oppfolgingstilfeller, motebehovList)
    }

    fun motebehovStatusForArbeidsgiver(
            arbeidstakerFnr: Fodselsnummer,
            virksomhetsnummer: String
    ): MotebehovStatus {
        val arbeidstakerAktorId = AktorId(aktorregisterConsumer.getAktorIdForFodselsnummer(arbeidstakerFnr))
        val oppfolgingstilfeller = oppfolgingstilfelleService.getOppfolgingstilfeller(
                arbeidstakerAktorId,
                virksomhetsnummer
        )
        val motebehovList = motebehovService.hentMotebehovListeForArbeidstakerOpprettetAvLeder(arbeidstakerFnr, virksomhetsnummer)

        return motebehovStatus(oppfolgingstilfeller, motebehovList)
    }

    private fun motebehovStatus(
            oppfolgingstilfeller: List<PersonOppfolgingstilfelle>,
            motebehovList: List<Motebehov>
    ): MotebehovStatus {
        return if (oppfolgingstilfeller.any { isMotebehovAvailable(it, motebehovList) }) {
            MotebehovStatus(
                    true,
                    getMotebehovSkjemaType(),
                    getNewestMotebehovInOppfolgingstilfeller(oppfolgingstilfeller, motebehovList)
            )
        } else {
            MotebehovStatus(
                    false,
                    null,
                    null
            )
        }
    }

    private fun isMotebehovAvailable(oppfolgingstilfelle: PersonOppfolgingstilfelle, motebehovList: List<Motebehov>): Boolean {
        return if (!isDateInOppfolgingstilfelle(LocalDate.now(), oppfolgingstilfelle) || !isDialogmote2BehovAvailable(oppfolgingstilfelle)) {
            false
        } else if (hasMotebehovInOppfolgingstilfelle(oppfolgingstilfelle, motebehovList)) {
            true
        } else {
            !moteConsumer.erMoteOpprettetForArbeidstakerEtterDato(oppfolgingstilfelle.aktorId, oppfolgingstilfelle.fom.atStartOfDay())
        }
    }

    private fun isDialogmote2BehovAvailable(oppfolgingstilfelle: PersonOppfolgingstilfelle): Boolean {
        val startDialogmote2BehovAvailability = oppfolgingstilfelle.fom.plusDays(DAYS_START_DIALOGMOTE2)
        val endDialogmote2BehovAvailability = oppfolgingstilfelle.fom.plusDays(DAYS_END_DIALOGMOTE2)
        val today = LocalDate.now()

        return today.isAfter(startDialogmote2BehovAvailability.minusDays(1)) && today.isBefore(endDialogmote2BehovAvailability)
    }

    private fun hasMotebehovInOppfolgingstilfelle(oppfolgingstilfelle: PersonOppfolgingstilfelle, motebehovList: List<Motebehov>): Boolean {
        return getNewestMotebehovInOppfolgingstilfeller(listOf(oppfolgingstilfelle), motebehovList) != null
    }

    private fun getNewestMotebehovInOppfolgingstilfeller(oppfolgingstilfeller: List<PersonOppfolgingstilfelle>, motebehovList: List<Motebehov>): Motebehov? {
        if (motebehovList.isNotEmpty()) {
            val newestMotebehov = motebehovList[0]
            val isNewestMotebehovInOppfolgingstilfelle = oppfolgingstilfeller.any {
                isDateInOppfolgingstilfelle(newestMotebehov.opprettetDato.toLocalDate(), it)
            }
            if (isNewestMotebehovInOppfolgingstilfelle) {
                return newestMotebehov
            }
            return null
        } else {
            return null
        }
    }

    private fun isDateInOppfolgingstilfelle(date: LocalDate, oppfolgingstilfelle: PersonOppfolgingstilfelle): Boolean {
        return date.isAfter(oppfolgingstilfelle.fom.minusDays(1)) && date.isBefore(oppfolgingstilfelle.tom.plusDays(1))
    }

    private fun getMotebehovSkjemaType(): MotebehovSkjemaType {
        return MotebehovSkjemaType.SVAR_BEHOV
    }
}
