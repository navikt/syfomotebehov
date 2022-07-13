package no.nav.syfo.motebehov.api

import javax.inject.Inject
import javax.validation.Valid
import javax.validation.constraints.Pattern
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.syfo.api.auth.tokenX.TokenXUtil
import no.nav.syfo.api.auth.tokenX.TokenXUtil.fnrFromIdportenTokenX
import no.nav.syfo.consumer.aktorregister.domain.Fodselsnummer
import no.nav.syfo.consumer.brukertilgang.BrukertilgangService
import no.nav.syfo.metric.Metric
import no.nav.syfo.motebehov.MotebehovOpfolgingstilfelleService
import no.nav.syfo.motebehov.NyttMotebehovArbeidsgiver
import no.nav.syfo.motebehov.motebehovstatus.MotebehovStatus
import no.nav.syfo.motebehov.motebehovstatus.MotebehovStatusService
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@ProtectedWithClaims(issuer = TokenXUtil.TokenXIssuer.TOKENX, claimMap = ["acr=Level4"])
@RequestMapping(value = ["/api/v3"])
class MotebehovArbeidsgiverV3Controller @Inject constructor(
    private val contextHolder: TokenValidationContextHolder,
    private val metric: Metric,
    private val motebehovOpfolgingstilfelleService: MotebehovOpfolgingstilfelleService,
    private val motebehovStatusService: MotebehovStatusService,
    private val brukertilgangService: BrukertilgangService,
    @Value("\${dialogmote.frontend.client.id}")
    val dialogmoteClientId: String,
    @Value("\${tokenx.idp}")
    val dialogmoteTokenxIdp: String
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
        TokenXUtil.validateTokenXClaims(contextHolder, dialogmoteClientId, dialogmoteTokenxIdp)
        val ansattFnr = Fodselsnummer(arbeidstakerFnr)
        brukertilgangService.kastExceptionHvisIkkeTilgangTilAnsattTokenX(ansattFnr.value)

        return motebehovStatusService.motebehovStatusForArbeidsgiver(ansattFnr, virksomhetsnummer)
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
        val innloggetFnr = TokenXUtil.validateTokenXClaims(contextHolder, dialogmoteClientId, dialogmoteTokenxIdp)
            .fnrFromIdportenTokenX()
        val ansattFnr = Fodselsnummer(nyttMotebehov.arbeidstakerFnr)
        brukertilgangService.kastExceptionHvisIkkeTilgangTilAnsattTokenX(ansattFnr.value)

        motebehovOpfolgingstilfelleService.createMotehovForArbeidgiver(
            innloggetFnr,
            ansattFnr,
            nyttMotebehov
        )
    }
}
