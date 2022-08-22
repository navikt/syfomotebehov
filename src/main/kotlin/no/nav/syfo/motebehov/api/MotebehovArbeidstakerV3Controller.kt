package no.nav.syfo.motebehov.api

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.syfo.api.auth.tokenX.TokenXUtil
import no.nav.syfo.api.auth.tokenX.TokenXUtil.TokenXIssuer
import no.nav.syfo.api.auth.tokenX.TokenXUtil.fnrFromIdportenTokenX
import no.nav.syfo.consumer.brukertilgang.BrukertilgangService
import no.nav.syfo.metric.Metric
import no.nav.syfo.motebehov.MotebehovOppfolgingstilfelleService
import no.nav.syfo.motebehov.MotebehovOppfolgingstilfelleServiceV2
import no.nav.syfo.motebehov.MotebehovSvar
import no.nav.syfo.motebehov.motebehovstatus.MotebehovStatus
import no.nav.syfo.motebehov.motebehovstatus.MotebehovStatusService
import no.nav.syfo.motebehov.motebehovstatus.MotebehovStatusServiceV2
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import javax.inject.Inject
import javax.validation.Valid

@RestController
@ProtectedWithClaims(issuer = TokenXIssuer.TOKENX, claimMap = ["acr=Level4"])
@RequestMapping(value = ["/api/v3/arbeidstaker"])
class MotebehovArbeidstakerV3Controller @Inject constructor(
    private val contextHolder: TokenValidationContextHolder,
    private val metric: Metric,
    private val motebehovStatusService: MotebehovStatusService,
    private val motebehovStatusServiceV2: MotebehovStatusServiceV2,
    private val motebehovOppfolgingstilfelleService: MotebehovOppfolgingstilfelleService,
    private val motebehovOppfolgingstilfelleServiceV2: MotebehovOppfolgingstilfelleServiceV2,
    private val brukertilgangService: BrukertilgangService,
    @Value("\${dialogmote.frontend.client.id}")
    val dialogmoteClientId: String,
    @Value("\${ditt.sykefravaer.frontend.client.id}")
    val dittSykefravaerClientId: String,
    @Value("\${tokenx.idp}")
    val dialogmoteTokenxIdp: String,
    @Value("\${use.kandidatlista}")
    private val useKandidatlista: Boolean,
) {
    @GetMapping(
        value = ["/motebehov"],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun motebehovStatusArbeidstaker(): MotebehovStatus {
        val arbeidstakerFnr = TokenXUtil.validateTokenXClaims(
            contextHolder,
            dialogmoteTokenxIdp,
            dialogmoteClientId,
            dittSykefravaerClientId
        )
            .fnrFromIdportenTokenX()

        brukertilgangService.kastExceptionHvisIkkeTilgangTilSegSelv(arbeidstakerFnr.value)

        metric.tellEndepunktKall("call_endpoint_motebehovstatus_arbeidstaker")

        if (useKandidatlista) {
            return motebehovStatusServiceV2.motebehovStatusForArbeidstaker(arbeidstakerFnr)
        }

        return motebehovStatusService.motebehovStatusForArbeidstaker(arbeidstakerFnr)
    }

    @PostMapping(
        value = ["/motebehov"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun submitMotebehovArbeidstaker(
        @RequestBody nyttMotebehovSvar: @Valid MotebehovSvar
    ) {
        metric.tellEndepunktKall("call_endpoint_save_motebehov_arbeidstaker")
        val arbeidstakerFnr = TokenXUtil.validateTokenXClaims(
            contextHolder,
            dialogmoteTokenxIdp,
            dialogmoteClientId,
        )
            .fnrFromIdportenTokenX()

        brukertilgangService.kastExceptionHvisIkkeTilgangTilSegSelv(arbeidstakerFnr.value)

        if (useKandidatlista) {
            motebehovOppfolgingstilfelleServiceV2.createMotebehovForArbeidstaker(
                arbeidstakerFnr,
                nyttMotebehovSvar
            )
        } else {
            motebehovOppfolgingstilfelleService.createMotehovForArbeidstaker(
                arbeidstakerFnr,
                nyttMotebehovSvar
            )
        }
    }
}
