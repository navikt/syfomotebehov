package no.nav.syfo.varsel.budstikka

import no.nav.syfo.config.kafka.KafkaAivenConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory

@Profile("!local")
@EnableKafka
@Configuration
class BudstikkaKafkaConfig(
    private val kafkaAivenConfig: KafkaAivenConfig,
) {
    @Bean("BudstikkaProducerFactory")
    fun producerFactory(): ProducerFactory<String, String> =
        DefaultKafkaProducerFactory(
            kafkaAivenConfig.commonKafkaAivenProducerConfig().apply {
                put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java)
            },
        )

    @Bean("BudstikkaKafkaTemplate")
    fun kafkaTemplate(
        @Qualifier("BudstikkaProducerFactory") producerFactory: ProducerFactory<String, String>,
    ): KafkaTemplate<String, String> = KafkaTemplate(producerFactory)
}
