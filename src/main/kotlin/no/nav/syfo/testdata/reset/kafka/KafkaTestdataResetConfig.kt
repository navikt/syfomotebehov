package no.nav.syfo.oppfolgingstilfelle.kafka

import no.nav.syfo.config.kafka.KafkaAivenConfig
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.listener.ContainerProperties

import org.springframework.beans.factory.annotation.Value

@EnableKafka
@Configuration
class KafkaTestdataResetConfig(
    private val kafkaAivenConfig: KafkaAivenConfig,
    @Value("\${app.name}") private val appName: String,
    @Value("\${kafka.env.name}") private val kafkaEnv: String,
) {
    @Bean
    fun testdataResetConsumerFactory(): ConsumerFactory<String, String> {
        fun kafkaTestdataResetConsumerConfig(): HashMap<String, Any> {
            return HashMap<String, Any>().apply {
                put(
                    ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                    StringDeserializer::class.java.canonicalName,
                )
                put(
                    ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                    StringDeserializer::class.java.canonicalName,
                )
                put(
                    ConsumerConfig.GROUP_ID_CONFIG,
                    "$appName-$kafkaEnv-testdata-reset",
                )
            }
        }

        val factoryConfig =
            kafkaAivenConfig.commonKafkaAivenConfig() + kafkaAivenConfig.commonKafkaAivenConsumerConfig() + kafkaTestdataResetConsumerConfig()

        return DefaultKafkaConsumerFactory(
            factoryConfig,
        )
    }

    @Bean("TestdataResetListenerContainerFactory")
    fun kafkaListenerContainerFactory(): ConcurrentKafkaListenerContainerFactory<String, String> {
        return ConcurrentKafkaListenerContainerFactory<String, String>().apply {
            this.containerProperties.ackMode = ContainerProperties.AckMode.MANUAL_IMMEDIATE
            this.consumerFactory = testdataResetConsumerFactory()
        }
    }
}
