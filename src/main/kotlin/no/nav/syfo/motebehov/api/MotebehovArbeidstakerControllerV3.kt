package no.nav.syfo.motebehov.api

import javax.inject.Inject
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.syfo.api.auth.tokenX.TokenXUtil
import no.nav.syfo.api.auth.tokenX.TokenXUtil.TokenXIssuer
import no.nav.syfo.api.auth.tokenX.TokenXUtil.fnrFromIdportenTokenX
import no.nav.syfo.metric.Metric
import no.nav.syfo.motebehov.motebehovstatus.MotebehovSkjemaType
import no.nav.syfo.motebehov.motebehovstatus.MotebehovStatus
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

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
    @Value("\${ditt.sykefravaer.frontend.client.id}")
    val dittSykefravaerClientId: String,
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

        // This endpoint is only used by Ditt sykefravær.
        // Should be removed when they stop calling it.
        // Until then, return empty result
        metric.tellEndepunktKall("call_endpoint_motebehovstatus_arbeidstaker_v3")
        return MotebehovStatus(
            false,
            MotebehovSkjemaType.MELD_BEHOV,
            null,
        )
    }

    @GetMapping("/motebehov/all")
    fun motebehovStatusArbeidstakerWithCodeSixUsers() = ResponseEntity
        .status(HttpStatus.MOVED_PERMANENTLY).build<Void>().also {
        metric.tellEndepunktKall("call_endpoint_motebehovstatus_arbeidstaker_all_v3")
    }

    @PostMapping("/motebehov")
    fun submitMotebehovArbeidstaker(
    ) = ResponseEntity
        .status(HttpStatus.MOVED_PERMANENTLY).build<Void>().also {
        metric.tellEndepunktKall("call_endpoint_save_motebehov_arbeidstaker_v3")
    }



    @PostMapping("/motebehov/ferdigstill")
    fun ferdigstillMotebehovArbeidstaker(): ResponseEntity<Void> = ResponseEntity
        .status(HttpStatus.MOVED_PERMANENTLY).build<Void>().also {
        metric.tellEndepunktKall("call_endpoint_ferdigstill_motebehov_arbeidstaker_v3")
    }
}
