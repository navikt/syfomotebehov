package no.nav.syfo.repository;

import no.nav.syfo.domain.Dialogmotebehov;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
@Repository
public class DialogmotebehovDAO{

    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public DialogmotebehovDAO(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
    }

    public String lagreDialogmotebehov(final Dialogmotebehov dialogmotebehov) {
        String uuid = UUID.randomUUID().toString();
        String lagreSql = "INSERT INTO DIALOGMOTEBEHOV VALUES(" +
                ":uuid, " +
                ":tidspunkt_friskmelding, " +
                ":tiltak, " +
                ":resultat_tiltak, " +
                ":trenger_mote, " +
                ":behov_dialogmote" +
                ")";

         MapSqlParameterSource mapeLagreSql = new MapSqlParameterSource()
                .addValue("uuid", uuid)
                .addValue("tidspunkt_friskmelding", dialogmotebehov.getTidspunktFriskmelding())
                .addValue("tiltak", dialogmotebehov.getTiltak())
                .addValue("resultat_tiltak", dialogmotebehov.getResultatTiltak())
                .addValue("trenger_mote", dialogmotebehov.isTrengerMote())
                .addValue("behov_dialogmote", dialogmotebehov.getBehovDialogmote());

        namedParameterJdbcTemplate.update(lagreSql, mapeLagreSql);

        return uuid;
    }

    public List<Dialogmotebehov> hentDialogmotebehovListe() {
        return namedParameterJdbcTemplate.query("SELECT * FROM DIALOGMOTEBEHOV", getInnsendingRowMapper());
    }

    public static RowMapper<Dialogmotebehov> getInnsendingRowMapper() {
        return (resultSet, i) -> Dialogmotebehov.builder()
                .uuid(resultSet.getString("DIALOGMOTEBEHOV_UUID"))
                .tiltak(resultSet.getString("TILTAK"))
                .resultatTiltak(resultSet.getString("RESULTAT_TILTAK"))
                //.trengerMote(resultSet.getBoolean("TRENGER_MOTE"))
                .behovDialogmote(resultSet.getString("BEHOV_DIALOGMOTE"))
                .build();
    }
}
