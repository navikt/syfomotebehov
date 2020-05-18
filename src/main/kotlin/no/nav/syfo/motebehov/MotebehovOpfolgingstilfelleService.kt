package no.nav.syfo.motebehov

import no.nav.syfo.consumer.aktorregister.domain.Fodselsnummer
import no.nav.syfo.motebehov.motebehovstatus.MotebehovStatusService
import no.nav.syfo.oppfolgingstilfelle.OppfolgingstilfelleService
import no.nav.syfo.oppfolgingstilfelle.database.PersonOppfolgingstilfelle
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import javax.inject.Inject

@Service
class MotebehovOpfolgingstilfelleService @Inject constructor(
        private val motebehovService: MotebehovService,
        private val motebehovStatusService: MotebehovStatusService,
        private val oppfolgingstilfelleService: OppfolgingstilfelleService
) {
    @Transactional
    fun createMotehovForArbeidstaker(innloggetFNR: Fodselsnummer, motebehovSvar: MotebehovSvar) {
        val oppfolgingstilfeller = oppfolgingstilfellerAvailableForArbeidstakerAnswer(innloggetFNR)
        for (oppfolgingstilfelle in oppfolgingstilfeller) {
            motebehovService.lagreMotebehov(innloggetFNR, innloggetFNR, oppfolgingstilfelle.virksomhetsnummer, motebehovSvar)
        }
    }

    private fun oppfolgingstilfellerAvailableForArbeidstakerAnswer(
            arbeidstakerFnr: Fodselsnummer
    ): List<PersonOppfolgingstilfelle> {
        val motebehovList: List<Motebehov> = motebehovService.hentMotebehovListeForOgOpprettetAvArbeidstaker(arbeidstakerFnr)
        return oppfolgingstilfelleService.getOppfolgingstilfeller(arbeidstakerFnr).filter {
            val motebehovStatusForOppfolgingstilfelle = motebehovStatusService.motebehovStatus(listOf(it), motebehovList)
            motebehovStatusForOppfolgingstilfelle.visMotebehov && motebehovStatusForOppfolgingstilfelle.motebehov == null
        }
    }
}
