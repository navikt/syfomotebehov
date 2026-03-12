package no.nav.syfo.dialogmotekandidat.database

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
class DialogmotekandidatVarselStatusDao @Inject constructor(
    private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate,
) {
    fun create(
        kafkaMeldingUuid: String,
        fnr: String,
        type: DialogmotekandidatVarselType,
    ) {
        val query = """
            INSERT INTO $TABLE_NAME (
                $COLUMN_ID,
                $COLUMN_KAFKA_MELDING_UUID,
                $COLUMN_FNR,
                $COLUMN_TYPE,
                $COLUMN_STATUS,
                $COLUMN_RETRY_COUNT,
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
                NOW()
            )
            ON CONFLICT ($COLUMN_KAFKA_MELDING_UUID) DO NOTHING
        """.trimIndent()
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
    }

    fun updateStatusToSent(id: UUID) {
        val query = """
            UPDATE $TABLE_NAME
            SET $COLUMN_STATUS = :status,
                $COLUMN_UPDATED_AT = NOW()
            WHERE $COLUMN_ID = :id
        """.trimIndent()
        namedParameterJdbcTemplate.update(
            query,
            MapSqlParameterSource()
                .addValue("status", STATUS_SENT)
                .addValue("id", id),
        )
    }

    fun incrementRetryCount(id: UUID) {
        val query = """
            UPDATE $TABLE_NAME
            SET $COLUMN_RETRY_COUNT = $COLUMN_RETRY_COUNT + 1,
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
        val query = """
            -- Leader election ensures single-pod processing; no row-level locking needed
            SELECT *
            FROM $TABLE_NAME
            WHERE $COLUMN_STATUS = :status
              AND $COLUMN_TYPE = :type
            ORDER BY $COLUMN_CREATED_AT ASC
            LIMIT :limit
        """.trimIndent()
        return namedParameterJdbcTemplate.query(
            query,
            MapSqlParameterSource()
                .addValue("status", STATUS_PENDING)
                .addValue("type", type.name)
                .addValue("limit", limit),
            varselStatusRowMapper,
        )
    }

    fun countPendingOlderThan(type: DialogmotekandidatVarselType, cutoff: LocalDateTime): Int {
        val query = """
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
        val query = """
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
        val query = """
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

    private val varselStatusRowMapper = RowMapper { rs, _ ->
        DialogmotekandidatVarselStatus(
            id = rs.getObject(COLUMN_ID, UUID::class.java),
            kafkaMeldingUuid = rs.getString(COLUMN_KAFKA_MELDING_UUID),
            fnr = rs.getString(COLUMN_FNR),
            type = DialogmotekandidatVarselType.valueOf(rs.getString(COLUMN_TYPE)),
            status = rs.getString(COLUMN_STATUS),
            retryCount = rs.getInt(COLUMN_RETRY_COUNT),
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
        const val COLUMN_CREATED_AT = "created_at"
        const val COLUMN_UPDATED_AT = "updated_at"

        const val STATUS_PENDING = "PENDING"
        const val STATUS_SENT = "SENT"
    }
}
