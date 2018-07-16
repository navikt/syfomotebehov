package no.nav.syfo.repository.dao;

import lombok.extern.slf4j.Slf4j;
import no.nav.syfo.repository.domain.PDialogmotebehov;
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
public class DialogmotebehovDAO {

    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    private JdbcTemplate jdbcTemplate;

    public DialogmotebehovDAO(NamedParameterJdbcTemplate namedParameterJdbcTemplate, JdbcTemplate jdbcTemplate) {
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<PDialogmotebehov> hentDialogmotebehovListeForAktoer(String aktoerId) {
        return jdbcTemplate.query("SELECT * FROM DIALOGMOTEBEHOV WHERE aktoer_id = ?", getInnsendingRowMapper(), aktoerId);
    }

    public String create(final PDialogmotebehov dialogmotebehov) {
        String uuid = UUID.randomUUID().toString();
        String lagreSql = "INSERT INTO DIALOGMOTEBEHOV VALUES(" +
                ":uuid, " +
                ":opprettet_dato, " +
                ":opprettet_av, " +
                ":aktoer_id, " +
                ":friskmelding_forventning, " +
                ":tiltak, " +
                ":tiltak_resultat, " +
                ":har_motebehov, " +
                ":forklaring" +
                ")";

        MapSqlParameterSource mapLagreSql = new MapSqlParameterSource()
                .addValue("uuid", uuid)
                .addValue("opprettet_av", dialogmotebehov.getOpprettetAv())
                .addValue("opprettet_dato", convert(now()))
                .addValue("aktoer_id", dialogmotebehov.getAktoerId())
                .addValue("friskmelding_forventning", new SqlLobValue(sanitizeUserInput(dialogmotebehov.getFriskmeldingForventning())), Types.CLOB)
                .addValue("tiltak", new SqlLobValue(sanitizeUserInput(dialogmotebehov.getTiltak())), Types.CLOB)
                .addValue("tiltak_resultat", new SqlLobValue(sanitizeUserInput(dialogmotebehov.getTiltakResultat())), Types.CLOB)
                .addValue("har_motebehov", dialogmotebehov.isHarMotebehov())
                .addValue("forklaring", new SqlLobValue(sanitizeUserInput(dialogmotebehov.getForklaring())), Types.CLOB);

        namedParameterJdbcTemplate.update(lagreSql, mapLagreSql);

        return uuid;
    }

    public static RowMapper<PDialogmotebehov> getInnsendingRowMapper() {
        return (rs, i) -> PDialogmotebehov.builder()
                .uuid(rs.getString("dialogmotebehov_uuid"))
                .opprettetDato(convert(rs.getTimestamp("opprettet_dato")))
                .opprettetAv(rs.getString("opprettet_av"))
                .aktoerId(rs.getString("aktoer_id"))
                .friskmeldingForventning(rs.getString("friskmelding_forventning"))
                .tiltak(rs.getString("tiltak"))
                .tiltakResultat(rs.getString("tiltak_resultat"))
                .harMotebehov(rs.getBoolean("har_motebehov"))
                .forklaring(rs.getString("forklaring"))
                .build();
    }

    public int nullstillMotebehov(String aktoerId) {
        List<Long> motebehovIder = namedParameterJdbcTemplate.query(
                "SELECT dialogmotebehov_uuid FROM DIALOGMOTEBEHOV WHERE (aktoer_id = :aktoerId)",

                new MapSqlParameterSource()
                        .addValue("aktoerId", aktoerId),

                (row, rowNum) -> row.getLong("dialogmotebehov_uuid"));

        int antallMotebehovSlettet = namedParameterJdbcTemplate.update(
                "DELETE FROM DIALOGMOTEBEHOV WHERE dialogmotebehov_uuid IN (:motebehovIder)",

                new MapSqlParameterSource()
                        .addValue("motebehovIder", motebehovIder));

        log.info("Slettet {} møtebehov på aktør: {}", antallMotebehovSlettet, aktoerId);

        return antallMotebehovSlettet;
    }
}
