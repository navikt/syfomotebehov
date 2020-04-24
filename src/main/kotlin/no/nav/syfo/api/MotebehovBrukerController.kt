package no.nav.syfo.api

import no.nav.security.oidc.api.ProtectedWithClaims
import no.nav.security.oidc.context.OIDCRequestContextHolder
import no.nav.syfo.aktorregister.domain.Fodselsnummer
import no.nav.syfo.brukertilgang.BrukertilgangService
import no.nav.syfo.domain.rest.Motebehov
import no.nav.syfo.domain.rest.NyttMotebehov
import no.nav.syfo.oidc.OIDCIssuer
import no.nav.syfo.service.MotebehovService
import no.nav.syfo.metric.Metrikk
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
@RequestMapping(value = ["/api/motebehov"])
class MotebehovBrukerController @Inject constructor(
        private val contextHolder: OIDCRequestContextHolder,
        private val metrikk: Metrikk,
        private val motebehovService: MotebehovService,
        private val brukertilgangService: BrukertilgangService
) {
    @GetMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentMotebehovListe(
            @RequestParam(name = "fnr") arbeidstakerFnr: @Pattern(regexp = "^[0-9]{11}$") String?,
            @RequestParam(name = "virksomhetsnummer") virksomhetsnummer: String
    ): List<Motebehov> {
        val fnr = if (StringUtils.isEmpty(arbeidstakerFnr)) OIDCUtil.fnrFraOIDCEkstern(contextHolder) else Fodselsnummer(arbeidstakerFnr!!)
        kastExceptionHvisIkkeTilgang(fnr.value)
        return if (virksomhetsnummer.isNotEmpty()) {
            motebehovService.hentMotebehovListeForArbeidstakerOpprettetAvLeder(fnr, virksomhetsnummer)
        } else motebehovService.hentMotebehovListeForOgOpprettetAvArbeidstaker(fnr)
    }

    @PostMapping(consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun lagreMotebehov(
            @RequestBody nyttMotebehov: @Valid NyttMotebehov
    ) {
        val fnr = if (StringUtils.isEmpty(nyttMotebehov.arbeidstakerFnr)) OIDCUtil.fnrFraOIDCEkstern(contextHolder) else Fodselsnummer(nyttMotebehov.arbeidstakerFnr)
        kastExceptionHvisIkkeTilgang(fnr.value)
        val erInnloggetBrukerArbeidstaker = StringUtils.isEmpty(nyttMotebehov.arbeidstakerFnr)
        motebehovService.lagreMotebehov(OIDCUtil.fnrFraOIDCEkstern(contextHolder), fnr, nyttMotebehov)
        lagBesvarMotebehovMetrikk(nyttMotebehov, erInnloggetBrukerArbeidstaker)
    }

    private fun kastExceptionHvisIkkeTilgang(fnr: String) {
        val innloggetIdent = OIDCUtil.fnrFraOIDCEkstern(contextHolder).value
        val harTilgang = brukertilgangService.harTilgangTilOppslaattBruker(innloggetIdent, fnr)
        if (!harTilgang) {
            throw ForbiddenException("Ikke tilgang")
        }
    }

    private fun lagBesvarMotebehovMetrikk(nyttMotebehov: NyttMotebehov, erInnloggetBrukerArbeidstaker: Boolean) {
        val motebehovSvar = nyttMotebehov.motebehovSvar
        metrikk.tellMotebehovBesvart(motebehovSvar.harMotebehov, erInnloggetBrukerArbeidstaker)
        if (!motebehovSvar.harMotebehov) {
            metrikk.tellMotebehovBesvartNeiAntallTegn(motebehovSvar.forklaring.length, erInnloggetBrukerArbeidstaker)
        } else if (!StringUtils.isEmpty(motebehovSvar.forklaring)) {
            metrikk.tellMotebehovBesvartJaMedForklaringTegn(motebehovSvar.forklaring.length, erInnloggetBrukerArbeidstaker)
            metrikk.tellMotebehovBesvartJaMedForklaringAntall(erInnloggetBrukerArbeidstaker)
        }
    }

}
