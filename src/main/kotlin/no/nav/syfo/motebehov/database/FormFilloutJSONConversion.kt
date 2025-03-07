package no.nav.syfo.motebehov.database

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.syfo.motebehov.formFillout.FormFillout

val objectMapper = jacksonObjectMapper()

fun convertFormFilloutToJson(formFillout: FormFillout): String {
    return objectMapper.writeValueAsString(formFillout)
}

fun convertJsonToFormFillout(json: String): FormFillout {
    return objectMapper.readValue(json)
}
