package no.nav.syfo.oppfolgingstilfelle.kafka

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.context.annotation.*
import org.springframework.core.io.FileUrlResource
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
        val strlist = kafkaBrokers.split(",")
        log.info("KAFKA_BROKERS_RAW: $kafkaBrokers | list: $strlist")
        properties.security.protocol = "SSL"
        properties.ssl.trustStoreType = "JAVA_STORE"
        properties.ssl.keyStoreType = "PKCS12"
        properties.ssl.trustStoreLocation = FileUrlResource("file://$truststorePath")
        properties.ssl.trustStorePassword = credstorePassword
        properties.ssl.keyStoreLocation = FileUrlResource("file://$keystorePath")
        properties.ssl.keyStorePassword = credstorePassword
        properties.ssl.keyPassword = credstorePassword
        properties.bootstrapServers = kafkaBrokers.split(",")

        return DefaultKafkaConsumerFactory(properties.buildConsumerProperties())
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
        private val log = LoggerFactory.getLogger(KafkaOppfolgingstilfelleConfig::class.java)
    }
}
