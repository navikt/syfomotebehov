package no.nav.syfo.motebehov.api

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.syfo.api.auth.tokenX.TokenXUtil
import no.nav.syfo.api.auth.tokenX.TokenXUtil.TokenXIssuer
import no.nav.syfo.api.auth.tokenX.TokenXUtil.fnrFromIdportenTokenX
import no.nav.syfo.consumer.brukertilgang.BrukertilgangService
import no.nav.syfo.metric.Metric
import no.nav.syfo.motebehov.MotebehovFormSubmissionCombinedDTO
import no.nav.syfo.motebehov.MotebehovOppfolgingstilfelleServiceV2
import no.nav.syfo.motebehov.NyttMotebehovArbeidsgiverDTO
import no.nav.syfo.motebehov.NyttMotebehovArbeidsgiverLegacyDTO
import no.nav.syfo.motebehov.motebehovstatus.MotebehovStatusServiceV2
import no.nav.syfo.motebehov.motebehovstatus.MotebehovStatusWithLegacyMotebehovDTO
import no.nav.syfo.motebehov.motebehovstatus.toMotebehovStatusWithLegacyMotebehovDTO
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import javax.inject.Inject
import javax.validation.Valid
import javax.validation.constraints.Pattern

@RestController
@ProtectedWithClaims(
    issuer = TokenXIssuer.TOKENX,
    claimMap = ["acr=Level4", "acr=idporten-loa-high"],
    combineWithOr = true
)
@RequestMapping(value = ["/api/v3"])
class MotebehovArbeidsgiverControllerV3 @Inject constructor(
    private val contextHolder: TokenValidationContextHolder,
    private val metric: Metric,
    private val motebehovOppfolgingstilfelleServiceV2: MotebehovOppfolgingstilfelleServiceV2,
    private val motebehovStatusServiceV2: MotebehovStatusServiceV2,
    private val brukertilgangService: BrukertilgangService,
    @Value("\${dialogmote.frontend.client.id}")
    val dialogmoteClientId: String,
) {
    @GetMapping(
        value = ["/motebehov"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun motebehovStatusArbeidsgiver(
        @RequestParam(name = "fnr") arbeidstakerFnr: @Pattern(regexp = "^[0-9]{11}$") String,
        @RequestParam(name = "virksomhetsnummer") virksomhetsnummer: String,
    ): MotebehovStatusWithLegacyMotebehovDTO {
        metric.tellEndepunktKall("call_endpoint_motebehovstatus_arbeidsgiver")
        TokenXUtil.validateTokenXClaims(contextHolder, dialogmoteClientId)
        brukertilgangService.kastExceptionHvisIkkeTilgangTilAnsatt(arbeidstakerFnr)

        val arbeidsgiverFnr = fnrFromIdportenTokenX(contextHolder)
        val isOwnLeader = arbeidsgiverFnr == arbeidstakerFnr

        return motebehovStatusServiceV2.motebehovStatusForArbeidsgiver(arbeidstakerFnr, isOwnLeader, virksomhetsnummer)
            .toMotebehovStatusWithLegacyMotebehovDTO()
    }

    // Currently used POST-endpoint to phase out
    @PostMapping(
        value = ["/motebehov"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun lagreMotebehovArbeidsgiver(
        @RequestBody nyttMotebehovDTO: @Valid NyttMotebehovArbeidsgiverLegacyDTO,
    ) {
        metric.tellEndepunktKall("call_endpoint_save_motebehov_arbeidsgiver")
        val innloggetFnr = TokenXUtil.validateTokenXClaims(contextHolder, dialogmoteClientId)
            .fnrFromIdportenTokenX()
        val ansattFnr = nyttMotebehovDTO.arbeidstakerFnr
        brukertilgangService.kastExceptionHvisIkkeTilgangTilAnsatt(ansattFnr)

        val arbeidsgiverFnr = fnrFromIdportenTokenX(contextHolder)
        val isOwnLeader = arbeidsgiverFnr == ansattFnr

        val nyttMotebehovArbeidsgiverDTO = NyttMotebehovArbeidsgiverDTO(
            arbeidstakerFnr = nyttMotebehovDTO.arbeidstakerFnr,
            virksomhetsnummer = nyttMotebehovDTO.virksomhetsnummer,
            formSubmission = MotebehovFormSubmissionCombinedDTO(
                harMotebehov = nyttMotebehovDTO.motebehovSvar.harMotebehov,
                forklaring = nyttMotebehovDTO.motebehovSvar.forklaring,
                formSnapshot = null,
            ),
        )

        motebehovOppfolgingstilfelleServiceV2.createMotebehovForArbeidgiver(
            innloggetFnr,
            ansattFnr,
            isOwnLeader,
            nyttMotebehovArbeidsgiverDTO,
        )
    }
}
