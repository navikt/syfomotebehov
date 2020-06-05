package no.nav.syfo.consumer.veiledertilgang

import no.nav.security.oidc.context.OIDCRequestContextHolder
import no.nav.syfo.api.auth.OIDCIssuer
import no.nav.syfo.api.auth.OIDCUtil
import no.nav.syfo.consumer.aktorregister.domain.Fodselsnummer
import no.nav.syfo.metric.Metric
import no.nav.syfo.util.*
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.*
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.reactive.function.client.*
import org.springframework.web.util.UriComponentsBuilder
import java.util.*

@Service
class VeilederTilgangConsumer(
    @Value("\${tilgangskontrollapi.url}") tilgangskontrollUrl: String,
    private val metric: Metric,
    private val webClient: WebClient,
    private val oidcContextHolder: OIDCRequestContextHolder
) {
    private val tilgangTilBrukerViaAzureUriTemplate: UriComponentsBuilder

    suspend fun sjekkVeiledersTilgangTilPerson(fnr: Fodselsnummer): Boolean {
        val tilgangTilBrukerViaAzureUriMedFnr = tilgangTilBrukerViaAzureUriTemplate.build(Collections.singletonMap(FNR, fnr.value))
        val callId = createCallId()
        val reponse = webClient
            .get()
            .uri(tilgangTilBrukerViaAzureUriMedFnr)
            .accept(MediaType.APPLICATION_JSON)
            .header(HttpHeaders.AUTHORIZATION, bearerCredentials(OIDCUtil.tokenFraOIDC(oidcContextHolder, OIDCIssuer.AZURE)))
            .header(NAV_CALL_ID_HEADER, callId)
            .header(NAV_CONSUMER_ID_HEADER, APP_CONSUMER_ID)
            .awaitExchange()
        val statusCode = HttpStatus.resolve(reponse.rawStatusCode()) ?: HttpStatus.INTERNAL_SERVER_ERROR
        return when {
            statusCode.is2xxSuccessful -> {
                reponse.awaitBody()
            }
            statusCode.value() == 403 -> {
                false
            }
            else -> {
                metric.tellHendelse(METRIC_CALL_VEILEDERTILGANG_USER_FAIL)
                LOG.error("Error requesting ansatt access from syfobrukertilgang with status-$statusCode callId-$callId")
                throw HttpClientErrorException(statusCode)
            }
        }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(VeilederTilgangConsumer::class.java)

        private const val METRIC_CALL_VEILEDERTILGANG_BASE = "call_syfotilgangskontroll"
        private const val METRIC_CALL_VEILEDERTILGANG_USER_FAIL = "${METRIC_CALL_VEILEDERTILGANG_BASE}_user_fail"

        const val FNR = "fnr"
        const val TILGANG_TIL_BRUKER_VIA_AZURE_PATH = "/bruker"
        private const val FNR_PLACEHOLDER = "{$FNR}"
    }

    init {
        tilgangTilBrukerViaAzureUriTemplate = UriComponentsBuilder.fromHttpUrl(tilgangskontrollUrl)
            .path(TILGANG_TIL_BRUKER_VIA_AZURE_PATH)
            .queryParam(FNR, FNR_PLACEHOLDER)
    }
}
