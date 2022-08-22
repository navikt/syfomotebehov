package no.nav.syfo.motebehov

import no.nav.syfo.api.exception.ConflictException
import no.nav.syfo.consumer.aktorregister.domain.Fodselsnummer
import no.nav.syfo.metric.Metric
import no.nav.syfo.motebehov.motebehovstatus.MotebehovStatusServiceV2
import no.nav.syfo.motebehov.motebehovstatus.isMotebehovAvailableForAnswer
import no.nav.syfo.oppfolgingstilfelle.OppfolgingstilfelleService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import javax.inject.Inject

@Service
class MotebehovOppfolgingstilfelleServiceV2 @Inject constructor(
    private val metric: Metric,
    private val motebehovService: MotebehovService,
    private val motebehovStatusServiceV2: MotebehovStatusServiceV2,
    private val oppfolgingstilfelleService: OppfolgingstilfelleService
) {
    fun createMotebehovForArbeidgiver(
        innloggetFnr: Fodselsnummer,
        arbeidstakerFnr: Fodselsnummer,
        isOwnLeader: Boolean,
        nyttMotebehov: NyttMotebehovArbeidsgiver
    ) {
        LOG.info("Oppretter nytt møtebehov for arbeidsgiver")

        val activeOppfolgingstilfelle = oppfolgingstilfelleService.getActiveOppfolgingstilfelleForArbeidsgiver(arbeidstakerFnr, nyttMotebehov.virksomhetsnummer)
        if (activeOppfolgingstilfelle != null) {
            val motebehovStatus = motebehovStatusServiceV2.motebehovStatusForArbeidsgiver(arbeidstakerFnr, isOwnLeader, nyttMotebehov.virksomhetsnummer)

            if (motebehovStatus.isMotebehovAvailableForAnswer()) {
                motebehovService.lagreMotebehov(
                    innloggetFnr,
                    arbeidstakerFnr,
                    nyttMotebehov.virksomhetsnummer,
                    motebehovStatus.skjemaType!!,
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
    fun createMotebehovForArbeidstaker(arbeidstakerFnr: Fodselsnummer, motebehovSvar: MotebehovSvar) {
        LOG.info("Oppretter nytt møtebehov for arbeidstaker")

        val activeOppolgingstilfelle = oppfolgingstilfelleService.getActiveOppfolgingstilfelleForArbeidstaker(arbeidstakerFnr)
        if (activeOppolgingstilfelle != null) {
            val motebehovStatus = motebehovStatusServiceV2.motebehovStatusForArbeidstaker(arbeidstakerFnr)

            val virksomhetsnummerList = if (motebehovStatus.isMotebehovAvailableForAnswer()) {
                oppfolgingstilfelleService.getActiveOppfolgingstilfeller(arbeidstakerFnr).map {
                    it.virksomhetsnummer
                }.toList()
            } else {
                emptyList()
            }

            if (virksomhetsnummerList.isNotEmpty()) {
                for (virksomhetsnummer in virksomhetsnummerList) {
                    motebehovService.lagreMotebehov(
                        arbeidstakerFnr,
                        arbeidstakerFnr,
                        virksomhetsnummer,
                        motebehovStatus.skjemaType!!,
                        motebehovSvar
                    )
                }
                metric.tellBesvarMotebehov(
                    activeOppolgingstilfelle,
                    motebehovStatus.skjemaType,
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
        private val LOG = LoggerFactory.getLogger(MotebehovOppfolgingstilfelleServiceV2::class.java)
        private const val METRIC_CREATE_FAILED_BASE = "create_motebehov_fail_no_oppfolgingstilfelle"
        private const val METRIC_CREATE_FAILED_ARBEIDSTAKER = "${METRIC_CREATE_FAILED_BASE}_arbeidstaker"
        private const val METRIC_CREATE_FAILED_ARBEIDSGIVER = "${METRIC_CREATE_FAILED_BASE}_arbeidsgiver"
    }
}
