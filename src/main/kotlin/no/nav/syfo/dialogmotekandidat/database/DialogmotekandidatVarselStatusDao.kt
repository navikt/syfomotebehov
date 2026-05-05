package no.nav.syfo.dialogmotekandidat.database

import net.logstash.logback.argument.StructuredArguments.kv
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject

@Transactional
@Repository
class DialogmotekandidatVarselStatusDao
    @Inject
    constructor(
        private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate,
    ) {
        fun create(
            kafkaMeldingUuid: String,
            fnr: String,
            type: DialogmotekandidatVarselType,
        ) {
            val query =
                """
                INSERT INTO $TABLE_NAME (
                    $COLUMN_ID,
                    $COLUMN_KAFKA_MELDING_UUID,
                    $COLUMN_FNR,
                    $COLUMN_TYPE,
                    $COLUMN_STATUS,
                    $COLUMN_RETRY_COUNT,
                    $COLUMN_NEXT_RETRY_AT,
                    $COLUMN_CREATED_AT,
                    $COLUMN_UPDATED_AT
                ) VALUES (
                    :id,
                    :kafkaMeldingUuid,
                    :fnr,
                    :type,
                    :status,
                    :retryCount,
                    NOW(),
                    NOW(),
                    NOW()
                )
                ON CONFLICT ($COLUMN_KAFKA_MELDING_UUID) DO NOTHING
                """.trimIndent()
            val insertedRows =
                namedParameterJdbcTemplate.update(
                    query,
                    MapSqlParameterSource()
                        .addValue("id", UUID.randomUUID())
                        .addValue("kafkaMeldingUuid", kafkaMeldingUuid)
                        .addValue("fnr", fnr)
                        .addValue("type", type.name)
                        .addValue("status", STATUS_PENDING)
                        .addValue("retryCount", 0),
                )
            if (insertedRows == 0) {
                log.info(
                    "Ignorerte duplikat outbox-rad",
                    kv("event", "dialogmotekandidat.outbox.duplicate_ignored"),
                    kv("kafkaMeldingUuid", kafkaMeldingUuid),
                )
            }
        }

        fun updateStatusToSent(id: UUID): Boolean {
            val query =
                """
                UPDATE $TABLE_NAME
                SET $COLUMN_STATUS = :status,
                    $COLUMN_UPDATED_AT = NOW()
                WHERE $COLUMN_ID = :id
                """.trimIndent()
            val updatedRows =
                namedParameterJdbcTemplate.update(
                    query,
                    MapSqlParameterSource()
                        .addValue("status", STATUS_SENT)
                        .addValue("id", id),
                )
            if (updatedRows == 0) {
                log.warn(
                    "Fant ingen rad å markere som sendt",
                    kv("event", "dialogmotekandidat.varsel_status.update_missing"),
                    kv("id", id),
                )
            }
            return updatedRows > 0
        }

        fun incrementRetryCount(id: UUID) {
            // NB: POWER(2, retry_count) bruker gammel verdi (før increment i samme UPDATE).
            // Backoff-sekvensen blir: 1m, 2m, 4m, 8m, ... 512m (capped ved 720m).
            val query =
                """
                UPDATE $TABLE_NAME
                SET $COLUMN_RETRY_COUNT = $COLUMN_RETRY_COUNT + 1,
                    $COLUMN_NEXT_RETRY_AT = NOW() + LEAST(POWER(2, $COLUMN_RETRY_COUNT), 720) * interval '1 minute',
                    $COLUMN_UPDATED_AT = NOW()
                WHERE $COLUMN_ID = :id
                """.trimIndent()
            namedParameterJdbcTemplate.update(
                query,
                MapSqlParameterSource()
                    .addValue("id", id),
            )
        }

        fun getPendingByType(
            type: DialogmotekandidatVarselType,
            limit: Int = 50,
        ): List<DialogmotekandidatVarselStatus> {
            val query =
                """
                SELECT
                    $COLUMN_ID,
                    $COLUMN_KAFKA_MELDING_UUID,
                    $COLUMN_FNR,
                    $COLUMN_TYPE,
                    $COLUMN_STATUS,
                    $COLUMN_RETRY_COUNT,
                    $COLUMN_NEXT_RETRY_AT,
                    $COLUMN_CREATED_AT,
                    $COLUMN_UPDATED_AT
                FROM $TABLE_NAME
                WHERE $COLUMN_STATUS = :status
                  AND $COLUMN_TYPE = :type
                  AND $COLUMN_RETRY_COUNT < :maxRetries
                  AND $COLUMN_NEXT_RETRY_AT <= NOW()
                ORDER BY $COLUMN_CREATED_AT ASC
                LIMIT :limit
                FOR UPDATE SKIP LOCKED
                """.trimIndent()
            return namedParameterJdbcTemplate.query(
                query,
                MapSqlParameterSource()
                    .addValue("status", STATUS_PENDING)
                    .addValue("type", type.name)
                    .addValue("maxRetries", MAX_RETRY_COUNT)
                    .addValue("limit", limit),
                varselStatusRowMapper,
            )
        }

        fun hasPendingFerdigstillForFnr(fnr: String): Boolean {
            val query =
                """
                SELECT EXISTS(
                    SELECT 1
                    FROM $TABLE_NAME
                    WHERE $COLUMN_FNR = :fnr
                      AND $COLUMN_TYPE = :type
                      AND $COLUMN_STATUS = :status
                )
                """.trimIndent()
            return namedParameterJdbcTemplate.queryForObject(
                query,
                MapSqlParameterSource()
                    .addValue("fnr", fnr)
                    .addValue("type", DialogmotekandidatVarselType.FERDIGSTILL.name)
                    .addValue("status", STATUS_PENDING),
                Boolean::class.java,
            ) ?: false
        }

        fun countGivenUp(): Int {
            val query =
                """
                SELECT COUNT(*)
                FROM $TABLE_NAME
                WHERE $COLUMN_STATUS = :status
                  AND $COLUMN_RETRY_COUNT >= :maxRetries
                """.trimIndent()
            return namedParameterJdbcTemplate.queryForObject(
                query,
                MapSqlParameterSource()
                    .addValue("status", STATUS_PENDING)
                    .addValue("maxRetries", MAX_RETRY_COUNT),
                Int::class.java,
            ) ?: 0
        }

        fun countPendingOlderThan(
            type: DialogmotekandidatVarselType,
            cutoff: LocalDateTime,
        ): Int {
            val query =
                """
                SELECT COUNT(*)
                FROM $TABLE_NAME
                WHERE $COLUMN_STATUS = :status
                  AND $COLUMN_TYPE = :type
                  AND $COLUMN_CREATED_AT < :cutoff
                """.trimIndent()
            return namedParameterJdbcTemplate.queryForObject(
                query,
                MapSqlParameterSource()
                    .addValue("status", STATUS_PENDING)
                    .addValue("type", type.name)
                    .addValue("cutoff", cutoff),
                Int::class.java,
            ) ?: 0
        }

        fun deleteSentOlderThan(cutoff: LocalDateTime): Int {
            val query =
                """
                DELETE FROM $TABLE_NAME
                WHERE $COLUMN_STATUS = :status
                  AND $COLUMN_UPDATED_AT < :cutoff
                """.trimIndent()
            return namedParameterJdbcTemplate.update(
                query,
                MapSqlParameterSource()
                    .addValue("status", STATUS_SENT)
                    .addValue("cutoff", cutoff),
            )
        }

        fun deletePendingOlderThan(cutoff: LocalDateTime): Int {
            val query =
                """
                DELETE FROM $TABLE_NAME
                WHERE $COLUMN_STATUS = :status
                  AND $COLUMN_CREATED_AT < :cutoff
                """.trimIndent()
            return namedParameterJdbcTemplate.update(
                query,
                MapSqlParameterSource()
                    .addValue("status", STATUS_PENDING)
                    .addValue("cutoff", cutoff),
            )
        }

        private val varselStatusRowMapper =
            RowMapper { rs, _ ->
                DialogmotekandidatVarselStatus(
                    id = rs.getObject(COLUMN_ID, UUID::class.java),
                    kafkaMeldingUuid = rs.getString(COLUMN_KAFKA_MELDING_UUID),
                    fnr = rs.getString(COLUMN_FNR),
                    type = DialogmotekandidatVarselType.valueOf(rs.getString(COLUMN_TYPE)),
                    status = rs.getString(COLUMN_STATUS),
                    retryCount = rs.getInt(COLUMN_RETRY_COUNT),
                    nextRetryAt = rs.getTimestamp(COLUMN_NEXT_RETRY_AT).toLocalDateTime(),
                    createdAt = rs.getTimestamp(COLUMN_CREATED_AT).toLocalDateTime(),
                    updatedAt = rs.getTimestamp(COLUMN_UPDATED_AT).toLocalDateTime(),
                )
            }

        companion object {
            const val TABLE_NAME = "dialogkandidat_varsel_status"

            const val COLUMN_ID = "id"
            const val COLUMN_KAFKA_MELDING_UUID = "kafka_melding_uuid"
            const val COLUMN_FNR = "fnr"
            const val COLUMN_TYPE = "type"
            const val COLUMN_STATUS = "status"
            const val COLUMN_RETRY_COUNT = "retry_count"
            const val COLUMN_NEXT_RETRY_AT = "next_retry_at"
            const val COLUMN_CREATED_AT = "created_at"
            const val COLUMN_UPDATED_AT = "updated_at"

            const val STATUS_PENDING = "PENDING"
            const val STATUS_SENT = "SENT"
            const val MAX_RETRY_COUNT = 10

            private val log = LoggerFactory.getLogger(DialogmotekandidatVarselStatusDao::class.java)
        }
    }
