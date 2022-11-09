package no.nav.syfo.motebehov

import no.nav.syfo.api.exception.ConflictException
import no.nav.syfo.metric.Metric
import no.nav.syfo.motebehov.motebehovstatus.MotebehovStatusService
import no.nav.syfo.oppfolgingstilfelle.OppfolgingstilfelleService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import javax.inject.Inject

@Service
class MotebehovOppfolgingstilfelleService @Inject constructor(
    private val metric: Metric,
    private val motebehovService: MotebehovService,
    private val motebehovStatusService: MotebehovStatusService,
    private val oppfolgingstilfelleService: OppfolgingstilfelleService
) {
    fun createMotehovForArbeidgiver(
        innloggetFnr: String,
        arbeidstakerFnr: String,
        isOwnLeader: Boolean,
        nyttMotebehov: NyttMotebehovArbeidsgiver
    ) {
        val activeOppfolgingstilfelle = oppfolgingstilfelleService.getActiveOppfolgingstilfelleForArbeidsgiver(arbeidstakerFnr, nyttMotebehov.virksomhetsnummer)
        if (activeOppfolgingstilfelle != null) {
            val motebehovStatus = motebehovStatusService.motebehovStatusForArbeidsgiver(arbeidstakerFnr, isOwnLeader, nyttMotebehov.virksomhetsnummer)

            val isActiveOppfolgingstilfelleAvailableForAnswer = motebehovStatus.visMotebehov &&
                motebehovStatus.skjemaType != null &&
                motebehovStatus.motebehov == null

            if (isActiveOppfolgingstilfelleAvailableForAnswer && motebehovStatus.skjemaType != null) {
                motebehovService.lagreMotebehov(
                    innloggetFnr,
                    arbeidstakerFnr,
                    nyttMotebehov.virksomhetsnummer,
                    motebehovStatus.skjemaType,
                    nyttMotebehov.motebehovSvar
                )
                metric.tellBesvarMotebehov(
                    activeOppfolgingstilfelle,
                    motebehovStatus.skjemaType,
                    nyttMotebehov.motebehovSvar,
                    false
                )
            } else {
                metric.tellHendelse(METRIC_CREATE_FAILED_ARBEIDSGIVER)
                throwCreateMotebehovConflict("Failed to create Motebehov for Arbeidsgiver: Found no Virksomhetsnummer with active Oppfolgingstilfelle available for answer")
            }
        } else {
            metric.tellHendelse(METRIC_CREATE_FAILED_ARBEIDSGIVER)
            throwCreateMotebehovFailed("Failed to create Motebehov for Arbeidsgiver: Found no Virksomhetsnummer with active Oppfolgingstilfelle for ${nyttMotebehov.virksomhetsnummer}")
        }
    }

    @Transactional
    fun createMotehovForArbeidstaker(arbeidstakerFnr: String, motebehovSvar: MotebehovSvar) {
        val activeOppolgingstilfelle = oppfolgingstilfelleService.getActiveOppfolgingstilfelleForArbeidstaker(arbeidstakerFnr)
        if (activeOppolgingstilfelle != null) {
            val motebehovStatusForOppfolgingstilfelle = motebehovStatusService.motebehovStatusForArbeidstaker(arbeidstakerFnr)

            val isActiveOppfolgingstilfelleAvailableForAnswer = motebehovStatusForOppfolgingstilfelle.visMotebehov &&
                motebehovStatusForOppfolgingstilfelle.skjemaType != null &&
                motebehovStatusForOppfolgingstilfelle.motebehov == null

            val virksomhetsnummerList = if (isActiveOppfolgingstilfelleAvailableForAnswer) {
                oppfolgingstilfelleService.getActiveOppfolgingstilfeller(arbeidstakerFnr).map {
                    it.virksomhetsnummer
                }.toList()
            } else {
                emptyList()
            }

            if (virksomhetsnummerList.isNotEmpty() && motebehovStatusForOppfolgingstilfelle.skjemaType != null) {
                for (virksomhetsnummer in virksomhetsnummerList) {
                    motebehovService.lagreMotebehov(
                        arbeidstakerFnr,
                        arbeidstakerFnr,
                        virksomhetsnummer,
                        motebehovStatusForOppfolgingstilfelle.skjemaType,
                        motebehovSvar
                    )
                }
                metric.tellBesvarMotebehov(
                    activeOppolgingstilfelle,
                    motebehovStatusForOppfolgingstilfelle.skjemaType,
                    motebehovSvar,
                    true
                )
            } else {
                metric.tellHendelse(METRIC_CREATE_FAILED_ARBEIDSTAKER)
                throwCreateMotebehovConflict("Failed to create Motebehov for Arbeidstaker: Found no Virksomhetsnummer with active Oppfolgingstilfelle")
            }
        } else {
            metric.tellHendelse(METRIC_CREATE_FAILED_ARBEIDSTAKER)
            throwCreateMotebehovFailed("Failed to create Motebehov for Arbeidstaker: Found no Virksomhetsnummer with active Oppfolgingstilfelle")
        }
    }

    private fun throwCreateMotebehovConflict(errorMessage: String) {
        LOG.warn(errorMessage)
        throw ConflictException()
    }

    private fun throwCreateMotebehovFailed(errorMessage: String) {
        LOG.error(errorMessage)
        throw RuntimeException(errorMessage)
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(MotebehovOppfolgingstilfelleService::class.java)
        private const val METRIC_CREATE_FAILED_BASE = "create_motebehov_fail_no_oppfolgingstilfelle"
        private const val METRIC_CREATE_FAILED_ARBEIDSTAKER = "${METRIC_CREATE_FAILED_BASE}_arbeidstaker"
        private const val METRIC_CREATE_FAILED_ARBEIDSGIVER = "${METRIC_CREATE_FAILED_BASE}_arbeidsgiver"
    }
}
