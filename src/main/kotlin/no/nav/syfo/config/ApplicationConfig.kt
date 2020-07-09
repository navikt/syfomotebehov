package no.nav.syfo.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
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

    @Bean
    fun restTemplate() = RestTemplate()

    @Bean
    fun webClient() = WebClient
        .builder()
        .build()
}
