package no.nav.syfo.motebehov.database

import no.nav.syfo.motebehov.motebehovstatus.MotebehovSkjemaType
import no.nav.syfo.util.DbUtil.hentTidligsteDatoForGyldigMotebehovSvar
import no.nav.syfo.util.DbUtil.sanitizeUserInput
import no.nav.syfo.util.convert
import no.nav.syfo.util.convertNullable
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.core.support.SqlLobValue
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.sql.ResultSet
import java.sql.Types
import java.time.LocalDateTime
import java.util.*
import java.util.UUID

@Service
@Transactional
@Repository
class MotebehovDAO(private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate, private val jdbcTemplate: JdbcTemplate) {
    fun hentMotebehovListeForAktoer(aktoerId: String): List<PMotebehov> {
        return Optional.ofNullable(jdbcTemplate.query("SELECT * FROM motebehov WHERE aktoer_id = ?", innsendingRowMapper, aktoerId)).orElse(emptyList())
    }

    fun hentMotebehovListeForOgOpprettetAvArbeidstaker(arbeidstakerAktorId: String): List<PMotebehov> {
        return Optional.ofNullable(jdbcTemplate.query("SELECT * FROM motebehov WHERE aktoer_id = ? AND opprettet_av = ? AND opprettet_dato >= ? ORDER BY opprettet_dato DESC", innsendingRowMapper, arbeidstakerAktorId, arbeidstakerAktorId, hentTidligsteDatoForGyldigMotebehovSvar())).orElse(emptyList())
    }

    fun hentMotebehovListeForArbeidstakerOpprettetAvLeder(arbeidstakerAktorId: String, virksomhetsnummer: String): List<PMotebehov> {
        return Optional.ofNullable(jdbcTemplate.query("SELECT * FROM motebehov WHERE aktoer_id = ? AND opprettet_av != ? AND virksomhetsnummer = ? AND opprettet_dato >= ? ORDER BY opprettet_dato DESC", innsendingRowMapper, arbeidstakerAktorId, arbeidstakerAktorId, virksomhetsnummer, hentTidligsteDatoForGyldigMotebehovSvar())).orElse(emptyList())
    }

    fun oppdaterUbehandledeMotebehovTilBehandlet(aktoerId: String, veilederIdent: String): Int {
        val oppdaterSql = "UPDATE motebehov SET behandlet_tidspunkt = ?, behandlet_veileder_ident = ? WHERE aktoer_id = ? AND har_motebehov = 1 AND behandlet_veileder_ident IS NULL"
        return jdbcTemplate.update(oppdaterSql, convert(LocalDateTime.now()), veilederIdent, aktoerId)
    }

    fun create(motebehov: PMotebehov): UUID {
        val uuid = UUID.randomUUID()
        val lagreSql = """
            INSERT INTO motebehov (motebehov_uuid, opprettet_dato, opprettet_av, aktoer_id, virksomhetsnummer, har_motebehov, forklaring, tildelt_enhet, behandlet_tidspunkt, behandlet_veileder_ident, skjematype)
            VALUES (
                :motebehov_uuid, :opprettet_dato, :opprettet_av, :aktoer_id, :virksomhetsnummer, :har_motebehov, :forklaring, :tildelt_enhet, :behandlet_tidspunkt, :behandlet_veileder_ident, :skjematype)
            """.trimIndent()
        val mapLagreSql = MapSqlParameterSource()
            .addValue("motebehov_uuid", uuid.toString())
            .addValue("opprettet_av", motebehov.opprettetAv)
            .addValue("opprettet_dato", convert(LocalDateTime.now()))
            .addValue("aktoer_id", motebehov.aktoerId)
            .addValue("virksomhetsnummer", motebehov.virksomhetsnummer)
            .addValue("har_motebehov", motebehov.harMotebehov)
            .addValue("forklaring", SqlLobValue(sanitizeUserInput(motebehov.forklaring)), Types.CLOB)
            .addValue("tildelt_enhet", motebehov.tildeltEnhet)
            .addValue("behandlet_tidspunkt", convertNullable(motebehov.behandletTidspunkt))
            .addValue("behandlet_veileder_ident", motebehov.behandletVeilederIdent)
            .addValue("skjematype", motebehov.skjemaType?.name)
        namedParameterJdbcTemplate.update(lagreSql, mapLagreSql)
        return uuid
    }

    fun nullstillMotebehov(aktoerId: String): Int {
        return if (hentMotebehovListeForAktoer(aktoerId).isNotEmpty()) {
            val motebehovIder: List<UUID> = hentMotebehovListeForAktoer(aktoerId).map {
                it.uuid
            }
            namedParameterJdbcTemplate.update(
                "DELETE FROM motebehov WHERE motebehov_uuid IN (:motebehovIder)",
                MapSqlParameterSource()
                    .addValue("motebehovIder", motebehovIder))
        } else {
            0
        }
    }

    companion object {
        private val innsendingRowMapper: RowMapper<PMotebehov>
            get() = RowMapper { rs: ResultSet, _: Int ->
                PMotebehov(
                    uuid = UUID.fromString(rs.getString("motebehov_uuid")),
                    opprettetDato = rs.getTimestamp("opprettet_dato").toLocalDateTime(),
                    opprettetAv = rs.getString("opprettet_av"),
                    aktoerId = rs.getString("aktoer_id"),
                    virksomhetsnummer = rs.getString("virksomhetsnummer"),
                    harMotebehov = rs.getBoolean("har_motebehov"),
                    forklaring = rs.getString("forklaring"),
                    tildeltEnhet = rs.getString("tildelt_enhet"),
                    behandletTidspunkt = convertNullable(rs.getTimestamp("behandlet_tidspunkt")),
                    behandletVeilederIdent = rs.getString("behandlet_veileder_ident"),
                    skjemaType = rs.getString("skjematype")?.let { MotebehovSkjemaType.valueOf(it) }
                )
            }
    }
}
