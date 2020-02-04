package no.nav.syfo.brukertilgang

import no.nav.security.oidc.context.OIDCRequestContextHolder
import no.nav.syfo.oidc.OIDCIssuer
import no.nav.syfo.util.*
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.*
import org.springframework.stereotype.Service
import org.springframework.web.client.*

@Service
class BrukertilgangConsumer(
        private val oidcContextHolder: OIDCRequestContextHolder,
        private val restTemplate: RestTemplate,
        private val metrikk: Metrikk,
        @Value("\${syfobrukertilgang.url}") private val baseUrl: String
) {
    fun hasAccessToAnsatt(ansattFnr: String): Boolean {
        metrikk.tellHendelse("call_syfobrukertilgang")
        try {
            val response = restTemplate.exchange<Boolean>(
                    arbeidstakerUrl(ansattFnr),
                    HttpMethod.GET,
                    entity(),
                    Boolean::class.java
            )

            val responseBody = response.body!!
            metrikk.tellHendelse("call_syfobrukertilgang_success")
            return responseBody
        } catch (e: RestClientException) {
            throw e
        } catch (e: HttpServerErrorException) {
            metrikk.tellHendelse("call_syfobrukertilgang_fail")
            LOG.error("Error requesting ansatt access from syfobrukertilgang: ", e)
            throw e
        }
    }

    private fun entity(): HttpEntity<*> {
        val headers = HttpHeaders()
        headers.add(HttpHeaders.AUTHORIZATION, bearerCredentials(OIDCUtil.tokenFraOIDC(oidcContextHolder, OIDCIssuer.EKSTERN)))
        return HttpEntity<Any>(headers)
    }

    private fun arbeidstakerUrl(ansattFnr: String): String {
        return "$baseUrl/api/v1/tilgang/ansatt/$ansattFnr"
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(BrukertilgangConsumer::class.java)
    }
}
