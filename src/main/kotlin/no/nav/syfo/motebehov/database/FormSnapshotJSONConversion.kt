package no.nav.syfo.motebehov.database

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
    } catch (e: Exception) {
        log.warn("Failed to convert JSON to FormSnapshot: ${'$'}{e.message}")
        null
    }

}
