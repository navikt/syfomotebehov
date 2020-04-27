package no.nav.syfo.config.kafka

import org.apache.kafka.common.serialization.Serializer

class FunctionSerializer<T>(private val serializer: (t: T) -> ByteArray) : Serializer<T> {
    override fun configure(configs: Map<String?, *>, isKey: Boolean) {}

    override fun serialize(topic: String, t: T): ByteArray {
        return serializer(t)
    }

    override fun close() {}
}
