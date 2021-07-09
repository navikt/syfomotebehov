package no.nav.syfo.consumer.azuread

import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestTemplate

@Configuration
class RestTemplateWithProxyConfig {
    @Bean(name = ["restTemplateWithProxy"])
    fun restTemplateWithProxy(): RestTemplate {
        return RestTemplateBuilder()
            .additionalCustomizers(NaisProxyCustomizer())
            .build()
    }
}
