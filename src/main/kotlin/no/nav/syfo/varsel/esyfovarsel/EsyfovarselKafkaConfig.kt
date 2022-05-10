package no.nav.syfo.varsel.esyfovarsel

import no.nav.syfo.varsel.esyfovarsel.domain.EsyfovarselHendelse
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.config.SaslConfigs
import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory

@EnableKafka
@Configuration
class EsyfovarselKafkaConfig(
    @Value("\${kafka.brokers}") private val aivenBrokers: String,
    @Value("\${kafka.truststore.path}") private val truststorePath: String,
    @Value("\${kafka.keystore.path}") private val keystorePath: String,
    @Value("\${kafka.credstore.password}") private val credstorePassword: String,
) {
    private val JAVA_KEYSTORE = "JKS"
    private val PKCS12 = "PKCS12"
    private val SSL = "SSL"
    private val USER_INFO = "USER_INFO"
    private val BASIC_AUTH_CREDENTIALS_SOURCE = "basic.auth.credentials.source"

    @Bean("EsyfovarselProducerFactory")
    fun producerFactory(): ProducerFactory<String, EsyfovarselHendelse> {

        val producerProperties = HashMap<String, Any>().apply {
            put(ProducerConfig.ACKS_CONFIG, "all")
            put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, SSL)
            put(SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG, "")
            put(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG, JAVA_KEYSTORE)
            put(SslConfigs.SSL_KEYSTORE_TYPE_CONFIG, PKCS12)
            put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, truststorePath)
            put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, credstorePassword)
            put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, keystorePath)
            put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, credstorePassword)
            put(SslConfigs.SSL_KEY_PASSWORD_CONFIG, credstorePassword)
            put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, aivenBrokers)

            put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java)
            put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JacksonKafkaSerializer::class.java)
            put(BASIC_AUTH_CREDENTIALS_SOURCE, USER_INFO)

            remove(SaslConfigs.SASL_MECHANISM)
            remove(SaslConfigs.SASL_JAAS_CONFIG)
            remove(SaslConfigs.SASL_MECHANISM)
        }
        return DefaultKafkaProducerFactory(producerProperties)
    }

    @Bean("EsyfovarselKafkaTemplate")
    fun kafkaTemplate(@Qualifier("EsyfovarselProducerFactory") producerFactory: ProducerFactory<String, EsyfovarselHendelse>): KafkaTemplate<String, EsyfovarselHendelse> {
        return KafkaTemplate(producerFactory)
    }
}
