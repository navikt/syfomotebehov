package no.nav.syfo.util

import tools.jackson.databind.DeserializationFeature
import tools.jackson.module.kotlin.jsonMapper

fun configuredJacksonMapper() =
    jsonMapper {
        disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    }
