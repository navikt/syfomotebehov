package no.nav.syfo.varsel.api

import no.nav.security.oidc.api.ProtectedWithClaims
import no.nav.syfo.varsel.MotebehovsvarVarselInfo
import no.nav.syfo.api.auth.OIDCIssuer.INTERN
import no.nav.syfo.varsel.VarselService
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import javax.inject.Inject
import javax.ws.rs.core.Response

@RestController
@RequestMapping(value = ["/api/varsel"])
class VarselController @Inject constructor(
        private val varselService: VarselService
) {
    @ResponseBody
    @ProtectedWithClaims(issuer = INTERN, claimMap = ["sub=srvsyfoservice"])
    @PostMapping(value = ["/naermesteleder"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun sendVarselNaermesteLeder(
            @RequestBody motebehovsvarVarselInfo: MotebehovsvarVarselInfo
    ): Response {
        varselService.sendVarselTilNaermesteLeder(motebehovsvarVarselInfo)
        return Response
                .ok()
                .build()
    }
}
