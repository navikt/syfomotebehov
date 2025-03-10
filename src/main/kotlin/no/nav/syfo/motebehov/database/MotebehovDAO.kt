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
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import java.util.UUID

@Service
@Transactional
@Repository
class MotebehovDAO(
    private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate,
    private val jdbcTemplate: JdbcTemplate
) {
    fun hentMotebehovListeForAktoer(aktoerId: String): List<PMotebehov> {
        return Optional.ofNullable(
            jdbcTemplate.query(
                """
                SELECT m.*, s.* FROM motebehov m
                LEFT JOIN motebehov_form_values s ON m.motebehov_form_values_id = s.id
                WHERE m.aktoer_id = ? ORDER BY m.opprettet_dato ASC
                """,
                innsendingRowMapper,
                aktoerId
            )
        ).orElse(emptyList())
    }

    fun hentMotebehovListeForOgOpprettetAvArbeidstaker(arbeidstakerAktorId: String): List<PMotebehov> {
        return Optional.ofNullable(
            jdbcTemplate.query(
                """
                SELECT m.*, s.* FROM motebehov m
                LEFT JOIN motebehov_form_values s ON m.motebehov_form_values_id = s.id
                WHERE m.aktoer_id = ? AND m.opprettet_av = ? AND m.opprettet_dato >= ?
                ORDER BY m.opprettet_dato DESC
                """,
                innsendingRowMapper,
                arbeidstakerAktorId,
                arbeidstakerAktorId,
                hentTidligsteDatoForGyldigMotebehovSvar()
            )
        ).orElse(emptyList())
    }

    fun hentMotebehovListeForArbeidstakerOpprettetAvLeder(
        arbeidstakerAktorId: String,
        isOwnLeader: Boolean,
        virksomhetsnummer: String
    ): List<PMotebehov> {
        if (isOwnLeader) {
            val query =
                """
                SELECT m.*, s.* FROM motebehov m
                LEFT JOIN motebehov_form_values s ON m.motebehov_form_values_id = s.id
                WHERE m.aktoer_id = ? AND m.virksomhetsnummer = ? AND m.opprettet_dato >= ?
                ORDER BY m.opprettet_dato DESC
                """
            return Optional.ofNullable(
                jdbcTemplate.query(
                    query,
                    innsendingRowMapper,
                    arbeidstakerAktorId,
                    virksomhetsnummer,
                    hentTidligsteDatoForGyldigMotebehovSvar()
                )
            ).orElse(emptyList())
        } else {
            val query =
                """
                SELECT m.*, s.* FROM motebehov m
                LEFT JOIN motebehov_form_values s ON m.motebehov_form_values_id = s.id
                WHERE m.aktoer_id = ? AND m.opprettet_av != ? AND m.virksomhetsnummer = ? AND m.opprettet_dato >= ?
                ORDER BY m.opprettet_dato DESC
                """
            return Optional.ofNullable(
                jdbcTemplate.query(
                    query,
                    innsendingRowMapper,
                    arbeidstakerAktorId,
                    arbeidstakerAktorId,
                    virksomhetsnummer,
                    hentTidligsteDatoForGyldigMotebehovSvar()
                )
            ).orElse(emptyList())
        }
    }

    fun hentUbehandledeMotebehov(aktoerId: String): List<PMotebehov> {
        return Optional.ofNullable(
            jdbcTemplate.query(
                """
                SELECT m.*, s.* FROM motebehov m
                LEFT JOIN motebehov_form_values s ON m.motebehov_form_values_id = s.id
                WHERE m.aktoer_id = ? AND m.har_motebehov AND m.behandlet_veileder_ident IS NULL
                """,
                innsendingRowMapper,
                aktoerId
            )
        ).orElse(emptyList())
    }

    fun hentUbehandledeMotebehovEldreEnnDato(date: LocalDate): List<PMotebehov> {
        return Optional.ofNullable(
            jdbcTemplate.query(
                """
                SELECT m.*, s.* FROM motebehov m
                LEFT JOIN motebehov_form_values s ON m.motebehov_form_values_id = s.id
                WHERE m.opprettet_dato < ? AND m.har_motebehov AND m.behandlet_veileder_ident IS NULL
                """,
                innsendingRowMapper,
                convert(date)
            )
        ).orElse(emptyList())
    }

    fun hentMotebehov(motebehovId: String): List<PMotebehov> {
        return Optional.ofNullable(
            jdbcTemplate.query(
                """
                SELECT m.*, s.* FROM motebehov m
                LEFT JOIN motebehov_form_values s ON m.motebehov_form_values_id = s.id
                WHERE m.motebehov_uuid = ?
                """,
                innsendingRowMapper,
                motebehovId
            )
        ).orElse(emptyList())
    }

    fun oppdaterUbehandledeMotebehovTilBehandlet(
        motebehovUUID: UUID,
        veilederIdent: String,
    ): Int {
        val oppdaterSql =
            "UPDATE motebehov SET behandlet_tidspunkt = ?, behandlet_veileder_ident = ? " +
                "WHERE motebehov_uuid = ? AND har_motebehov AND behandlet_veileder_ident IS NULL"
        return jdbcTemplate.update(oppdaterSql, convert(LocalDateTime.now()), veilederIdent, motebehovUUID.toString())
    }

    fun create(motebehov: PMotebehov): UUID {
        val motebehovSvarId = motebehov.motebehovFormValues?.let {
            val svarUuid = UUID.randomUUID()
            val lagreSvarSql = """
            INSERT INTO motebehov_field_values (id, form_snapshot, begrunnelse, onsker_sykmelder_deltar, onsker_sykmelder_deltar_begrunnelse, onsker_tolk, tolk_sprak)
            VALUES (:id, :form_snapshot, :begrunnelse, :onsker_sykmelder_deltar, :onsker_sykmelder_deltar_begrunnelse, :onsker_tolk, :tolk_sprak)
            """.trimIndent()
            val mapLagreSvarSql = MapSqlParameterSource()
                .addValue("id", svarUuid.toString())
                .addValue("form_snapshot", it.formSnapshotJSON, Types.OTHER)
                .addValue("begrunnelse", it.begrunnelse)
                .addValue("onsker_sykmelder_deltar", it.onskerSykmelderDeltar)
                .addValue("onsker_sykmelder_deltar_begrunnelse", it.onskerSykmelderDeltarBegrunnelse)
                .addValue("onsker_tolk", it.onskerTolk)
                .addValue("tolk_sprak", it.tolkSprak)
            namedParameterJdbcTemplate.update(lagreSvarSql, mapLagreSvarSql)
            svarUuid
        }

        val uuid = UUID.randomUUID()
        val lagreSql = """
        INSERT INTO motebehov (motebehov_uuid, opprettet_dato, opprettet_av, aktoer_id, virksomhetsnummer, har_motebehov, forklaring, tildelt_enhet, behandlet_tidspunkt, behandlet_veileder_ident, skjematype, sm_fnr, opprettet_av_fnr, motebehov_form_values_id)
        VALUES (:motebehov_uuid, :opprettet_dato, :opprettet_av, :aktoer_id, :virksomhetsnummer, :har_motebehov, :forklaring, :tildelt_enhet, :behandlet_tidspunkt, :behandlet_veileder_ident, :skjematype, :sm_fnr, :opprettet_av_fnr, :motebehov_form_values_id)
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
            .addValue("motebehov_form_values_id", motebehovSvarId?.toString())
        namedParameterJdbcTemplate.update(lagreSql, mapLagreSql)

        return uuid
    }

    fun nullstillMotebehov(aktoerId: String): Int {
        val motebehovListe = hentMotebehovListeForAktoer(aktoerId)
        if (motebehovListe.isNotEmpty()) {
            val motebehovIder: List<String> = motebehovListe.map { it.uuid.toString() }
            val motebehovSvarIder: List<String> = motebehovListe.mapNotNull { it.motebehovFormValues?.uuid?.toString() }

            namedParameterJdbcTemplate.update(
                "DELETE FROM motebehov WHERE motebehov_uuid IN (:motebehovIder)",
                MapSqlParameterSource().addValue("motebehovIder", motebehovIder)
            )

            if (motebehovSvarIder.isNotEmpty()) {
                namedParameterJdbcTemplate.update(
                    "DELETE FROM motebehov_field_values WHERE id IN (:motebehovSvarIder)",
                    MapSqlParameterSource().addValue("motebehovSvarIder", motebehovSvarIder)
                )
            }

            return motebehovIder.size
        } else {
            return 0
        }
    }

    companion object {
        val innsendingRowMapper: RowMapper<PMotebehov> = RowMapper { rs: ResultSet, _: Int ->
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
                sykmeldtFnr = rs.getString("sm_fnr"),
                opprettetAvFnr = rs.getString("opprettet_av_fnr"),
                motebehovFormValues = if (rs.getString("motebehov_form_values_id") != null) {
                    motebehovSvarRowMapper.mapRow(rs, rs.row)
                } else {
                    null
                }
            )
        }

        val motebehovSvarRowMapper: RowMapper<PMotebehovFormValues> = RowMapper { rs: ResultSet, _: Int ->
            PMotebehovFormValues(
                uuid = UUID.fromString(rs.getString("id")),
                formSnapshotJSON = rs.getString("form_snapshot"),
                begrunnelse = rs.getString("begrunnelse")?.takeIf { it.isNotEmpty() },
                onskerSykmelderDeltar = rs.getBoolean("onsker_sykmelder_deltar"),
                onskerSykmelderDeltarBegrunnelse = rs.getString(
                    "onsker_sykmelder_deltar_begrunnelse"
                )?.takeIf { it.isNotEmpty() },
                onskerTolk = rs.getBoolean("onsker_tolk"),
                tolkSprak = rs.getString("tolk_sprak")?.takeIf { it.isNotEmpty() },
            )
        }
    }
}
