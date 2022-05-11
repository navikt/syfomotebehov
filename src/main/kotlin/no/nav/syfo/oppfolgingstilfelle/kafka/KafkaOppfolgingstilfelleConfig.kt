package no.nav.syfo.oppfolgingstilfelle.kafka

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.context.annotation.*
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.listener.ContainerProperties

@Profile("remote")
@Configuration
class KafkaOppfolgingstilfelleConfig(
    @Value("\${kafka.brokers}") val kafkaBrokers: String,
    @Value("\${kafka.truststore.path}") val truststorePath: String,
    @Value("\${kafka.keystore.path}") val keystorePath: String,
    @Value("\${kafka.credstore.password}") val credstorePassword: String
) {
    @Bean
    fun oppfolgingstilfconsumerFactory(properties: KafkaProperties): ConsumerFactory<String, String> {
        properties.ssl.trustStoreType = "JKS"
        properties.ssl.keyStoreType = "PKCS12"
        properties.ssl.trustStorePassword = credstorePassword
        properties.ssl.keyStorePassword = credstorePassword
        properties.ssl.keyPassword = credstorePassword
        properties.bootstrapServers = kafkaBrokers.split(",")

        val buildConsumerProps = properties.buildConsumerProperties().apply {
            put(SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG, "")
            put(SECURITY_PROTOCOL_CONFIG, "SSL")
            put(SSL_TRUSTSTORE_LOCATION_CONFIG, truststorePath)
            put(SSL_KEYSTORE_LOCATION_CONFIG, keystorePath)
        }

        return DefaultKafkaConsumerFactory(buildConsumerProps)
    }
    @Bean
    fun oppfolgingstilfelleKafkaListenerContainerFactory(
        oppfolgingstilfconsumerFactory: ConsumerFactory<String, String>
    ): ConcurrentKafkaListenerContainerFactory<String, String> {
        return ConcurrentKafkaListenerContainerFactory<String, String>().apply {
            this.containerProperties.ackMode = ContainerProperties.AckMode.MANUAL_IMMEDIATE
            this.consumerFactory = oppfolgingstilfconsumerFactory
        }
    }
    companion object {
        private val SSL_TRUSTSTORE_LOCATION_CONFIG = "ssl.truststore.location"
        private val SSL_KEYSTORE_LOCATION_CONFIG = "ssl.keystore.location"
        private val SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG = "ssl.endpoint.identification.algorithm"
        private val SECURITY_PROTOCOL_CONFIG = "security.protocol"
        private val log = LoggerFactory.getLogger(KafkaOppfolgingstilfelleConfig::class.java)
    }
}
