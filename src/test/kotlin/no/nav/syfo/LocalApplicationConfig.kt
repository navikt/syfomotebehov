package no.nav.syfo

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import no.nav.security.token.support.spring.test.MockOAuth2ServerAutoConfiguration
import no.nav.syfo.config.kafka.FunctionSerializer
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory

@Configuration
@Import(MockOAuth2ServerAutoConfiguration::class)
class LocalApplicationConfig {
    @Bean
    fun kafkaTemplate(producerFactory: ProducerFactory<String, Any>): KafkaTemplate<String, Any> {
        return KafkaTemplate(producerFactory)
    }

    @Bean
    fun producerFactory(properties: KafkaProperties): ProducerFactory<String, Any> {
        val objectMapper = ObjectMapper()
            .registerModule(JavaTimeModule())
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        return DefaultKafkaProducerFactory(
            properties.buildProducerProperties(),
            StringSerializer(),
            FunctionSerializer { value ->
                try {
                    return@FunctionSerializer objectMapper.writeValueAsBytes(value)
                } catch (jsonProcessingException: JsonProcessingException) {
                    throw RuntimeException(jsonProcessingException)
                }
            }
        )
    }
}
