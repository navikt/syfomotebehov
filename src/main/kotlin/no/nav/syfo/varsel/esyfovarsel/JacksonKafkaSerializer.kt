package no.nav.syfo.varsel.esyfovarsel
import org.apache.kafka.common.serialization.Serializer
import tools.jackson.databind.DeserializationFeature
import tools.jackson.module.kotlin.jsonMapper

fun jacksonMapper() = jsonMapper {
    disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
}

class JacksonKafkaSerializer : Serializer<Any> {
    override fun serialize(topic: String?, data: Any?): ByteArray = jacksonMapper().writeValueAsBytes(data)
    override fun close() {}
}
