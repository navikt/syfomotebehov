package no.nav.syfo.consumer.azuread

import org.apache.hc.client5.http.classic.HttpClient
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder
import org.apache.hc.client5.http.impl.routing.DefaultProxyRoutePlanner
import org.apache.hc.core5.http.HttpException
import org.apache.hc.core5.http.HttpHost
import org.apache.hc.core5.http.protocol.HttpContext
import org.springframework.boot.web.client.RestTemplateCustomizer
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.web.client.RestTemplate

class NaisProxyCustomizer : RestTemplateCustomizer {
    override fun customize(restTemplate: RestTemplate) {
        val proxy = HttpHost("webproxy-nais.nav.no", 8088)
        val client: HttpClient = HttpClientBuilder.create()
            .setRoutePlanner(
                object : DefaultProxyRoutePlanner(proxy) {
                    @Throws(HttpException::class)
                    public override fun determineProxy(
                        target: HttpHost,
                        context: HttpContext
                    ): HttpHost? {
                        return if (target.hostName.contains("microsoft")) {
                            super.determineProxy(target, context)
                        } else null
                    }
                }
            ).build()
        restTemplate.requestFactory = HttpComponentsClientHttpRequestFactory(client)
    }
}
