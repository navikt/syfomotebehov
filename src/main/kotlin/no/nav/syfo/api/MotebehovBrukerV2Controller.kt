package no.nav.syfo.api

import no.nav.security.oidc.api.ProtectedWithClaims
import no.nav.security.oidc.context.OIDCRequestContextHolder
import no.nav.syfo.aktorregister.domain.Fodselsnummer
import no.nav.syfo.brukertilgang.BrukertilgangService
import no.nav.syfo.metric.Metrikk
import no.nav.syfo.motebehov.*
import no.nav.syfo.oidc.OIDCIssuer
import no.nav.syfo.oidc.OIDCUtil
import org.apache.commons.lang3.StringUtils
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import javax.inject.Inject
import javax.validation.Valid
import javax.validation.constraints.Pattern
import javax.ws.rs.ForbiddenException

@RestController
@ProtectedWithClaims(issuer = OIDCIssuer.EKSTERN, claimMap = ["acr=Level4"])
@RequestMapping(value = ["/api/v2/motebehov"])
class MotebehovBrukerV2Controller @Inject constructor(
        private val contextHolder: OIDCRequestContextHolder,
        private val metrikk: Metrikk,
        private val motebehovService: MotebehovService,
        private val motebehovStatusService: MotebehovStatusService,
        private val brukertilgangService: BrukertilgangService
) {
    @GetMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
    fun motebehovStatus(
            @RequestParam(name = "fnr") arbeidstakerFnr: @Pattern(regexp = "^[0-9]{11}$") String?,
            @RequestParam(name = "virksomhetsnummer") virksomhetsnummer: String
    ): MotebehovStatus {
        val fnr = if (StringUtils.isEmpty(arbeidstakerFnr)) OIDCUtil.fnrFraOIDCEkstern(contextHolder) else Fodselsnummer(arbeidstakerFnr!!)
        kastExceptionHvisIkkeTilgang(fnr.value)
        return motebehovStatusService.motebehovStatus(fnr, virksomhetsnummer)
    }

    @PostMapping(consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun lagreMotebehov(
            @RequestBody nyttMotebehov: @Valid NyttMotebehov
    ) {
        val arbeidstakerFnr = if (nyttMotebehov.arbeidstakerFnr.isNullOrEmpty()) {
            OIDCUtil.fnrFraOIDCEkstern(contextHolder)
        } else Fodselsnummer(nyttMotebehov.arbeidstakerFnr)
        kastExceptionHvisIkkeTilgang(arbeidstakerFnr.value)

        motebehovService.lagreMotebehov(
                OIDCUtil.fnrFraOIDCEkstern(contextHolder),
                arbeidstakerFnr,
                nyttMotebehov.virksomhetsnummer,
                nyttMotebehov.motebehovSvar
        )
        lagBesvarMotebehovMetrikk(nyttMotebehov.motebehovSvar, nyttMotebehov.arbeidstakerFnr.isNullOrEmpty())
    }

    private fun kastExceptionHvisIkkeTilgang(fnr: String) {
        val innloggetIdent = OIDCUtil.fnrFraOIDCEkstern(contextHolder).value
        val harTilgang = brukertilgangService.harTilgangTilOppslaattBruker(innloggetIdent, fnr)
        if (!harTilgang) {
            throw ForbiddenException("Ikke tilgang")
        }
    }

    private fun lagBesvarMotebehovMetrikk(motebehovSvar: MotebehovSvar, erInnloggetBrukerArbeidstaker: Boolean) {
        metrikk.tellMotebehovBesvart(motebehovSvar.harMotebehov, erInnloggetBrukerArbeidstaker)
        if (!motebehovSvar.harMotebehov) {
            metrikk.tellMotebehovBesvartNeiAntallTegn(motebehovSvar.forklaring!!.length, erInnloggetBrukerArbeidstaker)
        } else if (!StringUtils.isEmpty(motebehovSvar.forklaring)) {
            metrikk.tellMotebehovBesvartJaMedForklaringTegn(motebehovSvar.forklaring!!.length, erInnloggetBrukerArbeidstaker)
            metrikk.tellMotebehovBesvartJaMedForklaringAntall(erInnloggetBrukerArbeidstaker)
        }
    }
}
