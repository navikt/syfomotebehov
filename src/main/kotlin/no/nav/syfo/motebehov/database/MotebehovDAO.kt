package no.nav.syfo.motebehov.database

import no.nav.syfo.motebehov.MotebehovInnmelderType
import no.nav.syfo.motebehov.extractFormValuesFromFormSnapshot
import no.nav.syfo.motebehov.formSnapshot.FormSnapshot
import no.nav.syfo.motebehov.formSnapshot.convertFormSnapshotToJsonString
import no.nav.syfo.motebehov.formSnapshot.convertJsonStringToFormSnapshot
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
import org.springframework.jdbc.support.GeneratedKeyHolder
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.sql.ResultSet
import java.sql.Types
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Service
@Transactional
@Repository
class MotebehovDAO(
    private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate,
    private val jdbcTemplate: JdbcTemplate
) {
    fun hentMotebehovListeForAktoer(aktoerId: String): List<PMotebehov> {
        return jdbcTemplate.query(
            """
                SELECT m.*, f.* FROM motebehov m
                LEFT JOIN motebehov_form_values f ON m.id = f.motebehov_row_id
                WHERE m.aktoer_id = ? ORDER BY m.opprettet_dato ASC
                """,
            motebehovRowMapper,
            aktoerId
        ) ?: emptyList()
    }

    fun hentMotebehovListeForOgOpprettetAvArbeidstaker(arbeidstakerAktorId: String): List<PMotebehov> {
        return jdbcTemplate.query(
            """
                SELECT m.*, f.* FROM motebehov m
                LEFT JOIN motebehov_form_values f ON m.id = f.motebehov_row_id
                WHERE m.aktoer_id = ?
                AND m.opprettet_av = ?
                AND m.opprettet_dato >= ?
                ORDER BY m.opprettet_dato DESC
                """,
            motebehovRowMapper,
            arbeidstakerAktorId,
            arbeidstakerAktorId,
            hentTidligsteDatoForGyldigMotebehovSvar()
        ) ?: emptyList()
    }

    /**
     * isOwnLeader is true if the leader making the request is the same person as the arbeidstaker identified by
     * arbeidstakerAktorId.
     */
    fun hentMotebehovListeForArbeidstakerOpprettetAvLeder(
        arbeidstakerAktorId: String,
        isOwnLeader: Boolean,
        virksomhetsnummer: String
    ): List<PMotebehov> {
        if (isOwnLeader) {
            val query =
                """
                SELECT m.*, f.* FROM motebehov m
                LEFT JOIN motebehov_form_values f ON m.id = f.motebehov_row_id
                WHERE m.aktoer_id = ?
                AND m.virksomhetsnummer = ?
                AND m.opprettet_dato >= ?
                ORDER BY m.opprettet_dato DESC
                """
            return jdbcTemplate.query(
                query,
                motebehovRowMapper,
                arbeidstakerAktorId,
                virksomhetsnummer,
                hentTidligsteDatoForGyldigMotebehovSvar()
            ) ?: emptyList()
        } else {
            // If isOwnLeader is false, the leader making the request is a different person from the arbeidstaker, and
            // motebehov innmeldt by the arbeidstaker must not be included in the result. This is ensured by the
            // WHERE opprettet_av != arbeidstakerAktorId filter.
            val query =
                """
                SELECT m.*, f.* FROM motebehov m
                LEFT JOIN motebehov_form_values f ON m.id = f.motebehov_row_id
                WHERE m.aktoer_id = ?
                AND m.opprettet_av != ?
                AND m.virksomhetsnummer = ?
                AND m.opprettet_dato >= ?
                ORDER BY m.opprettet_dato DESC
                """
            return jdbcTemplate.query(
                query,
                motebehovRowMapper,
                arbeidstakerAktorId,
                arbeidstakerAktorId,
                virksomhetsnummer,
                hentTidligsteDatoForGyldigMotebehovSvar()
            ) ?: emptyList()
        }
    }

    fun hentUbehandledeMotebehov(aktoerId: String): List<PMotebehov> {
        return jdbcTemplate.query(
            """
                SELECT m.*, f.* FROM motebehov m
                LEFT JOIN motebehov_form_values f ON m.id = f.motebehov_row_id
                WHERE m.aktoer_id = ? AND m.har_motebehov AND m.behandlet_veileder_ident IS NULL
                """,
            motebehovRowMapper,
            aktoerId
        ) ?: emptyList()
    }

    fun hentUbehandledeMotebehovEldreEnnDato(date: LocalDate): List<PMotebehov> {
        return jdbcTemplate.query(
            """
                SELECT m.*, f.* FROM motebehov m
                LEFT JOIN motebehov_form_values f ON m.id = f.motebehov_row_id
                WHERE m.opprettet_dato < ? AND m.har_motebehov AND m.behandlet_veileder_ident IS NULL
                """,
            motebehovRowMapper,
            convert(date)
        ) ?: emptyList()
    }

    fun hentMotebehov(motebehovUUID: String): List<PMotebehov> {
        return jdbcTemplate.query(
            """
                SELECT m.*, f.* FROM motebehov m
                LEFT JOIN motebehov_form_values f ON m.id = f.motebehov_row_id
                WHERE m.motebehov_uuid = ?
                """,
            motebehovRowMapper,
            motebehovUUID
        ) ?: emptyList()
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
        val uuid = UUID.randomUUID()

        val lagreMotebehovSql = """
        INSERT INTO motebehov (motebehov_uuid, opprettet_dato, opprettet_av, aktoer_id, virksomhetsnummer,
            har_motebehov, forklaring, tildelt_enhet, behandlet_tidspunkt, behandlet_veileder_ident, skjematype,
            innmelder_type, sm_fnr, opprettet_av_fnr)
        VALUES                (:motebehov_uuid, :opprettet_dato, :opprettet_av, :aktoer_id, :virksomhetsnummer,
            :har_motebehov, :forklaring, :tildelt_enhet, :behandlet_tidspunkt, :behandlet_veileder_ident, :skjematype,
            :innmelder_type, :sm_fnr, :opprettet_av_fnr)
        """.trimIndent()

        val mapLagreMotebehovSql = MapSqlParameterSource()
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
            .addValue("skjematype", motebehov.skjemaType.name)
            .addValue("innmelder_type", motebehov.innmelderType.name)
            .addValue("sm_fnr", motebehov.sykmeldtFnr?.toLong())
            .addValue("opprettet_av_fnr", listOf(motebehov.opprettetAvFnr, "not", "string"))

        val keyHolder = GeneratedKeyHolder()

        namedParameterJdbcTemplate.update(
            lagreMotebehovSql,
            mapLagreMotebehovSql,
            keyHolder,
            arrayOf("id")
        )

        if (motebehov.formSnapshot != null) {
            val motebehovRowId = keyHolder.key?.toLong() ?: error("Failed to retrieve generated key for motebehov")

            insertIntoMotebehovFormValues(motebehov.formSnapshot, motebehovRowId)
        }

        return uuid
    }

    fun insertIntoMotebehovFormValues(formSnapshot: FormSnapshot, motebehovRowId: Long) {
        val formSnapshotJSON = convertFormSnapshotToJsonString(formSnapshot)
        val formValues = extractFormValuesFromFormSnapshot(formSnapshot)

        val lagreMotebehovFormValuesSql = """
            INSERT INTO motebehov_form_values (motebehov_row_id, form_snapshot, form_identifier, form_semantic_version,
                begrunnelse, onsker_sykmelder_deltar, onsker_sykmelder_deltar_begrunnelse,
                onsker_tolk, tolk_sprak)
            VALUES                         (:motebehov_row_id, :form_snapshot, :form_identifier, :form_semantic_version,
                :begrunnelse, :onsker_sykmelder_deltar, :onsker_sykmelder_deltar_begrunnelse,
                :onsker_tolk, :tolk_sprak)
        """.trimIndent()
        val mapLagreMotebehovFormValuesSql = MapSqlParameterSource()
            .addValue("motebehov_row_id", motebehovRowId)
            .addValue("form_snapshot", formSnapshotJSON, Types.OTHER)
            // The columns below store copies of values inside form_snapshot, and are only used for
            // debugging and data monitoring purposes. They are not read out again in this application.
            .addValue("form_identifier", formValues.formIdentifier)
            .addValue("form_semantic_version", formValues.formSemanticVersion)
            .addValue("begrunnelse", formValues.begrunnelse)
            .addValue("onsker_sykmelder_deltar", formValues.onskerSykmelderDeltar)
            .addValue("onsker_sykmelder_deltar_begrunnelse", formValues.onskerSykmelderDeltarBegrunnelse)
            .addValue("onsker_tolk", formValues.onskerTolk)
            .addValue("tolk_sprak", formValues.tolkSprak)
        namedParameterJdbcTemplate.update(lagreMotebehovFormValuesSql, mapLagreMotebehovFormValuesSql)
    }

    fun nullstillMotebehov(aktoerId: String): Int {
        val motebehovListe = hentMotebehovListeForAktoer(aktoerId)
        if (motebehovListe.isNotEmpty()) {
            val motebehovIder: List<String> = motebehovListe.map { it.uuid.toString() }

            namedParameterJdbcTemplate.update(
                "DELETE FROM motebehov WHERE motebehov_uuid IN (:motebehovIder)",
                MapSqlParameterSource().addValue("motebehovIder", motebehovIder)
            )

            return motebehovIder.size
        } else {
            return 0
        }
    }

    companion object {
        val motebehovRowMapper: RowMapper<PMotebehov> = RowMapper { rs: ResultSet, _: Int ->
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
                skjemaType = rs.getString("skjematype").let { MotebehovSkjemaType.valueOf(it) },
                innmelderType = rs.getString("innmelder_type").let { MotebehovInnmelderType.valueOf(it) },
                sykmeldtFnr = rs.getString("sm_fnr"),
                opprettetAvFnr = rs.getString("opprettet_av_fnr"),
                formSnapshot = rs.getString("form_snapshot")?.let { convertJsonStringToFormSnapshot(it) },
            )
        }
    }
}
