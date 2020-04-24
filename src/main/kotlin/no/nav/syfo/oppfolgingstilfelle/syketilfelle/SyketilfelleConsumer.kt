package no.nav.syfo.oppfolgingstilfelle.syketilfelle

import no.nav.syfo.brukertilgang.BrukertilgangConsumer
import no.nav.syfo.brukertilgang.RequestUnauthorizedException
import no.nav.syfo.metric.Metrikk
import no.nav.syfo.sts.StsConsumer
import no.nav.syfo.util.*
import org.slf4j.LoggerFactory
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.client.RestTemplate

@Component
class SyketilfelleConsumer(
        private val restTemplate: RestTemplate,
        private val metrikk: Metrikk,
        private val stsConsumer: StsConsumer
) {
    fun oppfolgingstilfelle(
            aktorId: String,
            virksomhetsnummer: String
    ): KOppfolgingstilfelle? {
        val httpEntity = entity(stsConsumer.token())
        try {
            val response = restTemplate.exchange(
                    syfosyketilfelleUrl(aktorId, virksomhetsnummer),
                    HttpMethod.GET,
                    httpEntity,
                    KOppfolgingstilfelle::class.java
            )
            metrikk.countOutgoingReponses(METRIC_CALL_SYFOSYKETILFELLE, response.statusCodeValue)
            return if (response.statusCodeValue == 204) {
                null
            } else {
                return response.body!!
            }
        } catch (e: RestClientResponseException) {
            metrikk.countOutgoingReponses(METRIC_CALL_SYFOSYKETILFELLE, e.rawStatusCode)
            if (e.rawStatusCode == 401) {
                throw RequestUnauthorizedException("Unauthorized request to get access to Ansatt from Syfobrukertilgang")
            } else {
                LOG.error("Error requesting ansatt access from syfobrukertilgang with callId ${httpEntity.headers[NAV_CALL_ID_HEADER]}: ", e)
                throw e
            }
        }
    }

    private fun entity(token: String): HttpEntity<*> {
        val headers = HttpHeaders()
        headers[HttpHeaders.AUTHORIZATION] = bearerCredentials(token)
        headers[NAV_CALL_ID_HEADER] = createCallId()
        headers[NAV_CONSUMER_ID_HEADER] = APP_CONSUMER_ID
        return HttpEntity<Any>(headers)
    }

    private fun syfosyketilfelleUrl(aktorId: String, virksomhetsnummer: String): String {
        return "http://syfosyketilfelle/kafka/oppfolgingstilfelle/beregn/$aktorId/$virksomhetsnummer"
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(BrukertilgangConsumer::class.java)

        const val METRIC_CALL_SYFOSYKETILFELLE = "call_syfosyketilfele"
    }
}
