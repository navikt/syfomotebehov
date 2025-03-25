package no.nav.syfo.motebehov

import no.nav.syfo.api.exception.ConflictException
import no.nav.syfo.metric.Metric
import no.nav.syfo.motebehov.motebehovstatus.MotebehovSkjemaType
import no.nav.syfo.motebehov.motebehovstatus.MotebehovStatusServiceV2
import no.nav.syfo.motebehov.motebehovstatus.isMotebehovAvailableForAnswer
import no.nav.syfo.oppfolgingstilfelle.OppfolgingstilfelleService
import no.nav.syfo.varsel.VarselServiceV2
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import javax.inject.Inject

@Service
class MotebehovOppfolgingstilfelleServiceV2 @Inject constructor(
    private val metric: Metric,
    private val motebehovService: MotebehovService,
    private val motebehovStatusServiceV2: MotebehovStatusServiceV2,
    private val oppfolgingstilfelleService: OppfolgingstilfelleService,
    private val varselServiceV2: VarselServiceV2,
) {
    /**
     * Creates a arbeidsgiver-motebehov if there is an active oppfolgingstilfelle for the arbeidstaker and if the
     * calculated motebehovStatus indicates that the arbeidsgiver can submit a motebehov for the arbeidstaker at this
     * time. If this is a "svar behov" (not "meld behov"), the related varsel or varsler will be ferdigstilt.
     */
    fun createMotebehovForArbeidgiver(
        innloggetFnr: String,
        arbeidstakerFnr: String,
        isOwnLeader: Boolean,
        nyttMotebehov: NyttMotebehovArbeidsgiverDTO,
    ) {
        val activeOppfolgingstilfelle = oppfolgingstilfelleService.getActiveOppfolgingstilfelleForArbeidsgiver(
            arbeidstakerFnr,
            nyttMotebehov.virksomhetsnummer
        )

        val activeOppfolgingstilfelleExists = activeOppfolgingstilfelle != null

        val motebehovStatus = motebehovStatusServiceV2.motebehovStatusForArbeidsgiver(
            arbeidstakerFnr,
            isOwnLeader,
            nyttMotebehov.virksomhetsnummer
        )

        if (activeOppfolgingstilfelleExists && motebehovStatus.isMotebehovAvailableForAnswer()) {
            val storedMotebehovSvar = storeNyttMotebehovForArbeidsgiver(
                arbeidstakerFnr,
                nyttMotebehov,
                innloggetFnr,
                motebehovStatus.skjemaType,
            )

            metric.tellBesvarMotebehov(
                activeOppfolgingstilfelle!!,
                motebehovStatus.skjemaType,
                storedMotebehovSvar,
                false,
            )

            if (motebehovStatus.skjemaType == MotebehovSkjemaType.SVAR_BEHOV) {
                ferdigstillVarselForSvarMotebehovForArbeidsgiver(
                    arbeidstakerFnr,
                    innloggetFnr,
                    nyttMotebehov,
                    isOwnLeader
                )
            }
        } else {
            if (!activeOppfolgingstilfelleExists) {
                metric.tellHendelse(METRIC_CREATE_FAILED_ARBEIDSGIVER)
                throwCreateMotebehovFailed(
                    "Failed to create Motebehov for Arbeidsgiver:" +
                        "Found no active Oppfolgingstilfelle for ${nyttMotebehov.virksomhetsnummer}"
                )
            }
            if (!motebehovStatus.isMotebehovAvailableForAnswer()) {
                metric.tellHendelse(METRIC_CREATE_FAILED_ARBEIDSGIVER)
                throwCreateMotebehovConflict(
                    "Failed to create Motebehov for Arbeidsgiver:" +
                        "Found no Virksomhetsnummer with active Oppfolgingstilfelle available for answer"
                )
            }
        }
    }

    private fun storeNyttMotebehovForArbeidsgiver(
        arbeidstakerFnr: String,
        nyttMotebehov: NyttMotebehovArbeidsgiverDTO,
        innloggetFnr: String,
        skjemaType: MotebehovSkjemaType?,
    ): MotebehovSvar {
        val motebehovSvarToStore = MotebehovSvar(
            harMotebehov = nyttMotebehov.motebehovSvarInputDTO.harMotebehov,
            forklaring = nyttMotebehov.motebehovSvarInputDTO.forklaring,
            formFillout = nyttMotebehov.motebehovSvarInputDTO.formFillout
        )

        motebehovService.lagreMotebehov(
            innloggetFnr,
            arbeidstakerFnr,
            nyttMotebehov.virksomhetsnummer,
            skjemaType!!,
            motebehovSvarToStore,
        )

        return motebehovSvarToStore
    }

    private fun ferdigstillVarselForSvarMotebehovForArbeidsgiver(
        arbeidstakerFnr: String,
        innloggetFnr: String,
        nyttMotebehov: NyttMotebehovArbeidsgiverDTO,
        isOwnLeader: Boolean
    ) {
        varselServiceV2.ferdigstillSvarMotebehovVarselForNarmesteLeder(
            arbeidstakerFnr,
            innloggetFnr,
            nyttMotebehov.virksomhetsnummer
        )
        if (isOwnLeader) {
            varselServiceV2.ferdigstillSvarMotebehovVarselForArbeidstaker(arbeidstakerFnr)
        }
    }

    @Transactional
    fun createMotebehovForArbeidstaker(
        arbeidstakerFnr: String,
        nyttMotebehovSvar: TemporaryCombinedNyttMotebehovSvar
    ) {
        val activeOppolgingstilfelle =
            oppfolgingstilfelleService.getActiveOppfolgingstilfelleForArbeidstaker(arbeidstakerFnr)
        log.info(
            """We have gotten the active oppfolgingstilfelle for the arbeidstaker 
            ${activeOppolgingstilfelle?.fom} and ${activeOppolgingstilfelle?.tom}
            """.trimMargin()
        )

        if (activeOppolgingstilfelle != null) {
            val motebehovStatus = motebehovStatusServiceV2.motebehovStatusForArbeidstaker(arbeidstakerFnr)

            val virksomhetsnummerList = if (motebehovStatus.isMotebehovAvailableForAnswer()) {
                oppfolgingstilfelleService.getActiveOppfolgingstilfeller(arbeidstakerFnr).map {
                    it.virksomhetsnummer
                }.toList()
            } else {
                emptyList()
            }

            val motebehovSvar = MotebehovSvar(
                harMotebehov = nyttMotebehovSvar.harMotebehov,
                forklaring = nyttMotebehovSvar.forklaring,
                formFillout = nyttMotebehovSvar.formFillout
            )

            if (virksomhetsnummerList.isNotEmpty()) {
                for (virksomhetsnummer in virksomhetsnummerList) {
                    motebehovService.lagreMotebehov(
                        arbeidstakerFnr,
                        arbeidstakerFnr,
                        virksomhetsnummer,
                        motebehovStatus.skjemaType!!,
                        motebehovSvar,
                    )
                }
                metric.tellBesvarMotebehov(
                    activeOppolgingstilfelle,
                    motebehovStatus.skjemaType,
                    motebehovSvar,
                    true,
                )
                if (motebehovStatus.skjemaType == MotebehovSkjemaType.SVAR_BEHOV) {
                    varselServiceV2.ferdigstillSvarMotebehovVarselForArbeidstaker(arbeidstakerFnr)
                }
            } else {
                metric.tellHendelse(METRIC_CREATE_FAILED_ARBEIDSTAKER)
                throwCreateMotebehovConflict("Failed to create Motebehov for Arbeidstaker: Found no Virksomhetsnummer with active Oppfolgingstilfelle")
            }
        } else {
            metric.tellHendelse(METRIC_CREATE_FAILED_ARBEIDSTAKER)
            throwCreateMotebehovFailed(
                """Failed to create Motebehov for Arbeidstaker: 
                Found no active Oppfolgingstilfelle
                """.trimMargin()
            )
        }
    }

    fun ferdigstillMotebehov(arbeidstakerFnr: String) {
        varselServiceV2.ferdigstillSvarMotebehovVarselForArbeidstaker(arbeidstakerFnr)
    }

    private fun throwCreateMotebehovConflict(errorMessage: String) {
        log.warn(errorMessage)
        throw ConflictException()
    }

    private fun throwCreateMotebehovFailed(errorMessage: String) {
        log.error(errorMessage)
        throw RuntimeException(errorMessage)
    }

    companion object {
        private val log = LoggerFactory.getLogger(MotebehovOppfolgingstilfelleServiceV2::class.java)
        private const val METRIC_CREATE_FAILED_BASE = "create_motebehov_fail_no_oppfolgingstilfelle"
        private const val METRIC_CREATE_FAILED_ARBEIDSTAKER = "${METRIC_CREATE_FAILED_BASE}_arbeidstaker"
        private const val METRIC_CREATE_FAILED_ARBEIDSGIVER = "${METRIC_CREATE_FAILED_BASE}_arbeidsgiver"
    }
}
