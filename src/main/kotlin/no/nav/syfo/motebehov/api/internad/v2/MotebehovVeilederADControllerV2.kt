package no.nav.syfo.motebehov.api.internad.v2

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.syfo.api.auth.OIDCIssuer.INTERN_AZUREAD_V2
import no.nav.syfo.api.auth.getSubjectInternADV2
import no.nav.syfo.consumer.aktorregister.domain.Fodselsnummer
import no.nav.syfo.consumer.veiledertilgang.VeilederTilgangConsumer
import no.nav.syfo.metric.Metric
import no.nav.syfo.motebehov.Motebehov
import no.nav.syfo.motebehov.MotebehovService
import no.nav.syfo.motebehov.historikk.Historikk
import no.nav.syfo.motebehov.historikk.HistorikkService
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import javax.inject.Inject
import javax.validation.constraints.Pattern
import javax.ws.rs.ForbiddenException

@RestController
@ProtectedWithClaims(issuer = INTERN_AZUREAD_V2)
@RequestMapping(value = ["/api/internad/v2/veileder"])
class MotebehovVeilederADControllerV2 @Inject constructor(
    private val contextHolder: TokenValidationContextHolder,
    private val metric: Metric,
    private val historikkService: HistorikkService,
    private val motebehovService: MotebehovService,
    private val veilederTilgangConsumer: VeilederTilgangConsumer
) {
    @GetMapping(value = ["/motebehov"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentMotebehovListe(
        @RequestParam(name = "fnr") sykmeldtFnr: @Pattern(regexp = "^[0-9]{11}$") String
    ): List<Motebehov> {
        metric.tellEndepunktKall("veileder_hent_motebehov")
        val fnr = Fodselsnummer(sykmeldtFnr)
        kastExceptionHvisIkkeTilgang(fnr)
        return motebehovService.hentMotebehovListe(Fodselsnummer(fnr.value))
    }

    @GetMapping(value = ["/historikk"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentMotebehovHistorikk(
        @RequestParam(name = "fnr") sykmeldtFnr: @Pattern(regexp = "^[0-9]{11}$") String
    ): List<Historikk> {
        metric.tellEndepunktKall("veileder_hent_motebehov_historikk")
        val fnr = Fodselsnummer(sykmeldtFnr)
        kastExceptionHvisIkkeTilgang(fnr)
        return historikkService.hentHistorikkListe(fnr.value)
    }

    @PostMapping(value = ["/motebehov/{fnr}/behandle"])
    fun behandleMotebehov(
        @PathVariable(name = "fnr") sykmeldtFnr: @Pattern(regexp = "^[0-9]{11}$") String
    ) {
        metric.tellEndepunktKall("veileder_behandle_motebehov_call")
        val fnr = Fodselsnummer(sykmeldtFnr)
        kastExceptionHvisIkkeTilgang(fnr)
        motebehovService.behandleUbehandledeMotebehov(fnr, getSubjectInternADV2(contextHolder))
        metric.tellEndepunktKall("veileder_behandle_motebehov_success")
    }

    private fun kastExceptionHvisIkkeTilgang(fnr: Fodselsnummer) {
        if (!veilederTilgangConsumer.sjekkVeiledersTilgangTilPersonMedOBO(fnr)) {
            throw ForbiddenException("Veilederen har ikke tilgang til denne personen")
        }
    }
}
