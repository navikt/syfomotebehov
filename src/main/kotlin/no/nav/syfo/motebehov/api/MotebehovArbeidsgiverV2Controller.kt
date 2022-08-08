package no.nav.syfo.motebehov.api

import javax.inject.Inject
import javax.validation.Valid
import javax.validation.constraints.Pattern
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.syfo.api.auth.OIDCIssuer
import no.nav.syfo.api.auth.OIDCUtil
import no.nav.syfo.consumer.aktorregister.domain.Fodselsnummer
import no.nav.syfo.consumer.brukertilgang.BrukertilgangService
import no.nav.syfo.metric.Metric
import no.nav.syfo.motebehov.MotebehovOpfolgingstilfelleService
import no.nav.syfo.motebehov.NyttMotebehovArbeidsgiver
import no.nav.syfo.motebehov.motebehovstatus.MotebehovStatus
import no.nav.syfo.motebehov.motebehovstatus.MotebehovStatusService
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@ProtectedWithClaims(issuer = OIDCIssuer.EKSTERN, claimMap = ["acr=Level4"])
@RequestMapping(value = ["/api/v2"])
class MotebehovArbeidsgiverV2Controller @Inject constructor(
    private val contextHolder: TokenValidationContextHolder,
    private val metric: Metric,
    private val motebehovOpfolgingstilfelleService: MotebehovOpfolgingstilfelleService,
    private val motebehovStatusService: MotebehovStatusService,
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
        val fnr = Fodselsnummer(arbeidstakerFnr)
        brukertilgangService.kastExceptionHvisIkkeTilgang(fnr.value)

        val arbeidsgiverFnr = OIDCUtil.fnrFraOIDCEkstern(contextHolder)
        val isOwnLeader = arbeidsgiverFnr.value == fnr.value

        return motebehovStatusService.motebehovStatusForArbeidsgiver(fnr, isOwnLeader, virksomhetsnummer)
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
        val arbeidstakerFnr = Fodselsnummer(nyttMotebehov.arbeidstakerFnr)
        brukertilgangService.kastExceptionHvisIkkeTilgang(arbeidstakerFnr.value)

        val arbeidsgiverFnr = OIDCUtil.fnrFraOIDCEkstern(contextHolder)
        val isOwnLeader = arbeidsgiverFnr.value == arbeidstakerFnr.value

        motebehovOpfolgingstilfelleService.createMotehovForArbeidgiver(
            OIDCUtil.fnrFraOIDCEkstern(contextHolder),
            arbeidstakerFnr,
            isOwnLeader,
            nyttMotebehov
        )
    }
}
