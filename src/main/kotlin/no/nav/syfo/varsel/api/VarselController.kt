package no.nav.syfo.varsel.api

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.syfo.api.auth.OIDCIssuer.STS
import no.nav.syfo.varsel.MotebehovsvarSykmeldtVarselInfo
import no.nav.syfo.varsel.MotebehovsvarVarselInfo
import no.nav.syfo.varsel.VarselService
import no.nav.syfo.varsel.VarselServiceV2
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import javax.inject.Inject
import javax.ws.rs.core.Response

@RestController
@RequestMapping(value = ["/api/varsel"])
class VarselController @Inject constructor(
    private val varselService: VarselService,
    private val varselServiceV2: VarselServiceV2,
    @Value("\${toggle.kandidatlista}")
    private val useKandidatlista: Boolean,
) {
    @ResponseBody
    @ProtectedWithClaims(issuer = STS)
    @PostMapping(value = ["/naermesteleder/esyfovarsel"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun sendVarselNaermesteLeder(
        @RequestBody motebehovsvarVarselInfo: MotebehovsvarVarselInfo
    ): Response {
        if (useKandidatlista) {
            varselServiceV2.sendVarselTilNaermesteLeder(motebehovsvarVarselInfo)
        } else {
            varselService.sendVarselTilNaermesteLeder(motebehovsvarVarselInfo)
        }
        return Response
            .ok()
            .build()
    }

    @ResponseBody
    @ProtectedWithClaims(issuer = STS)
    @PostMapping(value = ["/arbeidstaker/esyfovarsel"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun sendVarselArbeidstaker(
        @RequestBody motebehovsvarVarselInfo: MotebehovsvarSykmeldtVarselInfo
    ): Response {
        if (useKandidatlista) {
            varselServiceV2.sendVarselTilArbeidstaker(motebehovsvarVarselInfo)
        } else {
            varselService.sendVarselTilArbeidstaker(motebehovsvarVarselInfo)
        }
        return Response
            .ok()
            .build()
    }
}
