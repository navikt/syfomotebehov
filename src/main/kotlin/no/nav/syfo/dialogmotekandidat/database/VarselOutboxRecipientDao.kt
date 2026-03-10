package no.nav.syfo.dialogmotekandidat.database

import no.nav.syfo.dialogmotekandidat.kafka.configuredJacksonMapper
import no.nav.syfo.util.convert
import no.nav.syfo.varsel.esyfovarsel.domain.EsyfovarselHendelse
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject

@Service
@Transactional
@Repository
class VarselOutboxRecipientDao @Inject constructor(
    private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate,
) {
    fun createRecipient(outboxUuid: UUID?, mottakerFnr: String, hendelse: EsyfovarselHendelse): UUID {
        val uuid = UUID.randomUUID()
        val now = LocalDateTime.now()
        namedParameterJdbcTemplate.update(
            """
            INSERT INTO VARSEL_OUTBOX_RECIPIENT (uuid, outbox_uuid, mottaker_fnr, payload, status, created_at, updated_at)
            VALUES (:uuid, :outboxUuid, :mottakerFnr, :payload, :status, :createdAt, :updatedAt)
            ON CONFLICT (outbox_uuid, mottaker_fnr) WHERE outbox_uuid IS NOT NULL DO NOTHING
            """.trimIndent(),
            MapSqlParameterSource()
                .addValue("uuid", uuid)
                .addValue("outboxUuid", outboxUuid)
                .addValue("mottakerFnr", mottakerFnr)
                .addValue("payload", jsonbOf(objectMapper.writeValueAsString(hendelse)))
                .addValue("status", VarselOutboxRecipientStatus.PENDING.name)
                .addValue("createdAt", convert(now))
                .addValue("updatedAt", convert(now)),
        )
        return uuid
    }

    fun getPending(): List<VarselOutboxRecipientEntry> =
        namedParameterJdbcTemplate.query(
            "SELECT * FROM VARSEL_OUTBOX_RECIPIENT WHERE status = 'PENDING' ORDER BY created_at ASC LIMIT 500 FOR UPDATE SKIP LOCKED",
            MapSqlParameterSource(),
            rowMapper,
        )

    fun updateStatus(uuid: UUID, status: VarselOutboxRecipientStatus) {
        namedParameterJdbcTemplate.update(
            "UPDATE VARSEL_OUTBOX_RECIPIENT SET status = :status, updated_at = :updatedAt WHERE uuid = :uuid",
            MapSqlParameterSource()
                .addValue("uuid", uuid)
                .addValue("status", status.name)
                .addValue("updatedAt", convert(LocalDateTime.now())),
        )
    }

    private val rowMapper = RowMapper { rs, _ ->
        VarselOutboxRecipientEntry(
            uuid = UUID.fromString(rs.getString("uuid")),
            outboxUuid = rs.getString("outbox_uuid")?.let { UUID.fromString(it) },
            mottakerFnr = rs.getString("mottaker_fnr"),
            hendelse = objectMapper.readValue(rs.getString("payload"), EsyfovarselHendelse::class.java),
            status = VarselOutboxRecipientStatus.valueOf(rs.getString("status")),
            createdAt = rs.getTimestamp("created_at").toLocalDateTime(),
            updatedAt = rs.getTimestamp("updated_at").toLocalDateTime(),
        )
    }

    companion object {
        private val objectMapper = configuredJacksonMapper().apply {
            registerSubtypes(
                no.nav.syfo.varsel.esyfovarsel.domain.NarmesteLederHendelse::class.java,
                no.nav.syfo.varsel.esyfovarsel.domain.ArbeidstakerHendelse::class.java,
            )
        }
    }
}
