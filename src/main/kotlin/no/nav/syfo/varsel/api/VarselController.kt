package no.nav.syfo.varsel.api

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.syfo.api.auth.OIDCIssuer.INTERN
import no.nav.syfo.api.auth.OIDCIssuer.STS
import no.nav.syfo.consumer.aktorregister.AktorregisterConsumer
import no.nav.syfo.consumer.aktorregister.domain.AktorId
import no.nav.syfo.consumer.aktorregister.domain.Fodselsnummer
import no.nav.syfo.metric.Metric
import no.nav.syfo.varsel.*
import org.springframework.beans.factory.annotation.Value
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
    private val varselService: VarselService,
    private val varselServiceV2: VarselServiceV2,
    @Value("\${use.kandidatlista}")
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

    @ResponseBody
    @ProtectedWithClaims(issuer = INTERN, claimMap = ["sub=srvsyfoservice"])
    @PostMapping(value = ["/availability/arbeidstaker"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun motebehovVarselAvailabilityArbeidstaker(
        @RequestBody motebehovsvarVarselInfo: MotebehovsvarVarselInfoArbeidstaker
    ): ResponseEntity<Boolean> {
        val arbeidstakerFnr = aktorregisterConsumer.getFnrForAktorId(AktorId(motebehovsvarVarselInfo.sykmeldtAktorId))

        val isSvarBehovVarselAvailableForArbeidstaker = if (useKandidatlista) {
            varselServiceV2.isSvarBehovVarselAvailableArbeidstaker(Fodselsnummer(arbeidstakerFnr))
        } else {
            varselService.isSvarBehovVarselAvailableArbeidstaker(Fodselsnummer(arbeidstakerFnr))
        }

        countMotebehovVarselAvailabilityArbeidstaker(isSvarBehovVarselAvailableForArbeidstaker)
        return ResponseEntity
            .ok()
            .body(isSvarBehovVarselAvailableForArbeidstaker)
    }

    private fun countMotebehovVarselAvailabilityArbeidstaker(countIsVarselAvailableForMotebehov: Boolean) {
        if (countIsVarselAvailableForMotebehov) {
            metric.tellHendelse("varsel_arbeidstaker_available_true")
        } else {
            metric.tellHendelse("varsel_arbeidstaker_available_false")
        }
    }
}
