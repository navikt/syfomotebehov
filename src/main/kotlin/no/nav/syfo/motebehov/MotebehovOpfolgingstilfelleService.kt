package no.nav.syfo.motebehov

import no.nav.syfo.consumer.aktorregister.domain.Fodselsnummer
import no.nav.syfo.metric.Metric
import no.nav.syfo.motebehov.motebehovstatus.MotebehovStatusService
import no.nav.syfo.oppfolgingstilfelle.OppfolgingstilfelleService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import javax.inject.Inject

@Service
class MotebehovOpfolgingstilfelleService @Inject constructor(
        private val metric: Metric,
        private val motebehovService: MotebehovService,
        private val motebehovStatusService: MotebehovStatusService,
        private val oppfolgingstilfelleService: OppfolgingstilfelleService
) {
    fun createMotehovForArbeidgiver(
            innloggetFnr: Fodselsnummer,
            arbeidstakerFnr: Fodselsnummer,
            nyttMotebehov: NyttMotebehovArbeidsgiver
    ) {
        if (oppfolgingstilfelleService.getActiveOppfolgingstilfelle(arbeidstakerFnr) != null) {
            val motebehovStatus = motebehovStatusService.motebehovStatusForArbeidsgiver(arbeidstakerFnr, nyttMotebehov.virksomhetsnummer)

            val isActiveOppfolgingstilfelleAvailableForAnswer = motebehovStatus.visMotebehov
                    && motebehovStatus.skjemaType != null
                    && motebehovStatus.motebehov == null

            if (isActiveOppfolgingstilfelleAvailableForAnswer) {
                motebehovService.lagreMotebehov(
                        innloggetFnr,
                        arbeidstakerFnr,
                        nyttMotebehov.virksomhetsnummer,
                        nyttMotebehov.motebehovSvar
                )
                metric.tellBesvarMotebehov(motebehovStatus.skjemaType, nyttMotebehov.motebehovSvar, false)
            } else {
                metric.tellHendelse(METRIC_CREATE_FAILED_ARBEIDSGIVER)
                throwCreateMotebehovFailed("Failed to create Motebehov for Arbeidsgiver: Found no Virksomhetsnummer with active Oppfolgingstilfelle")
            }
        } else {
            metric.tellHendelse(METRIC_CREATE_FAILED_ARBEIDSGIVER)
            throwCreateMotebehovFailed("Failed to create Motebehov for Arbeidsgiver: Found no Virksomhetsnummer with active Oppfolgingstilfelle")
        }
    }

    @Transactional
    fun createMotehovForArbeidstaker(arbeidstakerFnr: Fodselsnummer, motebehovSvar: MotebehovSvar) {
        val activeOppolgingstilfelle = oppfolgingstilfelleService.getActiveOppfolgingstilfelle(arbeidstakerFnr)
        if (activeOppolgingstilfelle != null) {
            val motebehovStatusForOppfolgingstilfelle = motebehovStatusService.motebehovStatusForArbeidstaker(arbeidstakerFnr)

            val isActiveOppfolgingstilfelleAvailableForAnswer = motebehovStatusForOppfolgingstilfelle.visMotebehov
                    && motebehovStatusForOppfolgingstilfelle.skjemaType != null
                    && motebehovStatusForOppfolgingstilfelle.motebehov == null

            val virksomhetsnummerList = if (isActiveOppfolgingstilfelleAvailableForAnswer) {
                oppfolgingstilfelleService.getActiveOppfolgingstilfeller(arbeidstakerFnr).map {
                    it.virksomhetsnummer
                }.toList()
            } else {
                emptyList()
            }

            if (virksomhetsnummerList.isNotEmpty()) {
                for (virksomhetsnummer in virksomhetsnummerList) {
                    motebehovService.lagreMotebehov(arbeidstakerFnr, arbeidstakerFnr, virksomhetsnummer, motebehovSvar)
                }
                metric.tellBesvarMotebehov(motebehovStatusForOppfolgingstilfelle.skjemaType, motebehovSvar, true)
            } else {
                metric.tellHendelse(METRIC_CREATE_FAILED_ARBEIDSTAKER)
                throwCreateMotebehovFailed("Failed to create Motebehov for Arbeidstaker: Found no Virksomhetsnummer with active Oppfolgingstilfelle")
            }
        } else {
            metric.tellHendelse(METRIC_CREATE_FAILED_ARBEIDSTAKER)
            throwCreateMotebehovFailed("Failed to create Motebehov for Arbeidstaker: Found no Virksomhetsnummer with active Oppfolgingstilfelle")
        }
    }

    private fun throwCreateMotebehovFailed(errorMessage: String) {
        LOG.error(errorMessage)
        throw RuntimeException(errorMessage);
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(MotebehovOpfolgingstilfelleService::class.java)
        private const val METRIC_CREATE_FAILED_BASE = "create_motebehov_fail_no_oppfolgingstilfelle"
        private const val METRIC_CREATE_FAILED_ARBEIDSTAKER = "${METRIC_CREATE_FAILED_BASE}_arbeidstaker"
        private const val METRIC_CREATE_FAILED_ARBEIDSGIVER = "${METRIC_CREATE_FAILED_BASE}_arbeidsgiver"
    }
}
