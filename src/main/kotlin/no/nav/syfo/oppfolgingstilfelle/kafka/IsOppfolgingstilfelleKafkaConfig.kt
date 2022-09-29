package no.nav.syfo.oppfolgingstilfelle.kafka

import no.nav.syfo.config.kafka.KafkaAivenConfig
import no.nav.syfo.dialogmotekandidat.kafka.configuredJacksonMapper
import no.nav.syfo.oppfolgingstilfelle.kafka.domain.KafkaOppfolgingstilfellePerson
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.listener.ContainerProperties

import org.apache.kafka.common.errors.SerializationException
import org.apache.kafka.common.serialization.Deserializer
import org.springframework.beans.factory.annotation.Value

@EnableKafka
@Configuration
class KafkaIsOppfolgingstilfelleConfig(
    private val kafkaAivenConfig: KafkaAivenConfig,
    @Value("\${app.name}") private val appName: String,
    @Value("\${kafka.env.name}") private val kafkaEnv: String,
) {
    @Bean
    fun isOppfolgingtilfelleConsumerFactory(): ConsumerFactory<String, KafkaOppfolgingstilfellePerson> {

        fun kafkaIsOppfolgingtilfelleConsumerConfig(): HashMap<String, Any> {
            return HashMap<String, Any>().apply {
                put(
                    ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                    StringDeserializer::class.java.canonicalName
                )
                put(
                    ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                    KafkaIsOppfolgingstilfelleDeserializer::class.java.canonicalName
                )
                put(
                    ConsumerConfig.GROUP_ID_CONFIG,
                    "$appName-$kafkaEnv-isoppfolgingstilfelle"
                )
            }
        }

        val factoryConfig =
            kafkaAivenConfig.commonKafkaAivenConfig() + kafkaAivenConfig.commonKafkaAivenConsumerConfig() + kafkaIsOppfolgingtilfelleConsumerConfig()

        return DefaultKafkaConsumerFactory(
            factoryConfig,
        )
    }

    @Bean("IsOppfolgingstilfelleListenerContainerFactory")
    fun kafkaListenerContainerFactory(): ConcurrentKafkaListenerContainerFactory<String, KafkaOppfolgingstilfellePerson> {
        return ConcurrentKafkaListenerContainerFactory<String, KafkaOppfolgingstilfellePerson>().apply {
            this.containerProperties.ackMode = ContainerProperties.AckMode.MANUAL_IMMEDIATE
            this.consumerFactory = isOppfolgingtilfelleConsumerFactory()
        }
    }
}

class KafkaIsOppfolgingstilfelleDeserializer : Deserializer<KafkaOppfolgingstilfellePerson> {
    private val objectMapper = configuredJacksonMapper()

    override fun deserialize(topic: String, data: ByteArray): KafkaOppfolgingstilfellePerson {
        return try {
            objectMapper.readValue(data, KafkaOppfolgingstilfellePerson::class.java)
        } catch (e: Exception) {
            throw SerializationException("Error when deserializing byte[] to KafkaOppfolgingstilfellePerson")
        }
    }
    override fun close() {}
}
