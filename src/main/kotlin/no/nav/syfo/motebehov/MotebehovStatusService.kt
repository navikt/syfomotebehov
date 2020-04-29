package no.nav.syfo.motebehov

import no.nav.syfo.aktorregister.AktorregisterConsumer
import no.nav.syfo.aktorregister.domain.AktorId
import no.nav.syfo.aktorregister.domain.Fodselsnummer
import no.nav.syfo.mote.MoterService
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
        private val moterService: MoterService,
        private val oppfolgingstilfelleService: OppfolgingstilfelleService
) {
    fun motebehovStatus(
            arbeidstakerFnr: Fodselsnummer,
            virksomhetsnummer: String
    ): MotebehovStatus {
        val motebehovList = if (virksomhetsnummer.isNotEmpty()) {
            motebehovService.hentMotebehovListeForArbeidstakerOpprettetAvLeder(arbeidstakerFnr, virksomhetsnummer)
        } else {
            motebehovService.hentMotebehovListeForOgOpprettetAvArbeidstaker(arbeidstakerFnr)
        }
        val arbeidstakerAktorId = aktorregisterConsumer.getAktorIdForFodselsnummer(arbeidstakerFnr)
        val oppfolgingstilfelle = oppfolgingstilfelleService.getOppfolgingstilfelle(
                AktorId(arbeidstakerAktorId),
                virksomhetsnummer
        )
        return if (oppfolgingstilfelle != null && isMotebehovAvailable(oppfolgingstilfelle, motebehovList)) {
            MotebehovStatus(
                    true,
                    getMotebehovSkjemaType(),
                    getMotebehovInOppfolgingstilfelle(oppfolgingstilfelle, motebehovList)
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
        } else !moterService.erMoteOpprettetForArbeidstakerEtterDato(oppfolgingstilfelle.aktorId, oppfolgingstilfelle.fom.atStartOfDay())
    }

    private fun isDateInOppfolgingstilfelle(date: LocalDate, oppfolgingstilfelle: PersonOppfolgingstilfelle): Boolean {
        return date.isAfter(oppfolgingstilfelle.fom.minusDays(1)) && date.isBefore(oppfolgingstilfelle.tom.plusDays(1))
    }

    private fun isDialogmote2BehovAvailable(oppfolgingstilfelle: PersonOppfolgingstilfelle): Boolean {
        val today = LocalDate.now()

        val startDialogmote2BehovAvailability = oppfolgingstilfelle.fom.plusDays(DAYS_START_DIALOGMOTE2)
        val endDialogmote2BehovAvailability = oppfolgingstilfelle.fom.plusDays(DAYS_END_DIALOGMOTE2)

        return today.isAfter(startDialogmote2BehovAvailability.minusDays(1)) && today.isBefore(endDialogmote2BehovAvailability)
    }

    private fun hasMotebehovInOppfolgingstilfelle(oppfolgingstilfelle: PersonOppfolgingstilfelle, motebehovList: List<Motebehov>): Boolean {
        return getMotebehovInOppfolgingstilfelle(oppfolgingstilfelle, motebehovList) != null
    }

    private fun getMotebehovInOppfolgingstilfelle(oppfolgingstilfelle: PersonOppfolgingstilfelle, motebehovList: List<Motebehov>): Motebehov? {
        val motebehovInOppfolgingstilfelleList = motebehovList.filter {
            isDateInOppfolgingstilfelle(it.opprettetDato.toLocalDate(), oppfolgingstilfelle)
        }
        return if (motebehovInOppfolgingstilfelleList.isNotEmpty()) motebehovInOppfolgingstilfelleList[0] else null
    }

    private fun getMotebehovSkjemaType(): MotebehovSkjemaType {
        return MotebehovSkjemaType.SVAR_BEHOV
    }
}
