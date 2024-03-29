package no.nav.syfo

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.opentable.db.postgres.embedded.EmbeddedPostgres
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.token.support.spring.test.MockOAuth2ServerAutoConfiguration
import no.nav.syfo.config.kafka.FunctionSerializer
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory
import javax.sql.DataSource

@Configuration
@Import(MockOAuth2ServerAutoConfiguration::class)
class LocalApplicationConfig {

    private var embeddedPostgres: EmbeddedPostgres = EmbeddedPostgres.builder().start()

    @Bean
    fun embeddedPostgres(): DataSource {
        return embeddedPostgres.postgresDatabase
    }

    @Bean
    fun kafkaTemplate(producerFactory: ProducerFactory<String, Any>): KafkaTemplate<String, Any> {
        return KafkaTemplate(producerFactory)
    }

    @Bean
    fun mockOAuthServer(@Value("\${mock.token.server.port}") mockTokenServerPort: Int): MockOAuth2Server {
        var server = MockOAuth2Server()
        server.start(mockTokenServerPort)
        return server
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
