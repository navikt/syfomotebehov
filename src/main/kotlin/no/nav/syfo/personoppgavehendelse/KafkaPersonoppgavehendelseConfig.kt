package no.nav.syfo.personoppgavehendelse

import javax.inject.Inject
import no.nav.syfo.config.kafka.KafkaAivenConfig
import no.nav.syfo.personoppgavehendelse.domain.KPersonoppgavehendelse
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.core.*

@EnableKafka
@Configuration
class KafkaPersonoppgavehendelseConfig @Inject constructor(
    private val kafkaAivenConfig: KafkaAivenConfig
) {
    @Bean("PersonoppgavehendelseProducerFactory")
    fun producerFactory(): ProducerFactory<String, KPersonoppgavehendelse> {
        return DefaultKafkaProducerFactory(
            kafkaAivenConfig.commonKafkaAivenProducerConfig(),
        )
    }

    @Bean("PersonoppgavehendelseTemplate")
    fun kafkaTemplate(@Qualifier("PersonoppgavehendelseProducerFactory") producerFactory: ProducerFactory<String, KPersonoppgavehendelse>): KafkaTemplate<String, KPersonoppgavehendelse> {
        return KafkaTemplate(producerFactory)
    }
}
