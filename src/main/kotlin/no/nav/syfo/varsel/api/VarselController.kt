package no.nav.syfo.varsel.api

import no.nav.security.oidc.api.ProtectedWithClaims
import no.nav.syfo.api.auth.OIDCIssuer.INTERN
import no.nav.syfo.consumer.aktorregister.AktorregisterConsumer
import no.nav.syfo.consumer.aktorregister.domain.AktorId
import no.nav.syfo.consumer.aktorregister.domain.Fodselsnummer
import no.nav.syfo.metric.Metric
import no.nav.syfo.motebehov.motebehovstatus.MotebehovStatusService
import no.nav.syfo.motebehov.motebehovstatus.isSvarBehovVarselAvailable
import no.nav.syfo.varsel.MotebehovsvarVarselInfo
import no.nav.syfo.varsel.MotebehovsvarVarselInfoArbeidstaker
import no.nav.syfo.varsel.VarselService
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import javax.inject.Inject
import javax.ws.rs.core.Response

@RestController
@RequestMapping(value = ["/api/varsel"])
class VarselController @Inject constructor(
        private val metric: Metric,
        private val aktorregisterConsumer: AktorregisterConsumer,
        private val motebehovStatusService: MotebehovStatusService,
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

    @ResponseBody
    @ProtectedWithClaims(issuer = INTERN, claimMap = ["sub=srvsyfoservice"])
    @PostMapping(value = ["/availability/arbeidstaker"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun motebehovVarselAvailabilityArbeidstaker(
            @RequestBody motebehovsvarVarselInfo: MotebehovsvarVarselInfoArbeidstaker
    ): ResponseEntity<Boolean> {
        val arbeidstakerFnr = aktorregisterConsumer.getFnrForAktorId(AktorId(motebehovsvarVarselInfo.sykmeldtAktorId))
        val isSvarBehovVarselAvailableForArbeidstaker = motebehovStatusService.motebehovStatusForArbeidstaker(Fodselsnummer(arbeidstakerFnr)).isSvarBehovVarselAvailable()
        countMotebehovVarselAvailabilityArbeidstaker(isSvarBehovVarselAvailableForArbeidstaker)
        return ResponseEntity
                .ok()
                .body(isSvarBehovVarselAvailableForArbeidstaker)
    }

    private fun countMotebehovVarselAvailabilityArbeidstaker (countIsVarselAvailableForMotebehov: Boolean) {
        if (countIsVarselAvailableForMotebehov) {
            metric.tellHendelse("varsel_arbeidstaker_available_true")
        } else {
            metric.tellHendelse("varsel_arbeidstaker_available_false")
        }
    }
}
