package no.nav.syfo.varsel.api

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.syfo.api.auth.OIDCIssuer
import no.nav.syfo.api.auth.OIDCUtil
import no.nav.syfo.consumer.brukertilgang.BrukertilgangService
import no.nav.syfo.varsel.VarselService
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import javax.inject.Inject

@RestController
@ProtectedWithClaims(issuer = OIDCIssuer.EKSTERN, claimMap = ["acr=Level4"])
@RequestMapping(value = ["/api/esyfovarsel"])
class EsyfovarselController @Inject constructor(
    private val contextHolder: TokenValidationContextHolder,
    private val metric: no.nav.syfo.metric.Metric,
    private val varselService: VarselService,
    private val brukertilgangService: BrukertilgangService
) {
    @GetMapping(
        value = ["/39uker"],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun erVarslet39Uker(): Boolean {
        val fnr = OIDCUtil.fnrFraOIDCEkstern(contextHolder)
        brukertilgangService.kastExceptionHvisIkkeTilgang(fnr.value)

        metric.tellEndepunktKall("call_endpoint_esyfovarsel_39uker")
        return varselService.has39UkerVarselBeenSent(fnr)
    }
}
