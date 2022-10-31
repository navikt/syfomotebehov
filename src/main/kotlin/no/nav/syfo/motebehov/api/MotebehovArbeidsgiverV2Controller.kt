package no.nav.syfo.motebehov.api

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.syfo.api.auth.OIDCIssuer
import no.nav.syfo.api.auth.OIDCUtil
import no.nav.syfo.consumer.brukertilgang.BrukertilgangService
import no.nav.syfo.metric.Metric
import no.nav.syfo.motebehov.MotebehovOppfolgingstilfelleServiceV2
import no.nav.syfo.motebehov.NyttMotebehovArbeidsgiver
import no.nav.syfo.motebehov.motebehovstatus.MotebehovStatus
import no.nav.syfo.motebehov.motebehovstatus.MotebehovStatusServiceV2
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import javax.inject.Inject
import javax.validation.Valid
import javax.validation.constraints.Pattern

@RestController
@ProtectedWithClaims(issuer = OIDCIssuer.EKSTERN, claimMap = ["acr=Level4"])
@RequestMapping(value = ["/api/v2"])
class MotebehovArbeidsgiverV2Controller @Inject constructor(
    private val contextHolder: TokenValidationContextHolder,
    private val metric: Metric,
    private val motebehovOppfolgingstilfelleServiceV2: MotebehovOppfolgingstilfelleServiceV2,
    private val motebehovStatusServiceV2: MotebehovStatusServiceV2,
    private val brukertilgangService: BrukertilgangService
) {
    @GetMapping(
        value = ["/motebehov"],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun motebehovStatusArbeidsgiver(
        @RequestParam(name = "fnr") arbeidstakerFnr: @Pattern(regexp = "^[0-9]{11}$") String,
        @RequestParam(name = "virksomhetsnummer") virksomhetsnummer: String
    ): MotebehovStatus {
        metric.tellEndepunktKall("call_endpoint_motebehovstatus_arbeidsgiver")
        val fnr = arbeidstakerFnr
        brukertilgangService.kastExceptionHvisIkkeTilgang(fnr)

        val arbeidsgiverFnr = OIDCUtil.fnrFraOIDCEkstern(contextHolder)
        val isOwnLeader = arbeidsgiverFnr == fnr

        return motebehovStatusServiceV2.motebehovStatusForArbeidsgiver(fnr, isOwnLeader, virksomhetsnummer)
    }

    @PostMapping(
        value = ["/motebehov"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun lagreMotebehovArbeidsgiver(
        @RequestBody nyttMotebehov: @Valid NyttMotebehovArbeidsgiver
    ) {
        metric.tellEndepunktKall("call_endpoint_save_motebehov_arbeidsgiver")
        val arbeidstakerFnr = nyttMotebehov.arbeidstakerFnr
        brukertilgangService.kastExceptionHvisIkkeTilgang(arbeidstakerFnr)

        val arbeidsgiverFnr = OIDCUtil.fnrFraOIDCEkstern(contextHolder)
        val isOwnLeader = arbeidsgiverFnr == arbeidstakerFnr

        motebehovOppfolgingstilfelleServiceV2.createMotebehovForArbeidgiver(
            OIDCUtil.fnrFraOIDCEkstern(contextHolder),
            arbeidstakerFnr,
            isOwnLeader,
            nyttMotebehov
        )
    }
}
