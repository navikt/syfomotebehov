package no.nav.syfo.testhelper

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.OutputStreamAppender
import net.logstash.logback.encoder.LogstashEncoder
import org.slf4j.LoggerFactory
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import java.io.ByteArrayOutputStream

fun captureApplicationLogs(action: () -> Unit): List<JsonNode> {
    val logger = LoggerFactory.getLogger("no.nav.syfo") as Logger
    val output = ByteArrayOutputStream()
    val encoder =
        LogstashEncoder().apply {
            context = logger.loggerContext
            start()
        }
    val appender =
        OutputStreamAppender<ILoggingEvent>().apply {
            context = logger.loggerContext
            this.encoder = encoder
            outputStream = output
            start()
        }
    logger.addAppender(appender)
    try {
        action()
    } finally {
        logger.detachAppender(appender)
        appender.stop()
        encoder.stop()
    }
    val mapper = ObjectMapper()
    return output
        .toString(Charsets.UTF_8)
        .lineSequence()
        .filter { it.isNotBlank() }
        .map(mapper::readTree)
        .toList()
}
