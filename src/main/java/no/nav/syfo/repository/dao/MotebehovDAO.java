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
import java.util.List;
import java.util.UUID;

import static java.time.LocalDateTime.now;
import static no.nav.syfo.repository.DbUtil.convert;
import static no.nav.syfo.repository.DbUtil.sanitizeUserInput;

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

    public List<PMotebehov> hentMotebehovListeForAktoer(String aktoerId) {
        return jdbcTemplate.query("SELECT * FROM MOTEBEHOV WHERE aktoer_id = ?", getInnsendingRowMapper(), aktoerId);
    }

    public String create(final PMotebehov motebehov) {
        String uuid = UUID.randomUUID().toString();
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
                ":forklaring" +
                ")";

        MapSqlParameterSource mapLagreSql = new MapSqlParameterSource()
                .addValue("uuid", uuid)
                .addValue("opprettet_av", motebehov.getOpprettetAv())
                .addValue("opprettet_dato", convert(now()))
                .addValue("aktoer_id", motebehov.getAktoerId())
                .addValue("virksomhetsnummer", motebehov.getVirksomhetsnummer())
                .addValue("friskmelding_forventning", new SqlLobValue(sanitizeUserInput(motebehov.getFriskmeldingForventning())), Types.CLOB)
                .addValue("tiltak", new SqlLobValue(sanitizeUserInput(motebehov.getTiltak())), Types.CLOB)
                .addValue("tiltak_resultat", new SqlLobValue(sanitizeUserInput(motebehov.getTiltakResultat())), Types.CLOB)
                .addValue("har_motebehov", motebehov.isHarMotebehov())
                .addValue("forklaring", new SqlLobValue(sanitizeUserInput(motebehov.getForklaring())), Types.CLOB);

        namedParameterJdbcTemplate.update(lagreSql, mapLagreSql);

        return uuid;
    }

    public static RowMapper<PMotebehov> getInnsendingRowMapper() {
        return (rs, i) -> PMotebehov.builder()
                .uuid(rs.getString("motebehov_uuid"))
                .opprettetDato(convert(rs.getTimestamp("opprettet_dato")))
                .opprettetAv(rs.getString("opprettet_av"))
                .aktoerId(rs.getString("aktoer_id"))
                .virksomhetsnummer(rs.getString("virksomhetsnummer"))
                .friskmeldingForventning(rs.getString("friskmelding_forventning"))
                .tiltak(rs.getString("tiltak"))
                .tiltakResultat(rs.getString("tiltak_resultat"))
                .harMotebehov(rs.getBoolean("har_motebehov"))
                .forklaring(rs.getString("forklaring"))
                .build();
    }

    public int nullstillMotebehov(String aktoerId) {
        List<Long> motebehovIder = namedParameterJdbcTemplate.query(
                "SELECT motebehov_uuid FROM MOTEBEHOV WHERE (aktoer_id = :aktoerId)",

                new MapSqlParameterSource()
                        .addValue("aktoerId", aktoerId),

                (row, rowNum) -> row.getLong("motebehov_uuid"));

        int antallMotebehovSlettet = namedParameterJdbcTemplate.update(
                "DELETE FROM MOTEBEHOV WHERE motebehov_uuid IN (:motebehovIder)",

                new MapSqlParameterSource()
                        .addValue("motebehovIder", motebehovIder));

        log.info("Slettet {} møtebehov på aktør: {}", antallMotebehovSlettet, aktoerId);

        return antallMotebehovSlettet;
    }
}
