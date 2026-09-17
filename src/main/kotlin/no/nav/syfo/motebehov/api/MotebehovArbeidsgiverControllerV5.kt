package no.nav.syfo.motebehov.api

import jakarta.validation.Valid
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.syfo.api.auth.tokenX.TokenXUtil
import no.nav.syfo.api.auth.tokenX.TokenXUtil.TokenXIssuer
import no.nav.syfo.api.auth.tokenX.TokenXUtil.fnrFromIdportenTokenX
import no.nav.syfo.consumer.brukertilgang.DineSykmeldteResponse
import no.nav.syfo.consumer.brukertilgang.DineSykmeldteTilgangService
import no.nav.syfo.metric.Metric
import no.nav.syfo.motebehov.MotebehovOppfolgingstilfelleServiceV2
import no.nav.syfo.motebehov.NyttMotebehovArbeidsgiverDTO
import no.nav.syfo.motebehov.NyttMotebehovArbeidsgiverV5DTO
import no.nav.syfo.motebehov.motebehovstatus.MotebehovStatusServiceV2
import no.nav.syfo.motebehov.motebehovstatus.MotebehovStatusWithFormValuesDTO
import no.nav.syfo.motebehov.motebehovstatus.toMotebehovStatusWithFormValuesDTO
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID
import javax.inject.Inject

@RestController
@ProtectedWithClaims(
    issuer = TokenXIssuer.TOKENX,
    claimMap = ["acr=Level4", "acr=idporten-loa-high"],
    combineWithOr = true,
)
@RequestMapping(value = ["/api/v5/arbeidsgiver"])
class MotebehovArbeidsgiverControllerV5
    @Inject
    constructor(
        private val contextHolder: TokenValidationContextHolder,
        private val metric: Metric,
        private val motebehovOppfolgingstilfelleServiceV2: MotebehovOppfolgingstilfelleServiceV2,
        private val motebehovStatusServiceV2: MotebehovStatusServiceV2,
        private val dineSykmeldteTilgangService: DineSykmeldteTilgangService,
        @Value("\${dialogmote.frontend.client.id}")
        val dialogmoteClientId: String,
    ) {
        @GetMapping(
            value = ["/motebehov/{narmesteLederId}"],
            produces = [MediaType.APPLICATION_JSON_VALUE],
        )
        fun motebehovStatusArbeidsgiver(
            @PathVariable narmesteLederId: UUID,
        ): MotebehovStatusWithFormValuesDTO {
            metric.tellEndepunktKall("call_endpoint_motebehovstatus_arbeidsgiver_v5")
            val (innloggetFnr, sykmeldt) = hentSykmeldtMedTilgang(narmesteLederId)

            return motebehovStatusServiceV2
                .motebehovStatusForArbeidsgiver(
                    sykmeldt.fnr,
                    innloggetFnr == sykmeldt.fnr,
                    sykmeldt.orgnummer,
                ).toMotebehovStatusWithFormValuesDTO()
        }

        @PostMapping(
            value = ["/motebehov"],
            consumes = [MediaType.APPLICATION_JSON_VALUE],
            produces = [MediaType.APPLICATION_JSON_VALUE],
        )
        fun lagreMotebehovArbeidsgiver(
            @RequestBody nyttMotebehovDTO: @Valid NyttMotebehovArbeidsgiverV5DTO,
        ) {
            metric.tellEndepunktKall("call_endpoint_save_motebehov_arbeidsgiver_v5")
            val (innloggetFnr, sykmeldt) = hentSykmeldtMedTilgang(nyttMotebehovDTO.narmesteLederId)
            val lagringsgrunnlag =
                NyttMotebehovArbeidsgiverDTO(
                    arbeidstakerFnr = sykmeldt.fnr,
                    virksomhetsnummer = sykmeldt.orgnummer,
                    formSubmission = nyttMotebehovDTO.formSubmission,
                )

            motebehovOppfolgingstilfelleServiceV2.createMotebehovForArbeidgiver(
                innloggetFnr,
                sykmeldt.fnr,
                innloggetFnr == sykmeldt.fnr,
                lagringsgrunnlag,
            )
        }

        private fun hentSykmeldtMedTilgang(narmesteLederId: UUID): Pair<String, DineSykmeldteResponse> {
            val innloggetFnr =
                TokenXUtil
                    .validateTokenXClaims(contextHolder, dialogmoteClientId)
                    .fnrFromIdportenTokenX()
            val sykmeldt = dineSykmeldteTilgangService.hentSykmeldtMedTilgang(narmesteLederId)
            return innloggetFnr to sykmeldt
        }
    }
