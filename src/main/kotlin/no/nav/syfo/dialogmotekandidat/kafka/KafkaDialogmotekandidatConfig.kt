package no.nav.syfo.dialogmotekandidat.kafka

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
import javax.inject.Inject

@EnableKafka
@Configuration
class KafkaDialogmotekandidatConfig @Inject constructor(
    private val kafkaAivenConfig: KafkaAivenConfig
) {
    @Bean
    fun dialogmotekandidatConsumerFactory(): ConsumerFactory<String, KafkaDialogmotekandidatEndring> {

        fun kafkaDialogmotekandidatConsumerConfig(): HashMap<String, Any> {
            return HashMap<String, Any>().apply {
                put(
                    ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                    StringDeserializer::class.java.canonicalName
                )
                put(
                    ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                    KafkaDialogmotekandidatDeserializer::class.java.canonicalName
                )
            }
        }

        val factoryConfig =
            kafkaAivenConfig.commonKafkaAivenConfig() + kafkaAivenConfig.commonKafkaAivenConsumerConfig() + kafkaDialogmotekandidatConsumerConfig()

        return DefaultKafkaConsumerFactory(
            factoryConfig,
        )
    }

    @Bean("DialogmotekandidatListenerContainerFactory")
    fun kafkaListenerContainerFactory(): ConcurrentKafkaListenerContainerFactory<String, KafkaDialogmotekandidatEndring> {
        return ConcurrentKafkaListenerContainerFactory<String, KafkaDialogmotekandidatEndring>().apply {
            this.containerProperties.ackMode = ContainerProperties.AckMode.MANUAL_IMMEDIATE
            this.consumerFactory = dialogmotekandidatConsumerFactory()
        }
    }
}
