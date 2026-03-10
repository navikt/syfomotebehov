package no.nav.syfo.dialogmotekandidat.database

import no.nav.syfo.dialogmotekandidat.kafka.KafkaDialogmotekandidatEndring
import no.nav.syfo.dialogmotekandidat.kafka.configuredJacksonMapper
import no.nav.syfo.util.convert
import org.postgresql.util.PGobject
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
class VarselOutboxDao @Inject constructor(
    private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate,
) {
    fun createPending(endring: KafkaDialogmotekandidatEndring): UUID =
        create(endring, VarselOutboxStatus.PENDING)

    fun createSkipped(endring: KafkaDialogmotekandidatEndring): UUID =
        create(endring, VarselOutboxStatus.SKIPPED)

    private fun create(endring: KafkaDialogmotekandidatEndring, status: VarselOutboxStatus): UUID {
        val uuid = UUID.fromString(endring.uuid)
        val now = LocalDateTime.now()
        namedParameterJdbcTemplate.update(
            """
            INSERT INTO VARSEL_OUTBOX (uuid, kilde, payload, status, created_at, updated_at)
            VALUES (:uuid, :kilde, :payload, :status, :createdAt, :updatedAt)
            ON CONFLICT (uuid) DO NOTHING
            """.trimIndent(),
            MapSqlParameterSource()
                .addValue("uuid", uuid)
                .addValue("kilde", KILDE_DIALOGMOTEKANDIDAT_LISTENER)
                .addValue("payload", jsonbOf(objectMapper.writeValueAsString(endring)))
                .addValue("status", status.name)
                .addValue("createdAt", convert(now))
                .addValue("updatedAt", convert(now)),
        )
        return uuid
    }

    fun getPending(): List<VarselOutboxEntry> =
        namedParameterJdbcTemplate.query(
            "SELECT * FROM VARSEL_OUTBOX WHERE status = 'PENDING' ORDER BY created_at ASC LIMIT 500",
            MapSqlParameterSource(),
            rowMapper,
        )

    fun updateStatus(uuid: UUID, status: VarselOutboxStatus) {
        namedParameterJdbcTemplate.update(
            "UPDATE VARSEL_OUTBOX SET status = :status, updated_at = :updatedAt WHERE uuid = :uuid",
            MapSqlParameterSource()
                .addValue("uuid", uuid)
                .addValue("status", status.name)
                .addValue("updatedAt", convert(LocalDateTime.now())),
        )
    }

    private val rowMapper = RowMapper { rs, _ ->
        VarselOutboxEntry(
            uuid = UUID.fromString(rs.getString("uuid")),
            kilde = rs.getString("kilde"),
            payload = rs.getString("payload"),
            status = VarselOutboxStatus.valueOf(rs.getString("status")),
            createdAt = rs.getTimestamp("created_at").toLocalDateTime(),
            updatedAt = rs.getTimestamp("updated_at").toLocalDateTime(),
        )
    }

    companion object {
        const val KILDE_DIALOGMOTEKANDIDAT_LISTENER = "DIALOGMOTEKANDIDAT_LISTENER"
        private val objectMapper = configuredJacksonMapper()
    }
}

internal fun jsonbOf(json: String) = PGobject().apply {
    type = "jsonb"
    value = json
}
