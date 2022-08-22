package no.nav.syfo.motebehov.api

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.syfo.api.auth.tokenX.TokenXUtil
import no.nav.syfo.api.auth.tokenX.TokenXUtil.fnrFromIdportenTokenX
import no.nav.syfo.consumer.aktorregister.domain.Fodselsnummer
import no.nav.syfo.consumer.brukertilgang.BrukertilgangService
import no.nav.syfo.metric.Metric
import no.nav.syfo.motebehov.MotebehovOppfolgingstilfelleService
import no.nav.syfo.motebehov.MotebehovOppfolgingstilfelleServiceV2
import no.nav.syfo.motebehov.NyttMotebehovArbeidsgiver
import no.nav.syfo.motebehov.motebehovstatus.MotebehovStatus
import no.nav.syfo.motebehov.motebehovstatus.MotebehovStatusService
import no.nav.syfo.motebehov.motebehovstatus.MotebehovStatusServiceV2
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import javax.inject.Inject
import javax.validation.Valid
import javax.validation.constraints.Pattern

@RestController
@ProtectedWithClaims(issuer = TokenXUtil.TokenXIssuer.TOKENX, claimMap = ["acr=Level4"])
@RequestMapping(value = ["/api/v3"])
class MotebehovArbeidsgiverV3Controller @Inject constructor(
    private val contextHolder: TokenValidationContextHolder,
    private val metric: Metric,
    private val motebehovOppfolgingstilfelleService: MotebehovOppfolgingstilfelleService,
    private val motebehovOppfolgingstilfelleServiceV2: MotebehovOppfolgingstilfelleServiceV2,
    private val motebehovStatusService: MotebehovStatusService,
    private val motebehovStatusServiceV2: MotebehovStatusServiceV2,
    private val brukertilgangService: BrukertilgangService,
    @Value("\${dialogmote.frontend.client.id}")
    val dialogmoteClientId: String,
    @Value("\${tokenx.idp}")
    val dialogmoteTokenxIdp: String,
    @Value("\${use.kandidatlista}")
    private val useKandidatlista: Boolean,
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
        TokenXUtil.validateTokenXClaims(contextHolder, dialogmoteTokenxIdp, dialogmoteClientId)
        val ansattFnr = Fodselsnummer(arbeidstakerFnr)
        brukertilgangService.kastExceptionHvisIkkeTilgangTilAnsattTokenX(ansattFnr.value)

        val arbeidsgiverFnr = fnrFromIdportenTokenX(contextHolder)
        val isOwnLeader = arbeidsgiverFnr.value == ansattFnr.value

        if (useKandidatlista) {
            return motebehovStatusServiceV2.motebehovStatusForArbeidsgiver(ansattFnr, isOwnLeader, virksomhetsnummer)
        }

        return motebehovStatusService.motebehovStatusForArbeidsgiver(ansattFnr, isOwnLeader, virksomhetsnummer)
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
        val innloggetFnr = TokenXUtil.validateTokenXClaims(contextHolder, dialogmoteTokenxIdp, dialogmoteClientId)
            .fnrFromIdportenTokenX()
        val ansattFnr = Fodselsnummer(nyttMotebehov.arbeidstakerFnr)
        brukertilgangService.kastExceptionHvisIkkeTilgangTilAnsattTokenX(ansattFnr.value)

        val arbeidsgiverFnr = fnrFromIdportenTokenX(contextHolder)
        val isOwnLeader = arbeidsgiverFnr.value == ansattFnr.value

        if (useKandidatlista) {
            motebehovOppfolgingstilfelleServiceV2.createMotebehovForArbeidgiver(
                innloggetFnr,
                ansattFnr,
                isOwnLeader,
                nyttMotebehov
            )
        } else {
            motebehovOppfolgingstilfelleService.createMotehovForArbeidgiver(
                innloggetFnr,
                ansattFnr,
                isOwnLeader,
                nyttMotebehov
            )
        }
    }
}
