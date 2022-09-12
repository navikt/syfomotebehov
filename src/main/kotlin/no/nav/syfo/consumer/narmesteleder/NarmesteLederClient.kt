package no.nav.syfo.consumer.narmesteleder

import no.nav.syfo.consumer.azuread.v2.AzureAdV2TokenConsumer
import no.nav.syfo.util.APP_CONSUMER_ID
import no.nav.syfo.util.NAV_CALL_ID_HEADER
import no.nav.syfo.util.NAV_CONSUMER_ID_HEADER
import no.nav.syfo.util.NAV_PERSONIDENT_HEADER
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@Service
class NarmesteLederClient(
    private val azureAdV2TokenConsumer: AzureAdV2TokenConsumer,
    @Value("\${isnarmesteleder.url}") private val baseUrl: String,
    @Value("\${isnarmesteleder.client.id}") private val targetApp: String,
    @Qualifier("restTemplateWithProxy") private val restTemplateWithProxy: RestTemplate
) {
    fun getNarmesteledere(fnr: String): List<NarmesteLederRelasjonDTO>? {
        try {
            val token = azureAdV2TokenConsumer.getSystemToken(
                scopeClientId = targetApp
            )

            val response: ResponseEntity<List<NarmesteLederRelasjonDTO>?> = restTemplateWithProxy.exchange(
                "$baseUrl/api/system/v1/narmestelederrelasjoner",
                HttpMethod.GET,
                entity(token, fnr),
                object : ParameterizedTypeReference<List<NarmesteLederRelasjonDTO>?>() {}
            )

            return response.body
        } catch (e: Exception) {
            log.error("Noe gikk galt ved henting av nærmeste leder", e)
            throw e
        }
    }

    private fun entity(token: String, fnr: String): HttpEntity<String> {
        val headers = HttpHeaders()
        headers[HttpHeaders.AUTHORIZATION] = "Bearer $token"
        headers[NAV_CALL_ID_HEADER] = UUID.randomUUID().toString()
        headers[NAV_CONSUMER_ID_HEADER] = APP_CONSUMER_ID
        headers[NAV_PERSONIDENT_HEADER] = fnr
        return HttpEntity(headers)
    }

    companion object {
        private val log = LoggerFactory.getLogger(NarmesteLederClient::class.java)
    }
}

data class NarmesteLederRelasjonDTO(
    val uuid: String,
    val arbeidstakerPersonIdentNumber: String,
    val virksomhetsnavn: String?,
    val virksomhetsnummer: String,
    val narmesteLederPersonIdentNumber: String,
    val narmesteLederTelefonnummer: String,
    val narmesteLederEpost: String,
    val narmesteLederNavn: String?,
    val aktivFom: LocalDate,
    val aktivTom: LocalDate?,
    val arbeidsgiverForskutterer: Boolean?,
    val timestamp: LocalDateTime,
    val status: NarmesteLederRelasjonStatus,
)

enum class NarmesteLederRelasjonStatus {
    INNMELDT_AKTIV,
    DEAKTIVERT,
    DEAKTIVERT_ARBEIDSTAKER,
    DEAKTIVERT_ARBEIDSTAKER_INNSENDT_SYKMELDING,
    DEAKTIVERT_LEDER,
    DEAKTIVERT_ARBEIDSFORHOLD,
    DEAKTIVERT_NY_LEDER,
}
