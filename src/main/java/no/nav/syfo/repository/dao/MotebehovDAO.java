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
        return ofNullable(jdbcTemplate.query("SELECT DISTINCT AKTOER_ID FROM MOTEBEHOV WHERE TILDELT_ENHET = ? AND OPPRETTET_DATO >= ?", (rs, rowNum) -> rs.getString("aktoer_id"), enhetId, hentTidligsteDatoForGyldigMotebehovSvar()));
    }

    public Optional<List<PMotebehov>> hentMotebehovListeForAktoer(String aktoerId) {
        return ofNullable(jdbcTemplate.query("SELECT * FROM MOTEBEHOV WHERE AKTOER_ID = ? AND OPPRETTET_DATO >= ?", getInnsendingRowMapper(), aktoerId, hentTidligsteDatoForGyldigMotebehovSvar()));
    }

    public Optional<List<PMotebehov>> hentMotebehovListeForOgOpprettetAvArbeidstaker(String arbeidstakerAktorId) {
        return ofNullable(jdbcTemplate.query("SELECT * FROM MOTEBEHOV WHERE aktoer_id = ? AND OPPRETTET_AV = ? AND OPPRETTET_DATO >= ?", getInnsendingRowMapper(), arbeidstakerAktorId, arbeidstakerAktorId, hentTidligsteDatoForGyldigMotebehovSvar()));
    }

    public Optional<List<PMotebehov>> hentMotebehovListeForArbeidstakerOpprettetAvLeder(String arbeidstakerAktorId, String virksomhetsnummer) {
        return ofNullable(jdbcTemplate.query("SELECT * FROM MOTEBEHOV WHERE aktoer_id = ? AND OPPRETTET_AV != ? AND VIRKSOMHETSNUMMER = ? AND OPPRETTET_DATO >= ?", getInnsendingRowMapper(), arbeidstakerAktorId, arbeidstakerAktorId, virksomhetsnummer, hentTidligsteDatoForGyldigMotebehovSvar()));
    }

    public Optional<List<PMotebehov>> hentMotebehovListeForAktoerOgVirksomhetsnummer(String aktoerId, String virksomhetsnummer) {
        return ofNullable(jdbcTemplate.query("SELECT * FROM MOTEBEHOV WHERE aktoer_id = ? AND virksomhetsnummer = ? AND OPPRETTET_DATO >= ?", getInnsendingRowMapper(), aktoerId, virksomhetsnummer, hentTidligsteDatoForGyldigMotebehovSvar()));
    }

    public UUID create(final PMotebehov motebehov) {
        UUID uuid = UUID.randomUUID();
        String lagreSql = "INSERT INTO MOTEBEHOV VALUES(" +
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
                ":tildelt_enhet" +
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
                .addValue("tildelt_enhet", motebehov.tildeltEnhet);

        namedParameterJdbcTemplate.update(lagreSql, mapLagreSql);

        return uuid;
    }

    public List<PMotebehov> finnMotebehovOpprettetSiden(LocalDateTime timestamp) {
        return jdbcTemplate.query("SELECT * FROM MOTEBEHOV WHERE opprettet_dato > ?", getInnsendingRowMapper(), timestamp);
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
                .tildeltEnhet(rs.getString("tildelt_enhet"));
    }

    public int nullstillMotebehov(String aktoerId) {
        if (hentMotebehovListeForAktoer(aktoerId).isPresent()) {
            List<UUID> motebehovIder = ofNullable(hentMotebehovListeForAktoer(aktoerId)
                    .get()
                    .stream()
                    .map(PMotebehov::uuid)
                    .collect(toList())).orElse(emptyList());

            int antallMotebehovSlettet = namedParameterJdbcTemplate.update(
                    "DELETE FROM MOTEBEHOV WHERE motebehov_uuid IN (:motebehovIder)",

                    new MapSqlParameterSource()
                            .addValue("motebehovIder", motebehovIder));

            log.info("Slettet {} møtebehov på aktør: {}", antallMotebehovSlettet, aktoerId);

            return antallMotebehovSlettet;
        } else {
            return 0;
        }
    }
}
