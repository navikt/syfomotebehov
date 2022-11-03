package no.nav.syfo.dialogmotekandidat.database

import no.nav.syfo.util.convert
import no.nav.syfo.util.mapToBoolean
import no.nav.syfo.util.mapToString
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.*
import javax.inject.Inject

@Transactional
@Repository
class DialogmotekandidatDAO @Inject constructor(
    private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate
) {
    fun get(fnr: String): DialogmoteKandidatEndring? {
        val query = """
            SELECT *
            FROM DIALOGMOTEKANDIDAT
            WHERE $COLUMN_PERSON_IDENT = :fnr
        """.trimIndent()
        val mapSql = MapSqlParameterSource()
            .addValue("fnr", fnr)

        return try {
            namedParameterJdbcTemplate.queryForObject(
                query,
                mapSql,
                dialogmotekandidatRowMapper
            )
        } catch (emptyException: EmptyResultDataAccessException) {
            return null
        }
    }

    fun create(
        dialogmotekandidatExternalUUID: String,
        createdAt: LocalDateTime,
        fnr: String,
        kandidat: Boolean,
        arsak: String
    ): UUID {
        val uuid = UUID.randomUUID()
        val query = """
             INSERT INTO DIALOGMOTEKANDIDAT ($COLUMN_UUID, $COLUMN_EXTERNAL_UUID, $COLUMN_PERSON_IDENT, $COLUMN_KANDIDAT, $COLUMN_ARSAK, $COLUMN_CREATED_AT, $COLUMN_DATABASE_UPDATED_AT)
             VALUES (:uuid, :dialogmotekandidatExternalUUID, :fnr, :kandidat, :arsak, :createdAt, :databaseUpdatedAt)
        """.trimIndent()
        val mapSaveSql = MapSqlParameterSource()
            .addValue("uuid", uuid.toString())
            .addValue("dialogmotekandidatExternalUUID", dialogmotekandidatExternalUUID)
            .addValue("fnr", fnr)
            .addValue("kandidat", kandidat.mapToString())
            .addValue("arsak", arsak)
            .addValue("createdAt", convert(createdAt))
            .addValue("databaseUpdatedAt", convert(LocalDateTime.now()))
        namedParameterJdbcTemplate.update(query, mapSaveSql)
        return uuid
    }

    fun update(
        dialogmotekandidatExternalUUID: String,
        createdAt: LocalDateTime,
        fnr: String,
        kandidat: Boolean,
        arsak: String
    ) {
        val query = """
            UPDATE DIALOGMOTEKANDIDAT
            SET $COLUMN_KANDIDAT = :kandidat, $COLUMN_ARSAK = :arsak, $COLUMN_CREATED_AT = :createdAt,  $COLUMN_DATABASE_UPDATED_AT = :databaseUpdatedAt
            WHERE $COLUMN_PERSON_IDENT = :fnr
        """.trimIndent()
        val mapSaveSql = MapSqlParameterSource()
            .addValue("dialogmotekandidatExternalUUID", dialogmotekandidatExternalUUID)
            .addValue("fnr", fnr)
            .addValue("kandidat", kandidat.mapToString())
            .addValue("arsak", arsak)
            .addValue("createdAt", convert(createdAt))
            .addValue("databaseUpdatedAt", convert(LocalDateTime.now()))
        namedParameterJdbcTemplate.update(query, mapSaveSql)
    }

    val dialogmotekandidatRowMapper = RowMapper { resultSet, _ ->
        DialogmoteKandidatEndring(
            uuid = UUID.fromString(resultSet.getString(COLUMN_UUID)),
            dialogmotekandidatExternUUID = UUID.fromString(resultSet.getString(COLUMN_EXTERNAL_UUID)),
            personIdentNumber = resultSet.getString(COLUMN_PERSON_IDENT),
            kandidat = resultSet.getString(COLUMN_KANDIDAT).mapToBoolean(),
            arsak = DialogmotekandidatEndringArsak.valueOf(resultSet.getString(COLUMN_ARSAK)),
            createdAt = resultSet.getTimestamp(COLUMN_CREATED_AT).toLocalDateTime(),
            databaseUpdatedAt = resultSet.getTimestamp(COLUMN_DATABASE_UPDATED_AT).toLocalDateTime(),
        )
    }

    fun delete(fnr: String): Int {
        return namedParameterJdbcTemplate.update(
            "DELETE FROM DIALOGMOTEKANDIDAT WHERE $COLUMN_PERSON_IDENT = (:fnr)",
            MapSqlParameterSource()
                .addValue("fnr", fnr)
        )
    }

    companion object {
        const val COLUMN_UUID = "uuid"
        const val COLUMN_EXTERNAL_UUID = "dialogmotekandidat_external_uuid"
        const val COLUMN_PERSON_IDENT = "person_ident"
        const val COLUMN_KANDIDAT = "kandidat"
        const val COLUMN_ARSAK = "arsak"
        const val COLUMN_CREATED_AT = "created_at"
        const val COLUMN_DATABASE_UPDATED_AT = "database_updated_at"
    }
}
