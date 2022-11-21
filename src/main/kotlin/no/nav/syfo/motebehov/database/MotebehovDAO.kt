package no.nav.syfo.motebehov.database

import no.nav.syfo.motebehov.motebehovstatus.MotebehovSkjemaType
import no.nav.syfo.util.DbUtil.sanitizeUserInput
import no.nav.syfo.util.convert
import no.nav.syfo.util.convertNullable
import no.nav.syfo.util.hentTidligsteDatoForGyldigMotebehovSvar
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

    fun hentMotebehovUtenFnr(): List<PMotebehov> {
        return Optional.ofNullable(jdbcTemplate.query("SELECT * FROM motebehov WHERE sm_fnr is null OR opprettet_av_fnr is null", innsendingRowMapper)).orElse(emptyList())
    }

    fun oppdaterMotebehovMedSmFnr(uuid: UUID, smFnr: String): Int {
        val oppdaterSql = "UPDATE motebehov SET sm_fnr = ? WHERE motebehov_uuid = ?"
        return jdbcTemplate.update(oppdaterSql, smFnr, uuid.toString())
    }

    fun oppdaterMotebehovMedOpprettetAvFnr(uuid: UUID, opprettetAvFnr: String): Int {
        val oppdaterSql = "UPDATE motebehov SET opprettet_av_fnr = ? WHERE motebehov_uuid = ?"
        return jdbcTemplate.update(oppdaterSql, opprettetAvFnr, uuid.toString())
    }

    fun hentMotebehovListeForAktoer(aktoerId: String): List<PMotebehov> {
        return Optional.ofNullable(jdbcTemplate.query("SELECT * FROM motebehov WHERE aktoer_id = ?", innsendingRowMapper, aktoerId)).orElse(emptyList())
    }

    fun hentMotebehovListeForOgOpprettetAvArbeidstaker(arbeidstakerAktorId: String): List<PMotebehov> {
        return Optional.ofNullable(jdbcTemplate.query("SELECT * FROM motebehov WHERE aktoer_id = ? AND opprettet_av = ? AND opprettet_dato >= ? ORDER BY opprettet_dato DESC", innsendingRowMapper, arbeidstakerAktorId, arbeidstakerAktorId, hentTidligsteDatoForGyldigMotebehovSvar())).orElse(emptyList())
    }

    fun hentMotebehovListeForArbeidstakerOpprettetAvLeder(arbeidstakerAktorId: String, isOwnLeader: Boolean, virksomhetsnummer: String): List<PMotebehov> {
        if (isOwnLeader) {
            return Optional.ofNullable(jdbcTemplate.query("SELECT * FROM motebehov WHERE aktoer_id = ? AND virksomhetsnummer = ? AND opprettet_dato >= ? ORDER BY opprettet_dato DESC", innsendingRowMapper, arbeidstakerAktorId, virksomhetsnummer, hentTidligsteDatoForGyldigMotebehovSvar())).orElse(emptyList())
        }

        return Optional.ofNullable(jdbcTemplate.query("SELECT * FROM motebehov WHERE aktoer_id = ? AND opprettet_av != ? AND virksomhetsnummer = ? AND opprettet_dato >= ? ORDER BY opprettet_dato DESC", innsendingRowMapper, arbeidstakerAktorId, arbeidstakerAktorId, virksomhetsnummer, hentTidligsteDatoForGyldigMotebehovSvar())).orElse(emptyList())
    }

    fun hentUbehandledeMotebehov(aktoerId: String): List<PMotebehov> {
        return Optional.ofNullable(jdbcTemplate.query("SELECT * FROM motebehov WHERE aktoer_id = ? AND har_motebehov = 1 AND behandlet_veileder_ident IS NULL", innsendingRowMapper, aktoerId)).orElse(emptyList())
    }

    fun oppdaterUbehandledeMotebehovTilBehandlet(
        motebehovUUID: UUID,
        veilederIdent: String
    ): Int {
        val oppdaterSql = "UPDATE motebehov SET behandlet_tidspunkt = ?, behandlet_veileder_ident = ? WHERE motebehov_uuid = ? AND har_motebehov = 1 AND behandlet_veileder_ident IS NULL"
        return jdbcTemplate.update(oppdaterSql, convert(LocalDateTime.now()), veilederIdent, motebehovUUID.toString())
    }

    fun create(motebehov: PMotebehov): UUID {
        val uuid = UUID.randomUUID()
        val lagreSql = """
            INSERT INTO motebehov (motebehov_uuid, opprettet_dato, opprettet_av, aktoer_id, virksomhetsnummer, har_motebehov, forklaring, tildelt_enhet, behandlet_tidspunkt, behandlet_veileder_ident, skjematype, sm_fnr, opprettet_av_fnr)
            VALUES (
                :motebehov_uuid, :opprettet_dato, :opprettet_av, :aktoer_id, :virksomhetsnummer, :har_motebehov, :forklaring, :tildelt_enhet, :behandlet_tidspunkt, :behandlet_veileder_ident, :skjematype, :sm_fnr, :opprettet_av_fnr)
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
            .addValue("sm_fnr", motebehov.sykmeldtFnr)
            .addValue("opprettet_av_fnr", motebehov.opprettetAvFnr)
        namedParameterJdbcTemplate.update(lagreSql, mapLagreSql)
        return uuid
    }

    fun nullstillMotebehov(aktoerId: String): Int {
        return if (hentMotebehovListeForAktoer(aktoerId).isNotEmpty()) {
            val motebehovIder: List<String> = hentMotebehovListeForAktoer(aktoerId).map {
                it.uuid.toString()
            }
            namedParameterJdbcTemplate.update(
                "DELETE FROM motebehov WHERE motebehov_uuid IN (:motebehovIder)",
                MapSqlParameterSource()
                    .addValue("motebehovIder", motebehovIder)
            )
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
                    skjemaType = rs.getString("skjematype")?.let { MotebehovSkjemaType.valueOf(it) },

                )
            }
    }
}
