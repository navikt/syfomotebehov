package no.nav.syfo.config.kafka

import no.nav.syfo.varsel.esyfovarsel.JacksonKafkaSerializer
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.config.SaslConfigs
import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.EnableKafka

@Configuration
@EnableKafka
class KafkaAivenConfig(
    @Value("\${kafka.brokers}") private val aivenBrokers: String,
    @Value("\${kafka.truststore.path}") private val aivenTruststoreLocation: String,
    @Value("\${kafka.keystore.path}") private val aivenKeystoreLocation: String,
    @Value("\${kafka.credstore.password}") private val aivenCredstorePassword: String,
    @Value("\${app.name}") private val appName: String,
    @Value("\${kafka.env.name}") private val kafkaEnv: String,
) {

    fun commonKafkaAivenConfig(): HashMap<String, Any> {
        return HashMap<String, Any>().apply {
            put(
                SaslConfigs.SASL_MECHANISM,
                "PLAIN"
            )
            put(
                CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG,
                aivenBrokers
            )
            put(
                CommonClientConfigs.SECURITY_PROTOCOL_CONFIG,
                "SSL"
            )
            put(
                SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG,
                ""
            )
            put(
                SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG,
                "jks"
            )
            put(
                SslConfigs.SSL_KEYSTORE_TYPE_CONFIG,
                "PKCS12"
            )
            put(
                SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG,
                aivenTruststoreLocation
            )
            put(
                SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG,
                aivenCredstorePassword
            )
            put(
                SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG,
                aivenKeystoreLocation
            )
            put(
                SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG,
                aivenCredstorePassword
            )
            put(
                SslConfigs.SSL_KEY_PASSWORD_CONFIG,
                aivenCredstorePassword
            )
        }
    }

    fun commonKafkaAivenConsumerConfig(): HashMap<String, Any> {
        return HashMap<String, Any>().apply {
            put(
                ConsumerConfig.GROUP_ID_CONFIG,
                "$appName-$kafkaEnv"
            )
            put(
                ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG,
                "false"
            )
            put(
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                "earliest"
            )
            put(
                ConsumerConfig.MAX_POLL_RECORDS_CONFIG,
                "1"
            )
            put(
                ConsumerConfig.MAX_PARTITION_FETCH_BYTES_CONFIG,
                "" + (10 * 1024 * 1024)
            )
        }
    }

    fun commonKafkaAivenProducerConfig(): HashMap<String, Any> {
        return HashMap<String, Any>().apply {
            putAll(commonKafkaAivenConfig())
            put(
                ProducerConfig.ACKS_CONFIG,
                "all"
            )
            put(
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                StringSerializer::class.java
            )
            put(
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                JacksonKafkaSerializer::class.java
            )
        }
    }
}
