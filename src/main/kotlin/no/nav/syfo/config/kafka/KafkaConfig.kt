package no.nav.syfo.config.kafka

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.config.SaslConfigs
import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory
import org.springframework.kafka.listener.ContainerProperties

@Profile("remote")
@Configuration
@EnableKafka
class KafkaConfig(
    @Value("\${srv.username}") private val username: String,
    @Value("\${srv.password}") private val password: String,
    @Value("\${javax.net.ssl.trustStore}") private val trustStore: String,
    @Value("\${javax.net.ssl.trustStorePassword}") private val trustStorePassword: String,
    @Value("\${app.name}") private val appName: String,
    @Value("\${kafka.env.name}") private val kafkaEnv: String,
    @Value("\${spring.kafka.bootstrap.servers}") private val bootstrapServers: String,
) {
    @Bean
    fun getCommonKafkaProps(): HashMap<String, Any> {
        val commonProps = HashMap<String, Any>().apply {
            put(
                CommonClientConfigs.SECURITY_PROTOCOL_CONFIG,
                "SASL_SSL"
            )
            put(
                CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG,
                bootstrapServers
            )
            put(
                SaslConfigs.SASL_MECHANISM,
                "PLAIN"
            )
            put(
                SaslConfigs.SASL_JAAS_CONFIG,
                "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"${username}\" password=\"${password}\";"
            )
            put(
                SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG,
                trustStore
            )
            put(
                SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG,
                trustStorePassword
            )
        }
        return commonProps
    }

    @Bean
    fun getConsumerKafkaProps(): HashMap<String, Any> {
        val commonConsumerProps = HashMap<String, Any>().apply {
            put(
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer::class.java
            )
            put(
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer::class.java
            )
            put(
                ConsumerConfig.GROUP_ID_CONFIG,
                "$appName-$kafkaEnv"
            )
            put(
                ConsumerConfig.MAX_POLL_RECORDS_CONFIG,
                1
            )
            put(
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                "earliest"
            )
            put(
                ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG,
                false
            )
            put(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                bootstrapServers
            )
        }
        return commonConsumerProps
    }

    @Bean
    fun getProduserKafkaProps(): HashMap<String, Any> {
        val commonProducerProps = HashMap<String, Any>().apply {
            put(
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                StringSerializer::class.java
            )
            put(
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                StringSerializer::class.java
            )
        }
        return commonProducerProps
    }

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
            getCommonKafkaProps() + getProduserKafkaProps(),
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

    @Bean
    fun consumerFactory(): ConsumerFactory<String, String> {
        return DefaultKafkaConsumerFactory(getCommonKafkaProps() + getConsumerKafkaProps())
    }

    @Bean
    fun kafkaListenerContainerFactory(): ConcurrentKafkaListenerContainerFactory<String, String> {
        return ConcurrentKafkaListenerContainerFactory<String, String>().apply {
            this.containerProperties.ackMode = ContainerProperties.AckMode.MANUAL_IMMEDIATE
            this.consumerFactory = consumerFactory()
        }
    }
}
