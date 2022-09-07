package no.nav.syfo.util

import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestTemplate
import java.util.*

const val NAV_CONSUMER_ID_HEADER = "Nav-Consumer-Id"
const val APP_CONSUMER_ID = "syfomotebehov"
const val NAV_CALL_ID_HEADER = "Nav-Call-Id"

const val NAV_PERSONIDENT_HEADER = "Nav-Personident"

const val NAV_PERSONIDENTER_HEADER = "Nav-Personidenter"

const val NAV_CONSUMER_TOKEN_HEADER = "Nav-Consumer-Token"

const val TEMA_HEADER = "Tema"
const val ALLE_TEMA_HEADERVERDI = "GEN"

fun createCallId(): String = UUID.randomUUID().toString()

fun getOrCreateCallId(callId: String?): String = callId ?: UUID.randomUUID().toString()

fun <T> RestTemplate.getList(path: String, method: HttpMethod, entity: HttpEntity<String>?): List<T>? {
    val response: ResponseEntity<List<T>?> = this.exchange(
        path,
        method,
        entity,
        object : ParameterizedTypeReference<List<T>?>() {}
    )
    return response.body
}
