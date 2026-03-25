package no.nav.syfo.varsel.esyfovarsel

import no.nav.syfo.config.kafka.KafkaAivenConfig
import no.nav.syfo.varsel.esyfovarsel.domain.EsyfovarselHendelse
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
class EsyfovarselKafkaConfig(
    private val kafkaAivenConfig: KafkaAivenConfig,
) {
    @Bean("EsyfovarselProducerFactory")
    fun producerFactory(): ProducerFactory<String, EsyfovarselHendelse> =
        DefaultKafkaProducerFactory(
            kafkaAivenConfig.commonKafkaAivenProducerConfig(),
        )

    @Bean("EsyfovarselKafkaTemplate")
    fun kafkaTemplate(
        @Qualifier("EsyfovarselProducerFactory") producerFactory: ProducerFactory<String, EsyfovarselHendelse>,
    ): KafkaTemplate<String, EsyfovarselHendelse> = KafkaTemplate(producerFactory)
}
