package no.nav.syfo.oppfolgingstilfelle.database

import no.nav.syfo.consumer.aktorregister.domain.Fodselsnummer
import no.nav.syfo.oppfolgingstilfelle.kafka.KOversikthendelsetilfelle
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
    fun get(fnr: Fodselsnummer, virksomhetsnummer: String): List<PPersonOppfolgingstilfelle> {
        val query = """
            SELECT *
            FROM oppfolgingstilfelle
            WHERE fnr = :fnr AND virksomhetsnummer = :virksomhetsnummer
        """.trimIndent()
        val mapSql = MapSqlParameterSource()
            .addValue("fnr", fnr.value)
            .addValue("virksomhetsnummer", virksomhetsnummer)
        return namedParameterJdbcTemplate.query(
            query,
            mapSql,
            personOppfolgingstilfelleRowMapper
        )
    }

    fun get(fnr: Fodselsnummer): List<PPersonOppfolgingstilfelle> {
        val query = """
            SELECT *
            FROM oppfolgingstilfelle
            WHERE fnr = :fnr
        """.trimIndent()
        val mapSql = MapSqlParameterSource()
            .addValue("fnr", fnr.value)
        return namedParameterJdbcTemplate.query(
            query,
            mapSql,
            personOppfolgingstilfelleRowMapper
        )
    }

    fun update(oversikthendelsetilfelle: KOversikthendelsetilfelle) {
        val query = """
            UPDATE oppfolgingstilfelle
            SET sist_endret = :sistEndret, fom = :fom, tom = :tom
            WHERE fnr = :fnr AND virksomhetsnummer = :virksomhetsnummer
        """.trimIndent()
        val mapSaveSql = MapSqlParameterSource()
            .addValue("sistEndret", convert(LocalDateTime.now()))
            .addValue("fom", convert(oversikthendelsetilfelle.fom))
            .addValue("tom", convert(oversikthendelsetilfelle.tom))
            .addValue("fnr", oversikthendelsetilfelle.fnr)
            .addValue("virksomhetsnummer", oversikthendelsetilfelle.virksomhetsnummer)
        namedParameterJdbcTemplate.update(query, mapSaveSql)
    }

    fun create(oversikthendelsetilfelle: KOversikthendelsetilfelle): UUID {
        val uuid = UUID.randomUUID()
        val query = """
            INSERT INTO oppfolgingstilfelle (oppfolgingstilfelle_uuid, opprettet, sist_endret, fnr, virksomhetsnummer, fom, tom)
            VALUES (:oppfolgingstilfelleUuid, :opprettet, :sistEndret, :fnr, :virksomhetsnummer, :fom, :tom)
        """.trimIndent()
        val mapSaveSql = MapSqlParameterSource()
            .addValue("oppfolgingstilfelleUuid", uuid.toString())
            .addValue("opprettet", convert(LocalDateTime.now()))
            .addValue("sistEndret", convert(LocalDateTime.now()))
            .addValue("fnr", oversikthendelsetilfelle.fnr)
            .addValue("virksomhetsnummer", oversikthendelsetilfelle.virksomhetsnummer)
            .addValue("fom", convert(oversikthendelsetilfelle.fom))
            .addValue("tom", convert(oversikthendelsetilfelle.tom))
        namedParameterJdbcTemplate.update(query, mapSaveSql)
        return uuid
    }

    fun nullstillOppfolgingstilfeller(fnr: Fodselsnummer): Int {
        val oppfolgingstilfeller = get(fnr)
        return if (oppfolgingstilfeller.isNotEmpty()) {
            val oppfolgingstilfelleIder: List<UUID> = oppfolgingstilfeller.map {
                it.uuid
            }
            namedParameterJdbcTemplate.update(
                "DELETE FROM oppfolgingstilfelle WHERE oppfolgingstilfelle_uuid IN (:oppfolgingstilfelleIder)",
                MapSqlParameterSource()
                    .addValue("oppfolgingstilfelleIder", oppfolgingstilfelleIder)
            )
        } else {
            0
        }
    }

    val personOppfolgingstilfelleRowMapper = RowMapper { resultSet, _ ->
        PPersonOppfolgingstilfelle(
            uuid = UUID.fromString(resultSet.getString("oppfolgingstilfelle_uuid")),
            opprettet = resultSet.getTimestamp("opprettet").toLocalDateTime(),
            sistEndret = resultSet.getTimestamp("sist_endret").toLocalDateTime(),
            fnr = resultSet.getString("fnr"),
            virksomhetsnummer = resultSet.getString("virksomhetsnummer"),
            fom = convert(resultSet.getTimestamp("fom")),
            tom = convert(resultSet.getTimestamp("tom"))
        )
    }
}
