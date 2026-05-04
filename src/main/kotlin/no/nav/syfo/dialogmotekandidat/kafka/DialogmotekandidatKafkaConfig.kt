package no.nav.syfo.dialogmotekandidat.kafka

import no.nav.syfo.config.kafka.KafkaAivenConfig
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.errors.SerializationException
import org.apache.kafka.common.serialization.StringDeserializer
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.listener.ContainerProperties
import org.springframework.kafka.listener.DefaultErrorHandler
import org.springframework.util.backoff.FixedBackOff
import javax.inject.Inject

@Profile("!local")
@EnableKafka
@Configuration
class DialogmotekandidatKafkaConfig
    @Inject
    constructor(
        private val kafkaAivenConfig: KafkaAivenConfig,
    ) {
        @Bean
        fun dialogmotekandidatConsumerFactory(): ConsumerFactory<String, KafkaDialogmotekandidatEndring> {
            fun kafkaDialogmotekandidatConsumerConfig(): HashMap<String, Any> =
                HashMap<String, Any>().apply {
                    put(
                        ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                        StringDeserializer::class.java.canonicalName,
                    )
                    put(
                        ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                        KafkaDialogmotekandidatDeserializer::class.java.canonicalName,
                    )
                }

            val factoryConfig =
                kafkaAivenConfig.commonKafkaAivenConfig() + kafkaAivenConfig.commonKafkaAivenConsumerConfig() +
                    kafkaDialogmotekandidatConsumerConfig()

            return DefaultKafkaConsumerFactory(
                factoryConfig,
            )
        }

        @Bean("DialogmotekandidatListenerContainerFactory")
        fun kafkaListenerContainerFactory(): ConcurrentKafkaListenerContainerFactory<String, KafkaDialogmotekandidatEndring> =
            ConcurrentKafkaListenerContainerFactory<String, KafkaDialogmotekandidatEndring>()
                .apply {
                    this.containerProperties.ackMode = ContainerProperties.AckMode.MANUAL_IMMEDIATE
                    this.setCommonErrorHandler(
                        DefaultErrorHandler(
                            { record, exception ->
                                log.error(
                                    "Gir opp prosessering av melding fra topic={}, partition={}, offset={}. Hopper over.",
                                    record.topic(),
                                    record.partition(),
                                    record.offset(),
                                    exception,
                                )
                            },
                            FixedBackOff(0L, 0L),
                        ).apply {
                            addNotRetryableExceptions(SerializationException::class.java)
                        },
                    )
                }.also {
                    it.setConsumerFactory(dialogmotekandidatConsumerFactory())
                }

        companion object {
            private val log = LoggerFactory.getLogger(DialogmotekandidatKafkaConfig::class.java)
        }
    }
