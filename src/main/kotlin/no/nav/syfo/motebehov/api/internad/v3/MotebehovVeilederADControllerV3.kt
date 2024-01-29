package no.nav.syfo.motebehov.api.internad.v3

import jakarta.ws.rs.BadRequestException
import jakarta.ws.rs.ForbiddenException
import jakarta.ws.rs.NotFoundException
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.syfo.api.auth.OIDCIssuer.INTERN_AZUREAD_V2
import no.nav.syfo.api.auth.getSubjectInternADV2
import no.nav.syfo.consumer.pdl.PdlConsumer
import no.nav.syfo.consumer.pdl.fullName
import no.nav.syfo.consumer.veiledertilgang.VeilederTilgangConsumer
import no.nav.syfo.metric.Metric
import no.nav.syfo.motebehov.MotebehovService
import no.nav.syfo.motebehov.MotebehovTilbakemelding
import no.nav.syfo.motebehov.api.internad.dto.MotebehovVeilederDTO
import no.nav.syfo.motebehov.historikk.Historikk
import no.nav.syfo.motebehov.historikk.HistorikkService
import no.nav.syfo.motebehov.toMotebehovVeilederDTOList
import no.nav.syfo.util.NAV_PERSONIDENT_HEADER
import no.nav.syfo.varsel.esyfovarsel.EsyfovarselService
import org.jsoup.Jsoup
import org.jsoup.safety.Safelist
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import javax.inject.Inject
import javax.validation.Valid
import javax.validation.constraints.Pattern

@RestController
@ProtectedWithClaims(issuer = INTERN_AZUREAD_V2)
@RequestMapping(value = ["/api/internad/v3/veileder"])
class MotebehovVeilederADControllerV3 @Inject constructor(
    private val contextHolder: TokenValidationContextHolder,
    private val metric: Metric,
    private val historikkService: HistorikkService,
    private val motebehovService: MotebehovService,
    private val pdlConsumer: PdlConsumer,
    private val veilederTilgangConsumer: VeilederTilgangConsumer,
    private val esyfovarselService: EsyfovarselService,
) {
    @GetMapping(value = ["/motebehov"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentMotebehovListe(
        @RequestHeader(name = NAV_PERSONIDENT_HEADER) personident: @Pattern(regexp = "^[0-9]{11}$") String
    ): List<MotebehovVeilederDTO> {
        metric.tellEndepunktKall("veileder_hent_motebehov")

        kastExceptionHvisIkkeTilgang(personident)
        val motebehovVeilederDTOList = motebehovService.hentMotebehovListe(personident)
            .toMotebehovVeilederDTOList()
            .map { motebehovVeilederDTO ->
                val opprettetAvAktorId = motebehovVeilederDTO.opprettetAv
                motebehovVeilederDTO.copy(
                    opprettetAvNavn = pdlConsumer.person(opprettetAvAktorId)?.fullName()
                )
            }
        return motebehovVeilederDTOList
    }

    @GetMapping(value = ["/historikk"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentMotebehovHistorikk(
        @RequestParam(name = NAV_PERSONIDENT_HEADER) personident: @Pattern(regexp = "^[0-9]{11}$") String
    ): List<Historikk> {
        metric.tellEndepunktKall("veileder_hent_motebehov_historikk")
        kastExceptionHvisIkkeTilgang(personident)
        return historikkService.hentHistorikkListe(personident)
    }

    @PostMapping(
        value = ["/motebehov/tilbakemelding"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun sendTilbakemelding(
        @RequestBody tilbakemelding: @Valid MotebehovTilbakemelding,
    ) {
        metric.tellEndepunktKall("veileder_motebehov-tilbakemelding_call")

        val motebehov = motebehovService.hentMotebehov(tilbakemelding.motebehovId)

        if (motebehov === null) {
            throw NotFoundException()
        }

        kastExceptionHvisIkkeTilgang(motebehov.arbeidstakerFnr)

        if (!Jsoup.isValid(tilbakemelding.varseltekst, Safelist.none())) {
            throw BadRequestException("Invalid input")
        }

        esyfovarselService.sendTilbakemeldingsvarsel(tilbakemelding, motebehov)
        metric.tellEndepunktKall("veileder_motebehov-tilbakemelding_call_success")
    }

    @PostMapping(value = ["/motebehov/behandle"])
    fun behandleMotebehov(
        @RequestHeader(name = NAV_PERSONIDENT_HEADER) personident: @Pattern(regexp = "^[0-9]{11}$") String
    ) {
        metric.tellEndepunktKall("veileder_behandle_motebehov_call")
        kastExceptionHvisIkkeTilgang(personident)
        motebehovService.behandleUbehandledeMotebehov(personident, getSubjectInternADV2(contextHolder))
        metric.tellEndepunktKall("veileder_behandle_motebehov_success")
    }

    private fun kastExceptionHvisIkkeTilgang(fnr: String) {
        if (!veilederTilgangConsumer.sjekkVeiledersTilgangTilPersonMedOBO(fnr)) {
            throw ForbiddenException("Veilederen har ikke tilgang til denne personen")
        }
    }
}
