package no.nav.syfo.personoppgavehendelse

import no.nav.syfo.config.kafka.KafkaAivenConfig
import no.nav.syfo.personoppgavehendelse.domain.KPersonoppgavehendelse
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.core.*
import javax.inject.Inject

@Profile("!local")
@EnableKafka
@Configuration
class PersonoppgavehendelseKafkaConfig
    @Inject
    constructor(
        private val kafkaAivenConfig: KafkaAivenConfig,
    ) {
        @Bean("PersonoppgavehendelseProducerFactory")
        fun producerFactory(): ProducerFactory<String, KPersonoppgavehendelse> =
            DefaultKafkaProducerFactory(
                kafkaAivenConfig.commonKafkaAivenProducerConfig(),
            )

        @Bean("PersonoppgavehendelseTemplate")
        fun kafkaTemplate(
            @Qualifier("PersonoppgavehendelseProducerFactory") producerFactory: ProducerFactory<String, KPersonoppgavehendelse>,
        ): KafkaTemplate<String, KPersonoppgavehendelse> = KafkaTemplate(producerFactory)
    }
