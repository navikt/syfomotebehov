package no.nav.syfo.api

import no.nav.security.oidc.api.ProtectedWithClaims
import no.nav.security.oidc.context.OIDCRequestContextHolder
import no.nav.syfo.aktorregister.domain.Fodselsnummer
import no.nav.syfo.domain.rest.Motebehov
import no.nav.syfo.historikk.Historikk
import no.nav.syfo.historikk.HistorikkService
import no.nav.syfo.metric.Metrikk
import no.nav.syfo.oidc.OIDCIssuer.AZURE
import no.nav.syfo.oidc.getSubjectInternAD
import no.nav.syfo.motebehov.MotebehovService
import no.nav.syfo.veiledertilgang.VeilederTilgangConsumer
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import javax.inject.Inject
import javax.validation.constraints.Pattern
import javax.ws.rs.ForbiddenException

@RestController
@ProtectedWithClaims(issuer = AZURE)
@RequestMapping(value = ["/api/internad/veileder"])
class MotebehovVeilederADController @Inject constructor(
        private val oidcCtxHolder: OIDCRequestContextHolder,
        private val metrikk: Metrikk,
        private val historikkService: HistorikkService,
        private val motebehovService: MotebehovService,
        private val veilederTilgangConsumer: VeilederTilgangConsumer
) {
    @GetMapping(value = ["/motebehov"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentMotebehovListe(
            @RequestParam(name = "fnr") sykmeldtFnr: @Pattern(regexp = "^[0-9]{11}$") String
    ): List<Motebehov> {
        metrikk.tellEndepunktKall("veileder_hent_motebehov")
        val fnr = Fodselsnummer(sykmeldtFnr)
        kastExceptionHvisIkkeTilgang(fnr)
        return motebehovService.hentMotebehovListe(Fodselsnummer(fnr.value))
    }

    @GetMapping(value = ["/historikk"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentMotebehovHistorikk(
            @RequestParam(name = "fnr") sykmeldtFnr: @Pattern(regexp = "^[0-9]{11}$") String
    ): List<Historikk> {
        metrikk.tellEndepunktKall("veileder_hent_motebehov_historikk")
        val fnr = Fodselsnummer(sykmeldtFnr)
        kastExceptionHvisIkkeTilgang(fnr)
        return historikkService.hentHistorikkListe(fnr.value)
    }

    @PostMapping(value = ["/motebehov/{fnr}/behandle"])
    fun behandleMotebehov(
            @PathVariable(name = "fnr") sykmeldtFnr: @Pattern(regexp = "^[0-9]{11}$") String
    ) {
        metrikk.tellEndepunktKall("veileder_behandle_motebehov_call")
        val fnr = Fodselsnummer(sykmeldtFnr)
        kastExceptionHvisIkkeTilgang(fnr)
        motebehovService.behandleUbehandledeMotebehov(fnr, getSubjectInternAD(oidcCtxHolder))
        metrikk.tellEndepunktKall("veileder_behandle_motebehov_success")
    }

    private fun kastExceptionHvisIkkeTilgang(fnr: Fodselsnummer) {
        if (!veilederTilgangConsumer.sjekkVeiledersTilgangTilPerson(fnr)) {
            throw ForbiddenException("Veilederen har ikke tilgang til denne personen")
        }
    }
}
