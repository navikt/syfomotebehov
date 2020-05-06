package no.nav.syfo.motebehov.api

import no.nav.security.oidc.api.ProtectedWithClaims
import no.nav.security.oidc.context.OIDCRequestContextHolder
import no.nav.syfo.api.auth.OIDCIssuer
import no.nav.syfo.api.auth.OIDCUtil
import no.nav.syfo.consumer.aktorregister.domain.Fodselsnummer
import no.nav.syfo.consumer.brukertilgang.BrukertilgangService
import no.nav.syfo.metric.Metric
import no.nav.syfo.motebehov.MotebehovOpfolgingstilfelleService
import no.nav.syfo.motebehov.MotebehovService
import no.nav.syfo.motebehov.MotebehovSvar
import no.nav.syfo.motebehov.NyttMotebehovArbeidsgiver
import no.nav.syfo.motebehov.motebehovstatus.MotebehovStatus
import no.nav.syfo.motebehov.motebehovstatus.MotebehovStatusService
import org.apache.commons.lang3.StringUtils
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import javax.inject.Inject
import javax.validation.Valid
import javax.validation.constraints.Pattern
import javax.ws.rs.ForbiddenException

@RestController
@ProtectedWithClaims(issuer = OIDCIssuer.EKSTERN, claimMap = ["acr=Level4"])
@RequestMapping(value = ["/api/v2"])
class MotebehovBrukerV2Controller @Inject constructor(
        private val contextHolder: OIDCRequestContextHolder,
        private val metric: Metric,
        private val motebehovService: MotebehovService,
        private val motebehovStatusService: MotebehovStatusService,
        private val motebehovOpfolgingstilfelleService: MotebehovOpfolgingstilfelleService,
        private val brukertilgangService: BrukertilgangService
) {
    @GetMapping(value = ["/motebehov"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun motebehovStatusArbeidsgiver(
            @RequestParam(name = "fnr") arbeidstakerFnr: @Pattern(regexp = "^[0-9]{11}$") String,
            @RequestParam(name = "virksomhetsnummer") virksomhetsnummer: String
    ): MotebehovStatus {
        metric.tellEndepunktKall("call_endpoint_motebehovstatus_arbeidsgiver")
        val fnr = Fodselsnummer(arbeidstakerFnr)
        kastExceptionHvisIkkeTilgang(fnr.value)

        return motebehovStatusService.motebehovStatusForArbeidsgiver(fnr, virksomhetsnummer)
    }

    @PostMapping(value = ["/motebehov"], consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun lagreMotebehovArbeidsgiver(
            @RequestBody nyttMotebehov: @Valid NyttMotebehovArbeidsgiver
    ) {
        metric.tellEndepunktKall("call_endpoint_save_motebehov_arbeidsgiver")
        val arbeidstakerFnr = Fodselsnummer(nyttMotebehov.arbeidstakerFnr)
        kastExceptionHvisIkkeTilgang(arbeidstakerFnr.value)

        motebehovService.lagreMotebehov(
                OIDCUtil.fnrFraOIDCEkstern(contextHolder),
                arbeidstakerFnr,
                nyttMotebehov.virksomhetsnummer,
                nyttMotebehov.motebehovSvar
        )
        lagBesvarMotebehovMetrikk(nyttMotebehov.motebehovSvar, nyttMotebehov.arbeidstakerFnr.isNullOrEmpty())
    }

    @GetMapping(value = ["/arbeidstaker/motebehov"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun motebehovStatusArbeidstaker(): MotebehovStatus {
        val fnr = OIDCUtil.fnrFraOIDCEkstern(contextHolder)
        kastExceptionHvisIkkeTilgang(fnr.value)

        metric.tellEndepunktKall("call_endpoint_motebehovstatus_arbeidstaker")
        return motebehovStatusService.motebehovStatusForArbeidstaker(fnr)
    }

    @PostMapping(value = ["/arbeidstaker/motebehov"], consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun submitMotebehovArbeidstaker(
            @RequestBody nyttMotebehovSvar: @Valid MotebehovSvar
    ) {
        metric.tellEndepunktKall("call_endpoint_save_motebehov_arbeidstaker")

        val arbeidstakerFnr = OIDCUtil.fnrFraOIDCEkstern(contextHolder)
        kastExceptionHvisIkkeTilgang(arbeidstakerFnr.value)

        motebehovOpfolgingstilfelleService.createMotehovForArbeidstaker(
                OIDCUtil.fnrFraOIDCEkstern(contextHolder),
                nyttMotebehovSvar
        )
        lagBesvarMotebehovMetrikk(nyttMotebehovSvar, true)
    }

    private fun kastExceptionHvisIkkeTilgang(fnr: String) {
        val innloggetIdent = OIDCUtil.fnrFraOIDCEkstern(contextHolder).value
        val harTilgang = brukertilgangService.harTilgangTilOppslaattBruker(innloggetIdent, fnr)
        if (!harTilgang) {
            throw ForbiddenException("Ikke tilgang")
        }
    }

    private fun lagBesvarMotebehovMetrikk(motebehovSvar: MotebehovSvar, erInnloggetBrukerArbeidstaker: Boolean) {
        metric.tellMotebehovBesvart(motebehovSvar.harMotebehov, erInnloggetBrukerArbeidstaker)
        if (!motebehovSvar.harMotebehov) {
            metric.tellMotebehovBesvartNeiAntallTegn(motebehovSvar.forklaring!!.length, erInnloggetBrukerArbeidstaker)
        } else if (!StringUtils.isEmpty(motebehovSvar.forklaring)) {
            metric.tellMotebehovBesvartJaMedForklaringTegn(motebehovSvar.forklaring!!.length, erInnloggetBrukerArbeidstaker)
            metric.tellMotebehovBesvartJaMedForklaringAntall(erInnloggetBrukerArbeidstaker)
        }
    }
}
