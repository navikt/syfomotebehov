package no.nav.syfo.consumer.mote

import no.nav.syfo.consumer.sts.StsConsumer
import no.nav.syfo.metric.Metric
import no.nav.syfo.oppfolgingstilfelle.database.PersonOppfolgingstilfelle
import no.nav.syfo.util.*
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.*
import org.springframework.stereotype.Service
import org.springframework.web.client.*
import org.springframework.web.util.UriComponentsBuilder
import java.time.LocalDateTime
import javax.inject.Inject

@Service
class MoteConsumer @Inject constructor(
    private val restTemplate: RestTemplate,
    private val stsConsumer: StsConsumer,
    private val metric: Metric,
    @Value("\${syfomoteadmin.url}") val baseUrl: String
) {
    fun erMoteOpprettetForArbeidstakerEtterDato(aktorId: String, startDato: LocalDateTime): Boolean {
        val stsToken = stsConsumer.token()
        val requestEntity = HttpEntity(startDato, authorizationHeader(stsToken))
        val url = UriComponentsBuilder.fromHttpUrl(baseUrl)
            .pathSegment("system", aktorId, "harAktivtMote")
            .toUriString()
        return try {
            log.info("URL!: $url")
            log.info("Body: ${requestEntity.body}")
            metric.tellHendelse("call_syfomoteadmin")
            val erMoteOpprettetEtterDato = restTemplate.postForObject(url, requestEntity, Boolean::class.java)
            metric.tellHendelse("call_syfomoteadmin_success")
            erMoteOpprettetEtterDato
        } catch (e: HttpClientErrorException) {
            log.warn(SYFOMOTEADMIN_FEILMELDING_KLIENT)
            throw e
        } catch (e: HttpServerErrorException) {
            metric.tellHendelse("call_syfomoteadmin_fail")
            log.error(SYFOMOTEADMIN_FEILMELDING_SERVER, e)
            throw e
        } catch (e: RuntimeException) {
            log.error(SYFOMOTEADMIN_FEILMELDING_GENERELL, e)
            throw e
        }
    }

    fun isMoteplanleggerActiveInOppfolgingstilfelle(oppfolgingstilfelle: PersonOppfolgingstilfelle): Boolean {
        val stsToken = stsConsumer.token()
        val httpEntity = entity(null, stsToken, oppfolgingstilfelle.fnr.value, oppfolgingstilfelle.fom.atStartOfDay())
        try {
            val response = restTemplate.exchange(
                getMoteadminUrl("/system/moteplanlegger/aktiv"),
                HttpMethod.POST,
                httpEntity,
                Boolean::class.java
            )
            val responseBody = response.body!!
            metric.tellHendelse(METRIC_CALL_MOTEADMIN_MOTEPLANLEGGER_ACTIVE_SUCCESS)
            return responseBody
        } catch (e: RestClientResponseException) {
            log.error("Request to get check if Moteplanlegger is active for Arbeidstaker failed with status: ${e.rawStatusCode} and message: ${e.responseBodyAsString}")
            metric.tellHendelse(METRIC_CALL_MOTEADMIN_MOTEPLANLEGGER_ACTIVE_FAIL)
            throw e
        }
    }

    private fun getMoteadminUrl(subPath: String) = "$baseUrl$subPath"

    private fun entity(
        callId: String?,
        token: String,
        arbeidstakerIdent: String,
        oppfolgingstilfelleStartDate: LocalDateTime
    ): HttpEntity<LocalDateTime> {
        val credentials = bearerCredentials(token)
        val headers = HttpHeaders()
        headers[HttpHeaders.AUTHORIZATION] = credentials
        headers[NAV_CALL_ID_HEADER] = getOrCreateCallId(callId)
        headers[NAV_CONSUMER_ID_HEADER] = APP_CONSUMER_ID
        headers[NAV_PERSONIDENTER_HEADER] = arbeidstakerIdent

        return HttpEntity(
            oppfolgingstilfelleStartDate,
            headers
        )
    }

    private fun authorizationHeader(token: String): HttpHeaders {
        val headers = HttpHeaders()
        headers.add(HttpHeaders.AUTHORIZATION, bearerCredentials(token))
        return headers
    }

    companion object {
        private val log = LoggerFactory.getLogger(MoteConsumer::class.java)

        private const val METRIC_CALL_MOTEADMIN_BASE = "call_behandlendeenhet"
        private const val METRIC_CALL_MOTEADMIN_MOTEPLANLEGGER_ACTIVE_SUCCESS = "${METRIC_CALL_MOTEADMIN_BASE}_moteplanlegger_active_success"
        private const val METRIC_CALL_MOTEADMIN_MOTEPLANLEGGER_ACTIVE_FAIL = "${METRIC_CALL_MOTEADMIN_BASE}_moteplanlegger_active_fail"

        private const val SYFOMOTEADMIN_FEILMELDING_GENERELL = "Klarte ikke hente om det er mote i oppfolgingstilfelle fra syfomoteadmin"
        private const val SYFOMOTEADMIN_FEILMELDING_KLIENT = "Fikk 4XX-feil ved henting av om det er mote i oppfolgingstilfelle fra syfomoteadmin"
        private const val SYFOMOTEADMIN_FEILMELDING_SERVER = "Fikk 5XX-feil ved henting av om det er mote i oppfolgingstilfelle fra syfomoteadmin"
    }
}
