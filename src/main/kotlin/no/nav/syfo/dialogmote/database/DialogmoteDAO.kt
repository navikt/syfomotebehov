package no.nav.syfo.dialogmote.database

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import javax.inject.Inject
import no.nav.syfo.consumer.aktorregister.domain.Fodselsnummer
import no.nav.syfo.util.convert
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
@Repository
class DialogmoteDAO @Inject constructor(
    private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate
) {
    fun get(fnr: Fodselsnummer, virksomhetsnummer: String, dialogmoteExternUUID: String): List<Dialogmote> {
        val query = """
            SELECT *
            FROM dialogmoter
            WHERE person_ident = :person_ident AND virksomhetsnummer = :virksomhetsnummer AND dialogmote_extern_uuid = :dialogmoteExternUUID
        """.trimIndent()
        val mapSql = MapSqlParameterSource()
            .addValue("person_ident", fnr.value)
            .addValue("virksomhetsnummer", virksomhetsnummer)
            .addValue("dialogmoteExternUUID", dialogmoteExternUUID)
        return namedParameterJdbcTemplate.query(
            query,
            mapSql,
            dialogmoteRowMapper
        )
    }

    fun update(
        fnr: Fodselsnummer,
        virksomhetsnummer: String,
        dialogmoteExternUUID: String,
        statusEndringType: String,
        statusEndringTidspunkt: LocalDateTime
    ) {
        val query = """
            UPDATE dialogmoter
            SET db_endring_tidspunkt = :db_endring_tidspunkt, status_endring_type = :statusEndringType, status_endring_tidspunkt = :statusEndringTidspunkt
            WHERE person_ident = :fnr AND virksomhetsnummer = :virksomhetsnummer AND dialogmote_extern_uuid = :dialogmoteExternUUID
        """.trimIndent()
        val mapSaveSql = MapSqlParameterSource()
            .addValue("db_endring_tidspunkt", convert(LocalDateTime.now()))
            .addValue("statusEndringType", statusEndringType)
            .addValue("fnr", fnr.value)
            .addValue("virksomhetsnummer", virksomhetsnummer)
            .addValue("dialogmoteExternUUID", dialogmoteExternUUID)
            .addValue("statusEndringTidspunkt", statusEndringTidspunkt)
        namedParameterJdbcTemplate.update(query, mapSaveSql)
    }

    fun create(
        moteExternUUID: String,
        dialogmoteTidspunkt: LocalDateTime,
        statusEndringTidspunkt: LocalDateTime,
        statusEndringType: String,
        fnr: Fodselsnummer,
        virksomhetsnummer: String
    ): UUID {
        val uuid = UUID.randomUUID()
        val query = """
             INSERT INTO dialogmoter (uuid, dialogmote_extern_uuid, dialogmote_tidspunkt, status_endring_tidspunkt, db_endring_tidspunkt, status_endring_type, person_ident, virksomhetsnummer)
             VALUES (:uuid, :moteExternUUID, :dialogmoteTidspunkt, :statusEndringTidspunkt, :dbEndringTidspunkt, :statusEndringType, :fnr, :virksomhetsnummer)
        """.trimIndent()
        val mapSaveSql = MapSqlParameterSource()
            .addValue("uuid", uuid.toString())
            .addValue("moteExternUUID", moteExternUUID)
            .addValue("dialogmoteTidspunkt", convert(dialogmoteTidspunkt))
            .addValue("statusEndringTidspunkt", convert(statusEndringTidspunkt))
            .addValue("dbEndringTidspunkt", convert(LocalDateTime.now()))
            .addValue("statusEndringType", statusEndringType)
            .addValue("fnr", fnr.value)
            .addValue("virksomhetsnummer", virksomhetsnummer)
        namedParameterJdbcTemplate.update(query, mapSaveSql)
        return uuid
    }

    fun delete(fnr: Fodselsnummer, virksomhetsnummer: String, moteExternUUID: String): Int {
        val dialogmoter = get(fnr, virksomhetsnummer, moteExternUUID)
        return if (dialogmoter.isNotEmpty()) {
            val dialogmoteIder: List<String> = dialogmoter.map {
                "${it.uuid}"
            }
            namedParameterJdbcTemplate.update(
                "DELETE FROM dialogmoter WHERE uuid IN (:dialogmoteIder)",
                MapSqlParameterSource()
                    .addValue("dialogmoteIder", dialogmoteIder)
            )
        } else {
            0
        }
    }

    fun getAktiveDialogmoterPaVirksomhetEtterDato(
        fnr: Fodselsnummer,
        virksomhetsnummer: String,
        dialogmoteTidspunkt: LocalDate
    ): List<Dialogmote> {
        val query = """
            SELECT *
            FROM dialogmoter
            WHERE person_ident = :person_ident AND virksomhetsnummer = :virksomhetsnummer AND (CAST (dialogmote_tidspunkt AS DATE) = :dialogmoteTidspunkt OR CAST (dialogmote_tidspunkt AS DATE) > :dialogmoteTidspunkt)
        """.trimIndent()
        val mapSql = MapSqlParameterSource()
            .addValue("person_ident", fnr.value)
            .addValue("virksomhetsnummer", virksomhetsnummer)
            .addValue("dialogmoteTidspunkt", convert(dialogmoteTidspunkt))
        return namedParameterJdbcTemplate.query(
            query,
            mapSql,
            dialogmoteRowMapper
        )
    }

    fun getAktiveDialogmoterEtterDato(
        fnr: Fodselsnummer,
        dialogmoteTidspunkt: LocalDate
    ): List<Dialogmote> {
        val query = """
            SELECT *
            FROM dialogmoter
            WHERE person_ident = :person_ident AND (CAST (dialogmote_tidspunkt AS DATE) = :dialogmoteTidspunkt OR CAST (dialogmote_tidspunkt AS DATE) > :dialogmoteTidspunkt)
        """.trimIndent()
        val mapSql = MapSqlParameterSource()
            .addValue("person_ident", fnr.value)
            .addValue("dialogmoteTidspunkt", convert(dialogmoteTidspunkt))
        return namedParameterJdbcTemplate.query(
            query,
            mapSql,
            dialogmoteRowMapper
        )
    }

    val dialogmoteRowMapper = RowMapper { resultSet, _ ->
        Dialogmote(
            uuid = UUID.fromString(resultSet.getString("uuid")),
            dialogmoteExternUuid = UUID.fromString(resultSet.getString("dialogmote_extern_uuid")),
            moteTidspunkt = resultSet.getTimestamp("dialogmote_tidspunkt").toLocalDateTime(),
            statusEndringTidspunkt = resultSet.getTimestamp("status_endring_tidspunkt").toLocalDateTime(),
            statusEndringType = DialogmoteStatusEndringType.valueOf(resultSet.getString("status_endring_type")),
            personIdent = resultSet.getString("person_ident"),
            virksomhetsnummer = resultSet.getString("virksomhetsnummer"),
        )
    }
}
