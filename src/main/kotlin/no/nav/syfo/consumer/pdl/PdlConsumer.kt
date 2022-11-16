package no.nav.syfo.consumer.pdl

import no.nav.syfo.consumer.sts.StsConsumer
import no.nav.syfo.metric.Metric
import no.nav.syfo.util.*
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.*
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.client.RestTemplate

@Service
class PdlConsumer(
    private val metric: Metric,
    @Value("\${pdl.url}") private val pdlUrl: String,
    private val stsConsumer: StsConsumer,
    private val restTemplate: RestTemplate
) {
    fun person(ident: String): PdlHentPerson? {
        metric.tellHendelse("call_pdl")

        val query = this::class.java.getResource("/pdl/hentPerson.graphql").readText().replace("[\n\r]", "")
        val entity = createRequestEntity(PdlRequest(query, Variables(ident)))
        try {
            val pdlPerson = restTemplate.exchange(
                pdlUrl,
                HttpMethod.POST,
                entity,
                object : ParameterizedTypeReference<PdlPersonResponse>() {}
            )

            val pdlPersonReponse = pdlPerson.body!!
            return if (pdlPersonReponse.errors != null && pdlPersonReponse.errors.isNotEmpty()) {
                metric.tellHendelse("call_pdl_fail")
                pdlPersonReponse.errors.forEach {
                    LOG.error("Error while requesting person from PersonDataLosningen: ${it.errorMessage()}")
                }
                null
            } else {
                metric.tellHendelse("call_pdl_success")
                pdlPersonReponse.data
            }
        } catch (exception: RestClientResponseException) {
            metric.tellHendelse("call_pdl_fail")
            LOG.error("Error from PDL with request-url: $pdlUrl", exception)
            throw exception
        }
    }

    fun aktorid(fnr: String): String {
        metric.tellHendelse("call_pdl")

        val query = this::class.java.getResource("/pdl/hentIdenter.graphql").readText().replace("[\n\r]", "")
        val entity = createRequestEntity(
            PdlRequest(query, Variables(ident = fnr, grupper = IdentType.AKTORID.name))
        )
        try {
            val pdlIdenter = restTemplate.exchange(
                pdlUrl,
                HttpMethod.POST,
                entity,
                object : ParameterizedTypeReference<PdlIdenterResponse>() {}
            )

            val pdlIdenterReponse = pdlIdenter.body!!
            if (pdlIdenterReponse.errors != null && pdlIdenterReponse.errors.isNotEmpty()) {
                metric.tellHendelse("call_pdl_fail")
                pdlIdenterReponse.errors.forEach {
                    LOG.error("Error while requesting AKTORID from PersonDataLosningen: ${it.errorMessage()}")
                }
                throw RuntimeException("Error while requesting AKTORID from PDL")
            } else {
                metric.tellHendelse("call_pdl_success")
                try {
                    val aktorid = pdlIdenterReponse.data?.hentIdenter?.identer?.first()?.ident!!
                    return aktorid
                } catch (e: NoSuchElementException) {
                    LOG.info("Error while requesting AKTORID from PDL. Empty list in hentIdenter response")
                    throw RuntimeException("Error while requesting AKTORID from PDL")
                }
            }
        } catch (exception: RestClientResponseException) {
            metric.tellHendelse("call_pdl_fail")
            LOG.error("Error from PDL with request-url: $pdlUrl", exception)
            throw exception
        }
    }

    fun fnr(aktorid: String): String {
        metric.tellHendelse("call_pdl")

        val query = this::class.java.getResource("/pdl/hentIdenter.graphql").readText().replace("[\n\r]", "")
        val entity = createRequestEntity(
            PdlRequest(query, Variables(ident = aktorid, grupper = IdentType.FOLKEREGISTERIDENT.name))
        )
        try {
            val pdlIdenter = restTemplate.exchange(
                pdlUrl,
                HttpMethod.POST,
                entity,
                object : ParameterizedTypeReference<PdlIdenterResponse>() {}
            )

            val pdlIdenterReponse = pdlIdenter.body!!
            if (pdlIdenterReponse.errors != null && pdlIdenterReponse.errors.isNotEmpty()) {
                metric.tellHendelse("call_pdl_fail")
                pdlIdenterReponse.errors.forEach {
                    LOG.error("Error while requesting FNR from PersonDataLosningen: ${it.errorMessage()}")
                }
                throw RuntimeException("Error while requesting FNR from PDL")
            } else {
                metric.tellHendelse("call_pdl_success")
                try {
                    val fnr = pdlIdenterReponse.data?.hentIdenter?.identer?.first()?.ident!!
                    return fnr
                } catch (e: NoSuchElementException) {
                    LOG.info("Error while requesting FNR from PDL. Empty list in hentIdenter response")
                    throw RuntimeException("Error while requesting FNR from PDL")
                }
            }
        } catch (exception: RestClientResponseException) {
            metric.tellHendelse("call_pdl_fail")
            LOG.error("Error from PDL with request-url: $pdlUrl", exception)
            throw exception
        }
    }

    fun isKode6(fnr: String): Boolean {
        return person(fnr)?.isKode6() ?: throw PdlRequestFailedException()
    }

    private fun createRequestEntity(request: PdlRequest): HttpEntity<PdlRequest> {
        val stsToken: String = stsConsumer.token()
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        headers.set(TEMA_HEADER, ALLE_TEMA_HEADERVERDI)
        headers.set(AUTHORIZATION, bearerCredentials(stsToken))
        headers.set(NAV_CONSUMER_TOKEN_HEADER, bearerCredentials(stsToken))
        return HttpEntity(request, headers)
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(PdlConsumer::class.java)
    }
}
