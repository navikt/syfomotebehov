package no.nav.syfo.util

import org.slf4j.spi.LoggingEventBuilder
import org.springframework.core.codec.CodecException
import org.springframework.dao.DataAccessException
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.reactive.function.client.WebClientRequestException
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.net.ConnectException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.sql.SQLException
import java.util.Collections
import java.util.IdentityHashMap
import java.util.concurrent.CancellationException
import java.util.concurrent.TimeoutException
import javax.net.ssl.SSLException

enum class FailureKind(
    val value: String,
    val errorCode: String,
) {
    DNS("dns", "UPSTREAM_DNS_ERROR"),
    TIMEOUT("timeout", "UPSTREAM_TIMEOUT"),
    CONNECTION("connection", "UPSTREAM_CONNECTION_ERROR"),
    TLS("tls", "UPSTREAM_TLS_ERROR"),
    HTTP("http", "UPSTREAM_HTTP_ERROR"),
    INVALID_RESPONSE("invalid_response", "INVALID_UPSTREAM_RESPONSE"),
    UNKNOWN("unknown", "UNEXPECTED_ERROR"),
}

fun Throwable.failureKind(): FailureKind {
    val causes = causeChain()
    return when {
        causes.any { it is UnknownHostException } -> FailureKind.DNS
        causes.any {
            it is TimeoutException ||
                it is SocketTimeoutException ||
                it is org.apache.kafka.common.errors.TimeoutException ||
                it is io.netty.handler.timeout.TimeoutException
        } -> FailureKind.TIMEOUT
        causes.any { it is SSLException } -> FailureKind.TLS
        causes.any { it is ConnectException || it is SocketException } -> FailureKind.CONNECTION
        causes.any { it is RestClientResponseException || it is WebClientResponseException } -> FailureKind.HTTP
        causes.any { it is CodecException || it is com.fasterxml.jackson.core.JsonProcessingException } -> FailureKind.INVALID_RESPONSE
        else -> FailureKind.UNKNOWN
    }
}

fun Throwable.upstreamStatus(): Int? =
    causeChain().firstNotNullOfOrNull {
        when (it) {
            is RestClientResponseException -> it.statusCode.value()
            is WebClientResponseException -> it.statusCode.value()
            else -> null
        }
    }

fun Throwable.rethrowIfCancelled() {
    causeChain().firstOrNull { it is CancellationException || it is InterruptedException }?.let {
        if (it is InterruptedException) Thread.currentThread().interrupt()
        throw it
    }
}

// Keep code locations and bounded exception categories. HTTP exceptions and decoder
// messages may contain credentials, request URLs or personal data, even in causes.
fun LoggingEventBuilder.withFailureDiagnostics(cause: Throwable): LoggingEventBuilder {
    val chain = cause.causeChain()
    chain.filterIsInstance<SQLException>().firstNotNullOfOrNull { it.sqlState }?.let {
        addKeyValue("sql_state", it)
    }
    return addKeyValue("exception_type", cause.safeType())
        .addKeyValue("cause_type", chain.last().safeType())
        .addKeyValue(
            "stack_trace",
            chain.joinToString("\nCaused by: ") { error ->
                error.safeType() + error.stackTrace.take(30).joinToString("\n\tat ", prefix = "\n\tat ")
            },
        )
}

private fun Throwable.causeChain(): List<Throwable> {
    val seen = Collections.newSetFromMap(IdentityHashMap<Throwable, Boolean>())
    val result = mutableListOf<Throwable>()
    var current: Throwable? = this
    while (current != null && result.size < 16 && seen.add(current)) {
        result.add(current)
        current = current.cause
    }
    return result
}

private fun Throwable.safeType(): String =
    when (this) {
        is UnknownHostException -> "UnknownHostException"
        is SocketTimeoutException -> "SocketTimeoutException"
        is TimeoutException -> "TimeoutException"
        is SSLException -> "SSLException"
        is ConnectException -> "ConnectException"
        is SocketException -> "SocketException"
        is RestClientResponseException -> "RestClientResponseException"
        is WebClientResponseException -> "WebClientResponseException"
        is WebClientRequestException -> "WebClientRequestException"
        is CodecException -> "CodecException"
        is org.springframework.dao.DataIntegrityViolationException -> "DataIntegrityViolationException"
        is DataAccessException -> "DataAccessException"
        is java.sql.SQLException -> "SQLException"
        is org.apache.kafka.common.errors.TimeoutException -> "TimeoutException"
        is org.apache.kafka.common.errors.RecordTooLargeException -> "RecordTooLargeException"
        is org.apache.kafka.common.errors.SerializationException -> "SerializationException"
        is org.apache.kafka.common.errors.ProducerFencedException -> "ProducerFencedException"
        is org.apache.kafka.common.errors.NotEnoughReplicasException -> "NotEnoughReplicasException"
        is io.netty.handler.timeout.TimeoutException -> "TimeoutException"
        is org.apache.kafka.common.KafkaException -> "KafkaException"
        is com.fasterxml.jackson.core.JsonProcessingException -> "JsonProcessingException"
        is IllegalArgumentException -> "IllegalArgumentException"
        is IllegalStateException -> "IllegalStateException"
        is NullPointerException -> "NullPointerException"
        is java.io.IOException -> "IOException"
        else -> "UnknownException"
    }
