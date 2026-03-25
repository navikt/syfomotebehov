package no.nav.syfo.motebehov.formSnapshot

import org.slf4j.LoggerFactory
import tools.jackson.core.JsonParser
import tools.jackson.databind.DeserializationContext
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ValueDeserializer
import tools.jackson.module.kotlin.jacksonObjectMapper
import tools.jackson.module.kotlin.readValue

val log = LoggerFactory.getLogger("FormSnapshotJSONConversion")

val objectMapper = jacksonObjectMapper()

fun convertFormSnapshotToJsonString(formSnapshot: FormSnapshot): String {
    val value = objectMapper.writeValueAsString(formSnapshot)

    return value
}

fun convertJsonStringToFormSnapshot(json: String): FormSnapshot? =
    try {
        objectMapper.readValue(json)
    } catch (e: Exception) {
        error("Failed to parse FormSnapshot JSON: ${e.message}")
    }

class FieldSnapshotDeserializer : ValueDeserializer<FieldSnapshot>() {
    override fun deserialize(
        p: JsonParser,
        ctxt: DeserializationContext,
    ): FieldSnapshot {
        val node: JsonNode = p.readValueAsTree()

        return when (val fieldType = node.get("fieldType").asString()) {
            FormSnapshotFieldType.TEXT.name ->
                ctxt.readTreeAsValue(
                    node,
                    TextFieldSnapshot::class.java,
                )
            FormSnapshotFieldType.CHECKBOX_SINGLE.name ->
                ctxt.readTreeAsValue(
                    node,
                    SingleCheckboxFieldSnapshot::class.java,
                )
            FormSnapshotFieldType.RADIO_GROUP.name ->
                ctxt.readTreeAsValue(
                    node,
                    RadioGroupFieldSnapshot::class.java,
                )
            else -> throw IllegalArgumentException("Unknown field type: $fieldType")
        }
    }
}
