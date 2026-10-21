package no.nav.syfo.varsel.budstikka

import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

enum class MikrofrontendKommando {
    ENABLE,
    DISABLE,
}

enum class MikrofrontendUtsendingStatus {
    PENDING,
    SENT,
    SKIPPED,
}

data class MikrofrontendUtsending(
    val id: UUID,
    val eventId: UUID,
    val fnr: String,
    val kommando: MikrofrontendKommando,
    val retryCount: Int,
)

@Transactional
@Repository
class BudstikkaMikrofrontendDAO(
    private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate,
) {
    fun create(
        fnr: String,
        kommando: MikrofrontendKommando,
    ): UUID {
        val id = UUID.randomUUID()
        val query =
            """
            INSERT INTO $TABLE_NAME (id, event_id, fnr, kommando, status, retry_count, next_retry_at, created_at, updated_at)
            VALUES (:id, :eventId, :fnr, :kommando, :status, 0, NOW(), clock_timestamp(), NOW())
            """.trimIndent()
        namedParameterJdbcTemplate.update(
            query,
            MapSqlParameterSource()
                .addValue("id", id)
                .addValue("eventId", UUID.randomUUID())
                .addValue("fnr", fnr)
                .addValue("kommando", kommando.name)
                .addValue("status", MikrofrontendUtsendingStatus.PENDING.name),
        )
        return id
    }

    /**
     * Henter neste ventende kommando som er klar for sending. Budstikka lar siste kommando vinne per
     * person, så en kommando sendes ikke før eldre ventende kommandoer for samme person er sendt.
     */
    fun getNextPending(): MikrofrontendUtsending? {
        val query =
            """
            SELECT u.id, u.event_id, u.fnr, u.kommando, u.retry_count
            FROM $TABLE_NAME u
            WHERE u.status = :pending
              AND u.retry_count < :maxRetries
              AND u.next_retry_at <= NOW()
              AND NOT EXISTS (
                  SELECT 1
                  FROM $TABLE_NAME eldre
                  WHERE eldre.fnr = u.fnr
                    AND eldre.status = :pending
                    AND eldre.retry_count < :maxRetries
                    AND eldre.created_at < u.created_at
              )
            ORDER BY u.created_at ASC
            LIMIT 1
            FOR UPDATE SKIP LOCKED
            """.trimIndent()
        return namedParameterJdbcTemplate
            .query(
                query,
                MapSqlParameterSource()
                    .addValue("pending", MikrofrontendUtsendingStatus.PENDING.name)
                    .addValue("maxRetries", MAX_RETRY_COUNT),
                rowMapper,
            ).firstOrNull()
    }

    fun updateStatus(
        id: UUID,
        status: MikrofrontendUtsendingStatus,
    ) {
        namedParameterJdbcTemplate.update(
            "UPDATE $TABLE_NAME SET status = :status, updated_at = NOW() WHERE id = :id",
            MapSqlParameterSource()
                .addValue("status", status.name)
                .addValue("id", id),
        )
    }

    fun incrementRetryCount(id: UUID) {
        // Samme backoff som DIALOGKANDIDAT_VARSEL_STATUS: 1m, 2m, 4m, ... capped ved 720m.
        val query =
            """
            UPDATE $TABLE_NAME
            SET retry_count = retry_count + 1,
                next_retry_at = NOW() + LEAST(POWER(2, retry_count), 720) * interval '1 minute',
                updated_at = NOW()
            WHERE id = :id
            """.trimIndent()
        namedParameterJdbcTemplate.update(query, MapSqlParameterSource().addValue("id", id))
    }

    fun countGivenUp(): Int =
        namedParameterJdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM $TABLE_NAME WHERE status = :pending AND retry_count >= :maxRetries",
            MapSqlParameterSource()
                .addValue("pending", MikrofrontendUtsendingStatus.PENDING.name)
                .addValue("maxRetries", MAX_RETRY_COUNT),
            Int::class.java,
        ) ?: 0

    /** Ventende rader eldre enn grensen har gitt opp, fordi backoff-en er brukt opp etter noen dager. */
    fun deleteOlderThan(cutoff: LocalDateTime): Int =
        namedParameterJdbcTemplate.update(
            "DELETE FROM $TABLE_NAME WHERE created_at < :cutoff",
            MapSqlParameterSource().addValue("cutoff", cutoff),
        )

    private val rowMapper =
        RowMapper { rs, _ ->
            MikrofrontendUtsending(
                id = rs.getObject("id", UUID::class.java),
                eventId = rs.getObject("event_id", UUID::class.java),
                fnr = rs.getString("fnr"),
                kommando = MikrofrontendKommando.valueOf(rs.getString("kommando")),
                retryCount = rs.getInt("retry_count"),
            )
        }

    companion object {
        const val TABLE_NAME = "budstikka_mikrofrontend_utsending"
        const val MAX_RETRY_COUNT = 10
    }
}
