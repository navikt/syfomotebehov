package no.nav.syfo.motebehov.api

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.syfo.api.auth.tokenX.TokenXUtil
import no.nav.syfo.api.auth.tokenX.TokenXUtil.TokenXIssuer
import no.nav.syfo.api.auth.tokenX.TokenXUtil.fnrFromIdportenTokenX
import no.nav.syfo.metric.Metric
import no.nav.syfo.motebehov.MotebehovFormSubmissionDTO
import no.nav.syfo.motebehov.MotebehovOppfolgingstilfelleServiceV2
import no.nav.syfo.motebehov.motebehovstatus.MotebehovStatusServiceV2
import no.nav.syfo.motebehov.motebehovstatus.MotebehovStatusWithFormValuesDTO
import no.nav.syfo.motebehov.motebehovstatus.toMotebehovStatusWithFormValuesDTO
import no.nav.syfo.motebehov.toMotebehovFormSubmissionCombinedDTO
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
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
    @Value("\${ditt.sykefravaer.frontend.client.id}")
    val dittSykefravaerClientId: String,
    @Value("\${esyfo-proxy.client.id}")
    val esyfoProxyClientId: String,
) {
    @GetMapping(
        value = ["/motebehov"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun motebehovStatusArbeidstakerWithCodeSixUsers(): MotebehovStatusWithFormValuesDTO {
        val arbeidstakerFnr = TokenXUtil.validateTokenXClaims(
            contextHolder,
            dialogmoteClientId,
            esyfoProxyClientId
        )
            .fnrFromIdportenTokenX()

        metric.tellEndepunktKall("call_endpoint_motebehovstatus_arbeidstaker_all")

        return motebehovStatusServiceV2.motebehovStatusForArbeidstaker(
            arbeidstakerFnr
        ).toMotebehovStatusWithFormValuesDTO()
    }

    @PostMapping(
        value = ["/motebehov"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun submitMotebehovArbeidstaker(
        @RequestBody nyttMotebehovFormSubmission: @Valid MotebehovFormSubmissionDTO,
    ) {
        metric.tellEndepunktKall("call_endpoint_save_motebehov_arbeidstaker")
        val arbeidstakerFnr = TokenXUtil.validateTokenXClaims(
            contextHolder,
            dialogmoteClientId,
            esyfoProxyClientId
        )
            .fnrFromIdportenTokenX()

        motebehovOppfolgingstilfelleServiceV2.createMotebehovForArbeidstaker(
            arbeidstakerFnr,
            nyttMotebehovFormSubmission.toMotebehovFormSubmissionCombinedDTO(),
        )
    }
}
