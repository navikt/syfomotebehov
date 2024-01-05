package no.nav.syfo.config

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.*
import org.springframework.scheduling.TaskScheduler
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler
import org.springframework.transaction.annotation.EnableTransactionManagement
import org.springframework.web.client.RestTemplate
import org.springframework.web.reactive.function.client.WebClient

@Configuration
@EnableTransactionManagement
@EnableScheduling
class ApplicationConfig {
    @Bean
    fun taskScheduler(): TaskScheduler = ConcurrentTaskScheduler()

    @Primary
    @Bean
    fun restTemplate() = RestTemplate()

    @Bean
    @Qualifier("AzureAD")
    fun restTemplateAzureAd() = RestTemplate()

    @Bean
    fun webClient() = WebClient
        .builder()
        .build()
}
