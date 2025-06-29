package no.nav.syfo.motebehov.api

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.syfo.api.auth.tokenX.TokenXUtil
import no.nav.syfo.api.auth.tokenX.TokenXUtil.TokenXIssuer
import no.nav.syfo.api.auth.tokenX.TokenXUtil.fnrFromIdportenTokenX
import no.nav.syfo.metric.Metric
import no.nav.syfo.motebehov.MotebehovFormSubmissionCombinedDTO
import no.nav.syfo.motebehov.MotebehovOppfolgingstilfelleServiceV2
import no.nav.syfo.motebehov.MotebehovSvarLegacyDTO
import no.nav.syfo.motebehov.motebehovstatus.MotebehovSkjemaType
import no.nav.syfo.motebehov.motebehovstatus.MotebehovStatus
import no.nav.syfo.motebehov.motebehovstatus.MotebehovStatusServiceV2
import no.nav.syfo.motebehov.motebehovstatus.MotebehovStatusWithLegacyMotebehovDTO
import no.nav.syfo.motebehov.motebehovstatus.toMotebehovStatusWithLegacyMotebehovDTO
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import javax.inject.Inject
import javax.validation.Valid

@RestController
@ProtectedWithClaims(
    issuer = TokenXIssuer.TOKENX,
    claimMap = ["acr=Level4", "acr=idporten-loa-high"],
    combineWithOr = true
)
@RequestMapping(value = ["/api/v3/arbeidstaker"])
class MotebehovArbeidstakerControllerV3 @Inject constructor(
    private val contextHolder: TokenValidationContextHolder,
    private val metric: Metric,
    private val motebehovStatusServiceV2: MotebehovStatusServiceV2,
    private val motebehovOppfolgingstilfelleServiceV2: MotebehovOppfolgingstilfelleServiceV2,
    @Value("\${dialogmote.frontend.client.id}")
    val dialogmoteClientId: String,
    @Value("\${ditt.sykefravaer.frontend.client.id}")
    val dittSykefravaerClientId: String,
    @Value("\${esyfo-proxy.client.id}")
    val esyfoProxyClientId: String,
) {
    @GetMapping(
        value = ["/motebehov"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun motebehovStatusArbeidstaker(): MotebehovStatus {
        TokenXUtil.validateTokenXClaims(
            contextHolder,
            dittSykefravaerClientId,
        )
            .fnrFromIdportenTokenX()

        metric.tellEndepunktKall("call_endpoint_motebehovstatus_arbeidstaker")

        // This endpoint is only used by Ditt sykefravær. Should be removed when they stop calling it. Until then, return empty result
        return MotebehovStatus(
            false,
            MotebehovSkjemaType.MELD_BEHOV,
            null,
        )
    }

    @GetMapping(
        value = ["/motebehov/all"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun motebehovStatusArbeidstakerWithCodeSixUsers(): MotebehovStatusWithLegacyMotebehovDTO {
        val arbeidstakerFnr = TokenXUtil.validateTokenXClaims(
            contextHolder,
            dialogmoteClientId,
            esyfoProxyClientId
        )
            .fnrFromIdportenTokenX()

        metric.tellEndepunktKall("call_endpoint_motebehovstatus_arbeidstaker_all")

        return motebehovStatusServiceV2.motebehovStatusForArbeidstaker(arbeidstakerFnr)
            .toMotebehovStatusWithLegacyMotebehovDTO()
    }

    // Currently used POST-endpoint to phase out
    @PostMapping(
        value = ["/motebehov"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun submitMotebehovArbeidstaker(
        @RequestBody nyttMotebehovSvar: @Valid MotebehovSvarLegacyDTO,
    ) {
        metric.tellEndepunktKall("call_endpoint_save_motebehov_arbeidstaker")
        val arbeidstakerFnr = TokenXUtil.validateTokenXClaims(
            contextHolder,
            dialogmoteClientId,
            esyfoProxyClientId
        )
            .fnrFromIdportenTokenX()

        val formSubmission = MotebehovFormSubmissionCombinedDTO(
            harMotebehov = nyttMotebehovSvar.harMotebehov,
            forklaring = nyttMotebehovSvar.forklaring,
            formSnapshot = null,
        )

        motebehovOppfolgingstilfelleServiceV2.createMotebehovForArbeidstaker(
            arbeidstakerFnr,
            formSubmission,
        )
    }

    @PostMapping(
        value = ["/motebehov/ferdigstill"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun ferdigstillMotebehovArbeidstaker() {
        metric.tellEndepunktKall("call_endpoint_ferdigstill_motebehov_arbeidstaker")
        val arbeidstakerFnr = TokenXUtil.validateTokenXClaims(
            contextHolder,
            dialogmoteClientId,
            esyfoProxyClientId
        )
            .fnrFromIdportenTokenX()

        motebehovOppfolgingstilfelleServiceV2.ferdigstillMotebehov(arbeidstakerFnr)
    }
}
