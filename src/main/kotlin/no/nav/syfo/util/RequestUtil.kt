package no.nav.syfo.util

import java.util.*

const val NAV_CONSUMER_ID_HEADER = "Nav-Consumer-Id"
const val APP_CONSUMER_ID = "syfomotebehov"
const val NAV_CALL_ID_HEADER = "Nav-Call-Id"

const val NAV_PERSONIDENT_HEADER = "Nav-Personident"

const val NAV_PERSONIDENTER_HEADER = "Nav-Personidenter"

const val NAV_CONSUMER_TOKEN_HEADER = "Nav-Consumer-Token"

// Lenke til relevant behandling i behandlingskatalogen:
// https://behandlingskatalog.nais.adeo.no/process/team/6a3b85e0-0e06-4f58-95bb-4318e31c4b2b/4fbbf6c1-345c-4b31-afdc-9258c401b230
const val BEHANDLINGSNUMMER_MOTEBEHOV = "B380"
const val PDL_BEHANDLINGSNUMMER_HEADER = "behandlingsnummer"

fun createCallId(): String = UUID.randomUUID().toString()

fun getOrCreateCallId(callId: String?): String = callId ?: UUID.randomUUID().toString()
