package no.nav.syfo.dialogmotekandidat.kafka

import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.module.kotlin.jsonMapper
import com.fasterxml.jackson.module.kotlin.kotlinModule

fun configuredJsonMapper() = jsonMapper {
    addModule(kotlinModule())
    configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
}
