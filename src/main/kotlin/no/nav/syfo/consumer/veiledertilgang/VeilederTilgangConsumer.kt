package no.nav.syfo.consumer.veiledertilgang

import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.syfo.api.auth.OIDCIssuer
import no.nav.syfo.api.auth.OIDCUtil
import no.nav.syfo.consumer.aktorregister.domain.Fodselsnummer
import no.nav.syfo.consumer.azuread.v2.AzureAdV2TokenConsumer
import no.nav.syfo.metric.Metric
import no.nav.syfo.util.*
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.*
import org.springframework.stereotype.Service
import org.springframework.web.client.*
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI
import java.util.*

@Service
class VeilederTilgangConsumer(
    @Value("\${syfotilgangskontroll.client.id}") private val syfotilgangskontrollClientId: String,
    @Value("\${tilgangskontrollapi.url}") private val tilgangskontrollUrl: String,
    private val azureAdV2TokenConsumer: AzureAdV2TokenConsumer,
    private val metric: Metric,
    private val template: RestTemplate,
    private val oidcContextHolder: TokenValidationContextHolder
) {
    private val tilgangTilBrukerViaAzureUriTemplate: UriComponentsBuilder

    fun sjekkVeiledersTilgangTilPersonMedOBO(fnr: Fodselsnummer): Boolean {
        val token = OIDCUtil.tokenFraOIDC(oidcContextHolder, OIDCIssuer.INTERN_AZUREAD_V2)
        val oboToken = azureAdV2TokenConsumer.getOnBehalfOfToken(
            scopeClientId = syfotilgangskontrollClientId,
            token = token
        )

        val url = accessToUserV2Url(fnr)

        return try {
            template.exchange(
                url,
                HttpMethod.GET,
                entity(token = oboToken),
                String::class.java
            )
            true
        } catch (e: HttpClientErrorException) {
            if (e.rawStatusCode == 403) {
                false
            } else {
                LOG.error("HttpClientErrorException mot syfo-tilgangskontroll med status ${e.rawStatusCode}", e)
                metric.tellHendelse(METRIC_CALL_VEILEDERTILGANG_USER_FAIL)
                throw e
            }
        } catch (e: HttpServerErrorException) {
            LOG.error("HttpServerErrorException mot syfo-tilgangskontroll med status ${e.rawStatusCode}", e)
            metric.tellHendelse(METRIC_CALL_VEILEDERTILGANG_USER_FAIL)
            throw e
        }
    }

    fun accessToUserV2Url(fnr: Fodselsnummer): String {
        return "$tilgangskontrollUrl$TILGANG_TIL_BRUKER_VIA_AZURE_V2_PATH/${fnr.value}"
    }

    fun sjekkVeiledersTilgangTilPerson(fnr: Fodselsnummer): Boolean {
        val tilgangTilBrukerViaAzureUriMedFnr = tilgangTilBrukerViaAzureUriTemplate.build(Collections.singletonMap(FNR, fnr.value))
        return checkAccess(tilgangTilBrukerViaAzureUriMedFnr, OIDCIssuer.AZURE)
    }

    private fun checkAccess(uri: URI, oidcIssuer: String): Boolean {
        val httpEntity = entity(
            token = OIDCUtil.tokenFraOIDC(oidcContextHolder, oidcIssuer)
        )
        return try {
            template.exchange(
                uri,
                HttpMethod.GET,
                httpEntity,
                String::class.java
            )
            true
        } catch (e: HttpClientErrorException) {
            if (e.rawStatusCode == 403) {
                false
            } else {
                metric.tellHendelse(METRIC_CALL_VEILEDERTILGANG_USER_FAIL)
                LOG.error("Error requesting ansatt access from syfobrukertilgang with status-${e.rawStatusCode} callId-${httpEntity.headers[NAV_CALL_ID_HEADER]}: ", e)
                throw e
            }
        }
    }

    private fun entity(token: String): HttpEntity<String> {
        val headers = HttpHeaders()
        headers.accept = listOf(MediaType.APPLICATION_JSON)
        headers.setBearerAuth(token)
        headers[NAV_CALL_ID_HEADER] = createCallId()
        headers[NAV_CONSUMER_ID_HEADER] = APP_CONSUMER_ID
        return HttpEntity(headers)
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(VeilederTilgangConsumer::class.java)

        private const val METRIC_CALL_VEILEDERTILGANG_BASE = "call_syfotilgangskontroll"
        private const val METRIC_CALL_VEILEDERTILGANG_USER_FAIL = "${METRIC_CALL_VEILEDERTILGANG_BASE}_user_fail"

        const val FNR = "fnr"
        const val TILGANG_TIL_BRUKER_VIA_AZURE_PATH = "/bruker"
        private const val FNR_PLACEHOLDER = "{$FNR}"

        const val TILGANG_TIL_BRUKER_VIA_AZURE_V2_PATH = "/navident/bruker"
    }

    init {
        tilgangTilBrukerViaAzureUriTemplate = UriComponentsBuilder.fromHttpUrl(tilgangskontrollUrl)
            .path(TILGANG_TIL_BRUKER_VIA_AZURE_PATH)
            .queryParam(FNR, FNR_PLACEHOLDER)
    }
}
