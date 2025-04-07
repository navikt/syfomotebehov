package no.nav.syfo.motebehov.formSnapshot

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.slf4j.LoggerFactory

val log = LoggerFactory.getLogger("FormSnapshotJSONConversion")

val objectMapper = jacksonObjectMapper()

fun convertFormSnapshotToJsonString(formSnapshot: FormSnapshot): String {
    val value = objectMapper.writeValueAsString(formSnapshot)

    return value
}

fun convertJsonStringToFormSnapshot(json: String): FormSnapshot? {
    return try {
        objectMapper.readValue(json)
    } catch (e: JsonParseException) {
        log.error("Failed to parse JSON: ${e.message}")
        null
    } catch (e: JsonMappingException) {
        log.error("Failed to map JSON to FormSnapshot: ${e.message}")
        null
    } catch (e: JsonProcessingException) {
        log.error("Something went wrong with processing JSON and mapping to FormSnapshot: ${e.message}")
        null
    }
}

class FieldSnapshotDeserializer : JsonDeserializer<FieldSnapshot>() {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): FieldSnapshot {
        val node: JsonNode = p.codec.readTree(p)
        val fieldType = node.get("fieldType").asText()

        return when (fieldType) {
            FormSnapshotFieldType.TEXT.name -> objectMapper.treeToValue(
                node,
                TextFieldSnapshot::class.java
            )
            FormSnapshotFieldType.CHECKBOX_SINGLE.name -> objectMapper.treeToValue(
                node,
                SingleCheckboxFieldSnapshot::class.java
            )
            FormSnapshotFieldType.RADIO_GROUP.name -> objectMapper.treeToValue(
                node,
                RadioGroupFieldSnapshot::class.java
            )
            else -> throw IllegalArgumentException("Unknown field type: $fieldType")
        }
    }
}
