package no.nav.syfo.util

import org.apache.commons.lang3.StringEscapeUtils
import org.owasp.html.HtmlPolicyBuilder
import org.slf4j.LoggerFactory
import java.sql.Timestamp
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.*

object DbUtil {
    private val LOG = LoggerFactory.getLogger(DbUtil::class.java)
    const val MOTEBEHOVSVAR_GYLDIGHET_DAGER = 78 * 7

    @JvmStatic
    fun convert(timestamp: Timestamp?): LocalDateTime? {
        return Optional.ofNullable(timestamp).map { obj: Timestamp -> obj.toLocalDateTime() }.orElse(null)
    }

    private val sanitizer = HtmlPolicyBuilder().toFactory()

    @JvmStatic
    fun sanitizeUserInput(userinput: String?): String {
        val sanitizedInput = StringEscapeUtils.unescapeHtml4(sanitizer.sanitize(StringEscapeUtils.unescapeHtml4(userinput)))
        if (sanitizedInput != userinput && userinput != null) {
            LOG.warn("""
    Dette er ikke en feil, men burde vært stoppet av regexen i frontend. Finn ut hvorfor og evt. oppdater regex. 
    Det ble strippet vekk innhold slik at denne teksten: {} 
    ble til denne teksten: {}
    """.trimIndent(), userinput, sanitizedInput)
        }
        return sanitizedInput
    }

    @JvmStatic
    fun hentTidligsteDatoForGyldigMotebehovSvar(): Timestamp? {
        return convert(LocalDateTime.of(LocalDate.now().minusDays(MOTEBEHOVSVAR_GYLDIGHET_DAGER.toLong()), LocalTime.MIN))
    }
}
