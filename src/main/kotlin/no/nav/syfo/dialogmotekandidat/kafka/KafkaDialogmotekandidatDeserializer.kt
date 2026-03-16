package no.nav.syfo.dialogmotekandidat.kafka

import org.apache.kafka.common.errors.SerializationException
import org.apache.kafka.common.serialization.Deserializer

class KafkaDialogmotekandidatDeserializer : Deserializer<KafkaDialogmotekandidatEndring> {

    private val objectMapper = configuredJacksonMapper()

    override fun deserialize(topic: String, data: ByteArray): KafkaDialogmotekandidatEndring {
        return try {
            objectMapper.readValue(data, KafkaDialogmotekandidatEndring::class.java)
        } catch (e: Exception) {
            throw SerializationException("Error when deserializing byte[] to KafkaDialogmotekandidatEndring")
        }
    }

    override fun close() {}
}
