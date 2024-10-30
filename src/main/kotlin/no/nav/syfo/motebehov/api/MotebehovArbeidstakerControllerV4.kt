package no.nav.syfo.motebehov.api

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.syfo.api.auth.tokenX.TokenXUtil
import no.nav.syfo.api.auth.tokenX.TokenXUtil.TokenXIssuer
import no.nav.syfo.api.auth.tokenX.TokenXUtil.fnrFromIdportenTokenX
import no.nav.syfo.metric.Metric
import no.nav.syfo.motebehov.*
import no.nav.syfo.motebehov.motebehovstatus.MotebehovStatus
import no.nav.syfo.motebehov.motebehovstatus.MotebehovStatusServiceV2
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
@RequestMapping(value = ["/api/v4/arbeidstaker"])
class MotebehovArbeidstakerControllerV4 @Inject constructor(
    private val contextHolder: TokenValidationContextHolder,
    private val metric: Metric,
    private val motebehovStatusServiceV2: MotebehovStatusServiceV2,
    private val motebehovOppfolgingstilfelleServiceV2: MotebehovOppfolgingstilfelleServiceV2,
    @Value("\${dialogmote.frontend.client.id}")
    val dialogmoteClientId: String,
    @Value("\${esyfo-proxy.client.id}")
    val esyfoProxyClientId: String,
) {
    @PostMapping(
        value = ["/motebehov"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun submitMotebehovArbeidstaker(
        @RequestBody nyttMotebehovSvar: @Valid MotebehovDynamicFormSubmissionDTO,
    ) {
        metric.tellEndepunktKall("call_endpoint_save_motebehov_arbeidstaker")
        val arbeidstakerFnr = TokenXUtil.validateTokenXClaims(
            contextHolder,
            dialogmoteClientId,
            esyfoProxyClientId
        )
            .fnrFromIdportenTokenX()

        val motebehovSvar = MotebehovSvar(
            harMotebehov = nyttMotebehovSvar.harMotebehov,
            forklaring = null,
            dynamicFormSubmission = nyttMotebehovSvar.dynamicFormSubmission,
        )

        motebehovOppfolgingstilfelleServiceV2.createMotebehovForArbeidstaker(
            arbeidstakerFnr,
            motebehovSvar,
        )
    }

    @GetMapping(
        value = ["/motebehov/all"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun motebehovStatusArbeidstakerWithCodeSixUsers(): MotebehovStatus {
        val arbeidstakerFnr = TokenXUtil.validateTokenXClaims(
            contextHolder,
            dialogmoteClientId,
            esyfoProxyClientId
        )
            .fnrFromIdportenTokenX()

        metric.tellEndepunktKall("call_endpoint_motebehovstatus_arbeidstaker_all")

        return motebehovStatusServiceV2.motebehovStatusForArbeidstaker(arbeidstakerFnr)
    }
}
