package no.nav.syfo.repository.dao;

import lombok.extern.slf4j.Slf4j;
import no.nav.syfo.repository.domain.PMotebehov;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.support.SqlLobValue;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Types;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.time.LocalDateTime.now;
import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static java.util.UUID.fromString;
import static java.util.stream.Collectors.toList;
import static no.nav.syfo.repository.DbUtil.*;

@Service
@Slf4j
@Transactional
@Repository
public class MotebehovDAO {

    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    private JdbcTemplate jdbcTemplate;

    public MotebehovDAO(NamedParameterJdbcTemplate namedParameterJdbcTemplate, JdbcTemplate jdbcTemplate) {
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<List<String>> hentAktorIdMedMotebehovForEnhet(String enhetId) {
        return ofNullable(jdbcTemplate.query("SELECT DISTINCT aktoer_id FROM motebehov WHERE tildelt_enhet = ? AND opprettet_dato >= ?", (rs, rowNum) -> rs.getString("aktoer_id"), enhetId, hentTidligsteDatoForGyldigMotebehovSvar()));
    }

    public Optional<List<PMotebehov>> hentMotebehovListeForAktoer(String aktoerId) {
        return ofNullable(jdbcTemplate.query("SELECT * FROM motebehov WHERE aktoer_id = ?", getInnsendingRowMapper(), aktoerId));
    }

    public Optional<List<PMotebehov>> hentMotebehovUbehandletListeForAktoer(String aktoerId) {
        return ofNullable(jdbcTemplate.query("SELECT * FROM motebehov WHERE aktoer_id = ? AND opprettet_dato >= ? AND behandlet_veileder_ident IS NULL", getInnsendingRowMapper(), aktoerId, hentTidligsteDatoForGyldigMotebehovSvar()));
    }

    public Optional<List<PMotebehov>> hentMotebehovListeForOgOpprettetAvArbeidstaker(String arbeidstakerAktorId) {
        return ofNullable(jdbcTemplate.query("SELECT * FROM motebehov WHERE aktoer_id = ? AND opprettet_av = ? AND opprettet_dato >= ?", getInnsendingRowMapper(), arbeidstakerAktorId, arbeidstakerAktorId, hentTidligsteDatoForGyldigMotebehovSvar()));
    }

    public Optional<List<PMotebehov>> hentMotebehovListeForArbeidstakerOpprettetAvLeder(String arbeidstakerAktorId, String virksomhetsnummer) {
        return ofNullable(jdbcTemplate.query("SELECT * FROM motebehov WHERE aktoer_id = ? AND opprettet_av != ? AND virksomhetsnummer = ? AND opprettet_dato >= ?", getInnsendingRowMapper(), arbeidstakerAktorId, arbeidstakerAktorId, virksomhetsnummer, hentTidligsteDatoForGyldigMotebehovSvar()));
    }

    public Optional<List<PMotebehov>> hentMotebehovListeForAktoerOgVirksomhetsnummer(String aktoerId, String virksomhetsnummer) {
        return ofNullable(jdbcTemplate.query("SELECT * FROM motebehov WHERE aktoer_id = ? AND virksomhetsnummer = ? AND opprettet_dato >= ?", getInnsendingRowMapper(), aktoerId, virksomhetsnummer, hentTidligsteDatoForGyldigMotebehovSvar()));
    }

    public int oppdaterUbehandledeMotebehovTilBehandlet(final String aktoerId, final String veilederIdent) {
        String oppdaterSql = "UPDATE motebehov SET behandlet_tidspunkt = ?, behandlet_veileder_ident = ? WHERE aktoer_id = ? AND har_motebehov = true AND behandlet_veileder_ident IS NULL";

        return jdbcTemplate.update(oppdaterSql, convert(now()), veilederIdent, aktoerId);
    }

    public UUID create(final PMotebehov motebehov) {
        UUID uuid = UUID.randomUUID();
        String lagreSql = "INSERT INTO motebehov VALUES(" +
                ":uuid, " +
                ":opprettet_dato, " +
                ":opprettet_av, " +
                ":aktoer_id, " +
                ":virksomhetsnummer, " +
                ":friskmelding_forventning, " +
                ":tiltak, " +
                ":tiltak_resultat, " +
                ":har_motebehov, " +
                ":forklaring," +
                ":tildelt_enhet," +
                ":behandlet_tidspunkt," +
                ":behandlet_veileder_ident" +
                ")";

        MapSqlParameterSource mapLagreSql = new MapSqlParameterSource()
                .addValue("uuid", uuid.toString())
                .addValue("opprettet_av", motebehov.opprettetAv)
                .addValue("opprettet_dato", convert(now()))
                .addValue("aktoer_id", motebehov.aktoerId)
                .addValue("virksomhetsnummer", motebehov.virksomhetsnummer)
                .addValue("friskmelding_forventning", new SqlLobValue(sanitizeUserInput(motebehov.friskmeldingForventning)), Types.CLOB)
                .addValue("tiltak", new SqlLobValue(sanitizeUserInput(motebehov.tiltak)), Types.CLOB)
                .addValue("tiltak_resultat", new SqlLobValue(sanitizeUserInput(motebehov.tiltakResultat)), Types.CLOB)
                .addValue("har_motebehov", motebehov.harMotebehov)
                .addValue("forklaring", new SqlLobValue(sanitizeUserInput(motebehov.forklaring)), Types.CLOB)
                .addValue("tildelt_enhet", motebehov.tildeltEnhet)
                .addValue("behandlet_tidspunkt", convert(motebehov.behandletTidspunkt()))
                .addValue("behandlet_veileder_ident", motebehov.behandletVeilederIdent);

        namedParameterJdbcTemplate.update(lagreSql, mapLagreSql);

        return uuid;
    }

    public List<PMotebehov> finnMotebehovMedBehovOpprettetSiden(LocalDateTime timestamp) {
        return jdbcTemplate.query("SELECT * FROM motebehov WHERE opprettet_dato > ? AND har_motebehov = 1", getInnsendingRowMapper(), timestamp);
    }

    private static RowMapper<PMotebehov> getInnsendingRowMapper() {
        return (rs, i) -> new PMotebehov()
                .uuid(fromString(rs.getString("motebehov_uuid")))
                .opprettetDato(convert(rs.getTimestamp("opprettet_dato")))
                .opprettetAv(rs.getString("opprettet_av"))
                .aktoerId(rs.getString("aktoer_id"))
                .virksomhetsnummer(rs.getString("virksomhetsnummer"))
                .friskmeldingForventning(rs.getString("friskmelding_forventning"))
                .tiltak(rs.getString("tiltak"))
                .tiltakResultat(rs.getString("tiltak_resultat"))
                .harMotebehov(rs.getBoolean("har_motebehov"))
                .forklaring(rs.getString("forklaring"))
                .tildeltEnhet(rs.getString("tildelt_enhet"))
                .behandletTidspunkt(convert(rs.getTimestamp("behandlet_tidspunkt")))
                .behandletVeilederIdent(rs.getString("behandlet_veileder_ident"));
    }

    public int nullstillMotebehov(String aktoerId) {
        if (hentMotebehovListeForAktoer(aktoerId).isPresent()) {
            List<UUID> motebehovIder = ofNullable(hentMotebehovListeForAktoer(aktoerId)
                    .get()
                    .stream()
                    .map(PMotebehov::uuid)
                    .collect(toList())).orElse(emptyList());

            int antallMotebehovSlettet = namedParameterJdbcTemplate.update(
                    "DELETE FROM motebehov WHERE motebehov_uuid IN (:motebehovIder)",

                    new MapSqlParameterSource()
                            .addValue("motebehovIder", motebehovIder));

            log.info("Slettet {} møtebehov på aktør: {}", antallMotebehovSlettet, aktoerId);

            return antallMotebehovSlettet;
        } else {
            return 0;
        }
    }
}
