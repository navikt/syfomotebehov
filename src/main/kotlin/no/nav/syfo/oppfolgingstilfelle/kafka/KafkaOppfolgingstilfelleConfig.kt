package no.nav.syfo.oppfolgingstilfelle.kafka

import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.listener.ContainerProperties
import javax.inject.Inject

@Profile("remote")
@Configuration
class KafkaOppfolgingstilfelleConfig @Inject constructor() {

    @Bean
    fun consumerFactory(properties: KafkaProperties): ConsumerFactory<String, String> {
        return DefaultKafkaConsumerFactory(properties.buildConsumerProperties())
    }

    @Bean
    fun kafkaListenerContainerFactory(
            consumerFactory: ConsumerFactory<String, String>
    ): ConcurrentKafkaListenerContainerFactory<String, String> {
        return ConcurrentKafkaListenerContainerFactory<String, String>().apply {
            this.containerProperties.ackMode = ContainerProperties.AckMode.MANUAL_IMMEDIATE;
            this.consumerFactory = consumerFactory
        }
    }
}
