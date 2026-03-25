package no.nav.syfo

import no.nav.syfo.personoppgavehendelse.domain.KPersonoppgavehendelse
import no.nav.syfo.varsel.esyfovarsel.JacksonKafkaSerializer
import no.nav.syfo.varsel.esyfovarsel.domain.EsyfovarselHendelse
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory
import org.springframework.test.context.DynamicPropertyRegistrar
import org.testcontainers.kafka.KafkaContainer

@Profile("local")
@Configuration
class LocalKafkaContainer {
    @Bean
    fun kafkaContainer(): KafkaContainer = KafkaContainer("apache/kafka").also { it.start() }

    @Bean
    fun kafkaProperties(kafkaContainer: KafkaContainer) =
        DynamicPropertyRegistrar { registry ->
            registry.add("spring.kafka.bootstrap-servers") { kafkaContainer.bootstrapServers }
        }

    @Bean("EsyfovarselProducerFactory")
    fun esyfovarselProducerFactory(kafkaContainer: KafkaContainer): ProducerFactory<String, EsyfovarselHendelse> =
        DefaultKafkaProducerFactory(
            mapOf(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to kafkaContainer.bootstrapServers,
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to JacksonKafkaSerializer::class.java,
            ),
        )

    @Bean("EsyfovarselKafkaTemplate")
    fun esyfovarselKafkaTemplate(
        @Qualifier("EsyfovarselProducerFactory") producerFactory: ProducerFactory<String, EsyfovarselHendelse>,
    ): KafkaTemplate<String, EsyfovarselHendelse> = KafkaTemplate(producerFactory)

    @Bean("PersonoppgavehendelseProducerFactory")
    fun personoppgavehendelseProducerFactory(kafkaContainer: KafkaContainer): ProducerFactory<String, KPersonoppgavehendelse> =
        DefaultKafkaProducerFactory(
            mapOf(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to kafkaContainer.bootstrapServers,
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to JacksonKafkaSerializer::class.java,
            ),
        )

    @Bean("PersonoppgavehendelseTemplate")
    fun personoppgavehendelseTemplate(
        @Qualifier("PersonoppgavehendelseProducerFactory") producerFactory: ProducerFactory<String, KPersonoppgavehendelse>,
    ): KafkaTemplate<String, KPersonoppgavehendelse> = KafkaTemplate(producerFactory)
}
