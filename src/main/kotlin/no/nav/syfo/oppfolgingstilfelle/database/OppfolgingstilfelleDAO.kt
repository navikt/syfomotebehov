package no.nav.syfo.oppfolgingstilfelle.database

import no.nav.syfo.oppfolgingstilfelle.syketilfelle.KOppfolgingstilfelle
import no.nav.syfo.util.convert
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.*
import javax.inject.Inject

@Service
@Transactional
@Repository
class OppfolgingstilfelleDAO @Inject constructor(
        private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate
) {
    fun get(aktorId: String, virksomhetsnummer: String): List<PPersonOppfolgingstilfelle> {
        val query = """
            SELECT *
            FROM oppfolgingstilfelle
            WHERE aktoer_id = :aktorid AND virksomhetsnummer = :virksomhetsnummer
            """.trimIndent()
        val mapSql = MapSqlParameterSource()
                .addValue("aktorid", aktorId)
                .addValue("virksomhetsnummer", virksomhetsnummer)
        return namedParameterJdbcTemplate.query(
                query,
                mapSql,
                personOppfolgingstilfelleRowMapper
        )
    }

    fun get(aktorId: String): List<PPersonOppfolgingstilfelle> {
        val query = """
            SELECT *
            FROM oppfolgingstilfelle
            WHERE aktoer_id = :aktorid
            """.trimIndent()
        val mapSql = MapSqlParameterSource()
                .addValue("aktorid", aktorId)
        return namedParameterJdbcTemplate.query(
                query,
                mapSql,
                personOppfolgingstilfelleRowMapper
        )
    }

    fun update(oppfolgingstilfelle: KOppfolgingstilfelle) {
        val query = """
            UPDATE oppfolgingstilfelle
            SET sist_endret = :sistEndret, fom = :fom, tom = :tom
            WHERE aktoer_id = :aktorId AND virksomhetsnummer = :virksomhetsnummer
            """.trimIndent()
        val mapSaveSql = MapSqlParameterSource()
                .addValue("sistEndret", convert(LocalDateTime.now()))
                .addValue("fom", convert(oppfolgingstilfelle.tidslinje.first().dag))
                .addValue("tom", convert(oppfolgingstilfelle.tidslinje.last().dag))
                .addValue("aktorId", oppfolgingstilfelle.aktorId)
                .addValue("virksomhetsnummer", oppfolgingstilfelle.orgnummer)
        namedParameterJdbcTemplate.update(query, mapSaveSql)
    }

    fun create(oppfolgingstilfelle: KOppfolgingstilfelle): UUID {
        val uuid = UUID.randomUUID()
        val query = """
            INSERT INTO oppfolgingstilfelle (oppfolgingstilfelle_uuid, opprettet, sist_endret, aktoer_id, virksomhetsnummer, fom, tom)
            VALUES (:oppfolgingstilfelleUuid, :opprettet, :sistEndret, :aktorId, :virksomhetsnummer, :fom, :tom)
            """.trimIndent()
        val mapSaveSql = MapSqlParameterSource()
                .addValue("oppfolgingstilfelleUuid", uuid.toString())
                .addValue("opprettet", convert(LocalDateTime.now()))
                .addValue("sistEndret", convert(LocalDateTime.now()))
                .addValue("aktorId", oppfolgingstilfelle.aktorId)
                .addValue("virksomhetsnummer", oppfolgingstilfelle.orgnummer)
                .addValue("fom", convert(oppfolgingstilfelle.tidslinje.first().dag))
                .addValue("tom", convert(oppfolgingstilfelle.tidslinje.last().dag))
        namedParameterJdbcTemplate.update(query, mapSaveSql)
        return uuid
    }

    fun nullstillOppfolgingstilfeller(aktorId: String): Int {
        val oppfolgingstilfeller = get(aktorId)
        return if (oppfolgingstilfeller.isNotEmpty()) {
            val oppfolgingstilfelleIder: List<UUID> = oppfolgingstilfeller.map {
                it.uuid
            }
            namedParameterJdbcTemplate.update(
                    "DELETE FROM oppfolgingstilfelle WHERE oppfolgingstilfelle_uuid IN (:oppfolgingstilfelleIder)",
                    MapSqlParameterSource()
                            .addValue("oppfolgingstilfelleIder", oppfolgingstilfelleIder))
        } else {
            0
        }
    }

    val personOppfolgingstilfelleRowMapper = RowMapper { resultSet, _ ->
        PPersonOppfolgingstilfelle(
                uuid = UUID.fromString(resultSet.getString("oppfolgingstilfelle_uuid")),
                opprettet = resultSet.getTimestamp("opprettet").toLocalDateTime(),
                sistEndret = resultSet.getTimestamp("sist_endret").toLocalDateTime(),
                aktorId = resultSet.getString("aktoer_id"),
                virksomhetsnummer = resultSet.getString("virksomhetsnummer"),
                fom = convert(resultSet.getTimestamp("fom")),
                tom = convert(resultSet.getTimestamp("tom"))
        )
    }
}
