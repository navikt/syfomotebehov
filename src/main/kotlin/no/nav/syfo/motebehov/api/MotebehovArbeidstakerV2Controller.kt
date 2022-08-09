package no.nav.syfo.motebehov.api

import javax.inject.Inject
import javax.validation.Valid
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.syfo.api.auth.OIDCIssuer
import no.nav.syfo.api.auth.OIDCUtil
import no.nav.syfo.consumer.brukertilgang.BrukertilgangService
import no.nav.syfo.metric.Metric
import no.nav.syfo.motebehov.MotebehovOppfolgingstilfelleService
import no.nav.syfo.motebehov.MotebehovSvar
import no.nav.syfo.motebehov.motebehovstatus.MotebehovStatus
import no.nav.syfo.motebehov.motebehovstatus.MotebehovStatusService
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@ProtectedWithClaims(issuer = OIDCIssuer.EKSTERN, claimMap = ["acr=Level4"])
@RequestMapping(value = ["/api/v2/arbeidstaker"])
class MotebehovArbeidstakerV2Controller @Inject constructor(
    private val contextHolder: TokenValidationContextHolder,
    private val metric: Metric,
    private val motebehovStatusService: MotebehovStatusService,
    private val motebehovOppfolgingstilfelleService: MotebehovOppfolgingstilfelleService,
    private val brukertilgangService: BrukertilgangService
) {
    @GetMapping(
        value = ["/motebehov"],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun motebehovStatusArbeidstaker(): MotebehovStatus {
        val fnr = OIDCUtil.fnrFraOIDCEkstern(contextHolder)
        brukertilgangService.kastExceptionHvisIkkeTilgang(fnr.value)

        metric.tellEndepunktKall("call_endpoint_motebehovstatus_arbeidstaker")
        return motebehovStatusService.motebehovStatusForArbeidstaker(fnr)
    }

    @PostMapping(
        value = ["/motebehov"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun submitMotebehovArbeidstaker(
        @RequestBody nyttMotebehovSvar: @Valid MotebehovSvar
    ) {
        metric.tellEndepunktKall("call_endpoint_save_motebehov_arbeidstaker")

        val arbeidstakerFnr = OIDCUtil.fnrFraOIDCEkstern(contextHolder)
        brukertilgangService.kastExceptionHvisIkkeTilgang(arbeidstakerFnr.value)

        motebehovOppfolgingstilfelleService.createMotehovForArbeidstaker(
            OIDCUtil.fnrFraOIDCEkstern(contextHolder),
            nyttMotebehovSvar
        )
    }
}
