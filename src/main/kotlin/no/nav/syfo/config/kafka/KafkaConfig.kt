package no.nav.syfo.config.kafka

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory

@Profile("remote")
@Configuration
@EnableKafka
class KafkaConfig {
    @Bean
    fun kafkaTemplate(producerFactory: ProducerFactory<String, Any>?): KafkaTemplate<String, Any> {
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
                })
    }
}
