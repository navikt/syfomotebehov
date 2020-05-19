package no.nav.syfo.motebehov.api

import no.nav.security.oidc.api.ProtectedWithClaims
import no.nav.security.oidc.context.OIDCRequestContextHolder
import no.nav.syfo.consumer.aktorregister.domain.Fodselsnummer
import no.nav.syfo.consumer.brukertilgang.BrukertilgangService
import no.nav.syfo.motebehov.Motebehov
import no.nav.syfo.motebehov.MotebehovSvar
import no.nav.syfo.metric.Metric
import no.nav.syfo.motebehov.MotebehovService
import no.nav.syfo.motebehov.NyttMotebehov
import no.nav.syfo.api.auth.OIDCIssuer
import no.nav.syfo.api.auth.OIDCUtil
import no.nav.syfo.motebehov.motebehovstatus.MotebehovSkjemaType
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
        private val metric: Metric,
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
        lagBesvarMotebehovMetrikk(nyttMotebehov.motebehovSvar, false)
    }

    private fun kastExceptionHvisIkkeTilgang(fnr: String) {
        val innloggetIdent = OIDCUtil.fnrFraOIDCEkstern(contextHolder).value
        val harTilgang = brukertilgangService.harTilgangTilOppslaattBruker(innloggetIdent, fnr)
        if (!harTilgang) {
            throw ForbiddenException("Ikke tilgang")
        }
    }

    private fun lagBesvarMotebehovMetrikk(motebehovSvar: MotebehovSvar, erInnloggetBrukerArbeidstaker: Boolean) {
        metric.tellMotebehovBesvart(null, MotebehovSkjemaType.SVAR_BEHOV, motebehovSvar.harMotebehov, erInnloggetBrukerArbeidstaker)
        if (!motebehovSvar.harMotebehov) {
            metric.tellMotebehovBesvartNeiAntallTegn(motebehovSvar.forklaring!!.length, erInnloggetBrukerArbeidstaker)
        } else if (!StringUtils.isEmpty(motebehovSvar.forklaring)) {
            metric.tellMotebehovBesvartJaMedForklaringTegn(motebehovSvar.forklaring!!.length, erInnloggetBrukerArbeidstaker)
            metric.tellMotebehovBesvartJaMedForklaringAntall(erInnloggetBrukerArbeidstaker)
        }
    }

}
