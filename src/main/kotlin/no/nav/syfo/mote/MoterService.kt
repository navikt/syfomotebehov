package no.nav.syfo.mote

import no.nav.syfo.metric.Metric
import no.nav.syfo.sts.StsConsumer
import no.nav.syfo.util.bearerCredentials
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.time.LocalDateTime
import javax.inject.Inject

@Service
class MoterService @Inject constructor(
        private val template: RestTemplate,
        private val stsConsumer: StsConsumer,
        private val metric: Metric
) {
    fun erMoteOpprettetForArbeidstakerEtterDato(aktorId: String, startDato: LocalDateTime): Boolean {
        val stsToken = stsConsumer.token()
        val requestEntity = HttpEntity(startDato, authorizationHeader(stsToken))
        val url = UriComponentsBuilder.fromHttpUrl(SYFOMOTEADMIN_BASEURL)
                .pathSegment("system", aktorId, "harAktivtMote")
                .toUriString()
        return try {
            metric.tellHendelse("call_syfomoteadmin")
            val erMoteOpprettetEtterDato = template.postForObject(url, requestEntity, Boolean::class.java)
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

    private fun authorizationHeader(token: String): HttpHeaders {
        val headers = HttpHeaders()
        headers.add(HttpHeaders.AUTHORIZATION, bearerCredentials(token))
        return headers
    }

    companion object {
        private val log = LoggerFactory.getLogger(MoterService::class.java)
        const val SYFOMOTEADMIN_BASEURL = "http://syfomoteadmin/syfomoteadmin/api"
        private const val SYFOMOTEADMIN_FEILMELDING_GENERELL = "Klarte ikke hente om det er mote i oppfolgingstilfelle fra syfomoteadmin"
        private const val SYFOMOTEADMIN_FEILMELDING_KLIENT = "Fikk 4XX-feil ved henting av om det er mote i oppfolgingstilfelle fra syfomoteadmin"
        private const val SYFOMOTEADMIN_FEILMELDING_SERVER = "Fikk 5XX-feil ved henting av om det er mote i oppfolgingstilfelle fra syfomoteadmin"
    }
}
