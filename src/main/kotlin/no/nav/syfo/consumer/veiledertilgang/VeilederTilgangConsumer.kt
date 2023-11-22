package no.nav.syfo.consumer.veiledertilgang

import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.syfo.api.auth.OIDCIssuer
import no.nav.syfo.api.auth.OIDCUtil
import no.nav.syfo.consumer.azuread.v2.AzureAdV2TokenConsumer
import no.nav.syfo.metric.Metric
import no.nav.syfo.util.*
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.*
import org.springframework.stereotype.Service
import org.springframework.web.client.*

@Service
class VeilederTilgangConsumer(
    @Value("\${istilgangskontroll.client.id}") private val istilgangskontrollClientId: String,
    @Value("\${istilgangskontroll.url}") private val istilgangskontrollUrl: String,
    private val azureAdV2TokenConsumer: AzureAdV2TokenConsumer,
    private val metric: Metric,
    private val template: RestTemplate,
    private val oidcContextHolder: TokenValidationContextHolder,
) {
    private val tilgangskontrollPersonUrl: String

    init {
        tilgangskontrollPersonUrl = "$istilgangskontrollUrl$TILGANGSKONTROLL_PERSON_PATH"
    }

    fun sjekkVeiledersTilgangTilPersonMedOBO(fnr: String): Boolean {
        val token = OIDCUtil.tokenFraOIDC(oidcContextHolder, OIDCIssuer.INTERN_AZUREAD_V2)
        val oboToken = azureAdV2TokenConsumer.getOnBehalfOfToken(
            scopeClientId = istilgangskontrollClientId,
            token = token,
        )

        return try {
            val tilgang = template.exchange(
                tilgangskontrollPersonUrl,
                HttpMethod.GET,
                entity(
                    personIdentNumber = fnr,
                    token = oboToken,
                ),
                Tilgang::class.java,
            )
            tilgang.body!!.erGodkjent
        } catch (e: HttpClientErrorException) {
            if (e.statusCode.value() == 403) {
                false
            } else {
                LOG.error("HttpClientErrorException mot istilgangskontroll med status ${e.rawStatusCode}", e)
                metric.tellHendelse(METRIC_CALL_VEILEDERTILGANG_USER_FAIL)
                throw e
            }
        } catch (e: HttpServerErrorException) {
            LOG.error("HttpServerErrorException mot istilgangskontroll med status ${e.rawStatusCode}", e)
            metric.tellHendelse(METRIC_CALL_VEILEDERTILGANG_USER_FAIL)
            throw e
        }
    }

    private fun entity(
        personIdentNumber: String,
        token: String,
    ): HttpEntity<String> {
        val headers = HttpHeaders()
        headers.accept = listOf(MediaType.APPLICATION_JSON)
        headers.setBearerAuth(token)
        headers[NAV_PERSONIDENT_HEADER] = personIdentNumber
        headers[NAV_CALL_ID_HEADER] = createCallId()
        headers[NAV_CONSUMER_ID_HEADER] = APP_CONSUMER_ID
        return HttpEntity(headers)
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(VeilederTilgangConsumer::class.java)

        private const val METRIC_CALL_VEILEDERTILGANG_BASE = "call_istilgangskontroll"
        private const val METRIC_CALL_VEILEDERTILGANG_USER_FAIL = "${METRIC_CALL_VEILEDERTILGANG_BASE}_user_fail"

        const val TILGANGSKONTROLL_COMMON_PATH = "/api/tilgang/navident"
        const val TILGANGSKONTROLL_PERSON_PATH = "$TILGANGSKONTROLL_COMMON_PATH/person"
    }
}
