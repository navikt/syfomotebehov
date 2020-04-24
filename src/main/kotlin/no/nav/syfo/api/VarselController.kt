package no.nav.syfo.api

import no.nav.security.oidc.api.ProtectedWithClaims
import no.nav.syfo.domain.rest.MotebehovsvarVarselInfo
import no.nav.syfo.oidc.OIDCIssuer.INTERN
import no.nav.syfo.service.VarselService
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import javax.inject.Inject
import javax.ws.rs.core.Response

@RestController
@RequestMapping(value = ["/api/varsel/naermesteleder"])
class VarselController @Inject constructor(
        private val varselService: VarselService
) {
    @ResponseBody
    @ProtectedWithClaims(issuer = INTERN, claimMap = ["sub=srvsyfoservice"])
    @PostMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
    fun sendVarselNaermesteLeder(
            @RequestBody motebehovsvarVarselInfo: MotebehovsvarVarselInfo
    ): Response {
        varselService.sendVarselTilNaermesteLeder(motebehovsvarVarselInfo)
        return Response
                .ok()
                .build()
    }
}
