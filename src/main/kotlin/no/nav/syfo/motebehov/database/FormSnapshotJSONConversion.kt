package no.nav.syfo.motebehov.database

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.syfo.motebehov.formSnapshot.FormSnapshot
import org.slf4j.LoggerFactory

val objectMapper = jacksonObjectMapper()
val log = LoggerFactory.getLogger("FormSnapshotJSONConversion")

fun convertFormSnapshotToJson(formSnapshot: FormSnapshot): String {
    return objectMapper.writeValueAsString(formSnapshot)
}

fun convertJsonToFormSnapshot(json: String): FormSnapshot? {
    return try {
        objectMapper.readValue(json)
    } catch (e: JsonParseException) {
        log.warn("Failed to parse JSON: ${e.message}")
        null
    } catch (e: JsonMappingException) {
        log.warn("Failed to map JSON to FormSnapshot: ${e.message}")
        null
    } catch (e: JsonProcessingException) {
        log.warn("Something went wrong with processing JSON and mapping to FormSnapshot: ${e.message}")
        null
    }
}
