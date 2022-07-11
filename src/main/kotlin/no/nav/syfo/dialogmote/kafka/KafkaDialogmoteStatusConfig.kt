package no.nav.syfo.dialogmote.kafka

import io.confluent.kafka.serializers.KafkaAvroDeserializer
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig
import no.nav.syfo.dialogmote.avro.KDialogmoteStatusEndring
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.config.SaslConfigs
import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.common.serialization.StringDeserializer
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.listener.ContainerProperties

@EnableKafka
@Configuration
class KafkaDialogmoteStatusConfig(
    @Value("\${kafka.brokers}") private val aivenBrokers: String,
    @Value("\${kafka.truststore.path}") private val aivenTruststoreLocation: String,
    @Value("\${kafka.keystore.path}") private val aivenKeystoreLocation: String,
    @Value("\${kafka.credstore.password}") private val aivenCredstorePassword: String,
    @Value("\${app.name}") private val appName: String,
    @Value("\${kafka.env.name}") private val kafkaEnv: String,
    @Value("\${kafka.schema.registry.user}") private val aivenRegistryUser: String,
    @Value("\${kafka.schema.registry.password}") private val aivenRegistryPassword: String,
    @Value("\${kafka.schema.registry}") private val aivenSchemaRegistryUrl: String,
) {
    @Bean
    fun dialogmoteStatusConsumerFactory(): ConsumerFactory<String, KDialogmoteStatusEndring> {
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
                    "$appName-isdialogmote-dialogmote-statusendring-$kafkaEnv-group"
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

        fun kafkaDialogmoteStatusEndringConsumerConfig(): HashMap<String, Any> {
            return HashMap<String, Any>().apply {
                put(
                    ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                    StringDeserializer::class.java.canonicalName
                )
                put(
                    ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                    KafkaAvroDeserializer::class.java.canonicalName
                )
                put(
                    KafkaAvroDeserializerConfig.SCHEMA_REGISTRY_URL_CONFIG,
                    aivenSchemaRegistryUrl
                )
                put(
                    KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG,
                    true
                )
                put(
                    KafkaAvroDeserializerConfig.USER_INFO_CONFIG,
                    "$aivenRegistryUser:$aivenRegistryPassword"
                )
                put(
                    KafkaAvroDeserializerConfig.BASIC_AUTH_CREDENTIALS_SOURCE,
                    "USER_INFO"
                )
            }
        }

        val factoryConfig =
            commonKafkaAivenConfig() + commonKafkaAivenConsumerConfig() + kafkaDialogmoteStatusEndringConsumerConfig()

        return DefaultKafkaConsumerFactory(
            factoryConfig,
        )
    }

    @Bean("DialogmoteListenerContainerFactory")
    fun kafkaListenerContainerFactory(): ConcurrentKafkaListenerContainerFactory<String, KDialogmoteStatusEndring> {
        LOG.warn("aivenSchemaRegistryUrl: $aivenSchemaRegistryUrl")
        LOG.warn("aivenRegistryUser: $aivenRegistryUser")
        LOG.warn("aivenRegistryPassword: $aivenRegistryPassword")
        LOG.warn("aivenKeystoreLocation: $aivenKeystoreLocation")
        LOG.warn("aivenTruststoreLocation: $aivenTruststoreLocation")
        LOG.warn("aivenCredstorePassword: $aivenCredstorePassword")
        return ConcurrentKafkaListenerContainerFactory<String, KDialogmoteStatusEndring>().apply {
            this.containerProperties.ackMode = ContainerProperties.AckMode.MANUAL_IMMEDIATE
            this.consumerFactory = dialogmoteStatusConsumerFactory()
        }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(KafkaDialogmoteStatusConfig::class.java)
    }
}
