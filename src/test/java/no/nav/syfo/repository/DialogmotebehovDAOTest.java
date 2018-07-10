package no.nav.syfo.repository;

import no.nav.syfo.LocalApplication;
import no.nav.syfo.domain.Dialogmotebehov;
import no.nav.syfo.repository.dao.DialogmotebehovDAO;
import no.nav.syfo.repository.domain.PDialogmotebehov;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import javax.inject.Inject;
import java.sql.Timestamp;
import java.util.List;

import static no.nav.syfo.repository.DbUtil.convert;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = LocalApplication.class)
@DirtiesContext
public class DialogmotebehovDAOTest {

    private static final String TILTAK = "Hvilerom";
    private static final String SYKMELDT_AKTOERID = "10123456789";
    private static final String ARBEIDSGIVER_AKTOERID = "10123456780";
    private static final Timestamp OPPRETTET_DATO = Timestamp.valueOf("2018-03-07 15:10:50.112000");

    private static final Dialogmotebehov DIALOGMOTEBEHOV_1 = Dialogmotebehov.builder()
            .opprettetDato(convert(OPPRETTET_DATO))
            .opprettetAv(ARBEIDSGIVER_AKTOERID)
            .aktoerId(SYKMELDT_AKTOERID)
            .friskmeldingForventning("Snart")
            .tiltak(TILTAK)
            .tiltakResultat("Mindre smerter")
            .harMotebehov(true)
            .forklaring("Megling")
            .build();

    @Inject
    private JdbcTemplate jdbcTemplate;

    @Inject
    private DialogmotebehovDAO dialogmotebehovDAO;

    @Before
    public void cleanup() {
        jdbcTemplate.update("DELETE FROM DIALOGMOTEBEHOV");
    }

    @Test
    public void hentDialogmotebehovListeForAktoer() {
        jdbcTemplate.update("INSERT INTO dialogmotebehov VALUES('id', '2018-03-07 15:10:50.112000', '" + ARBEIDSGIVER_AKTOERID + "', '" + SYKMELDT_AKTOERID + "', 'Snart', '" + TILTAK + "', " +
                "'Mindre smerter', '1', 'Megling')");
        List<Dialogmotebehov> dialogmotebehovListe = dialogmotebehovDAO.hentDialogmotebehovListeForAktoer(SYKMELDT_AKTOERID);

        assertThat(dialogmotebehovListe.size()).isEqualTo(1);
        final Dialogmotebehov dialogmotebehovFraDb = dialogmotebehovListe.get(0);
        assertThat(dialogmotebehovFraDb.getOpprettetDato()).isEqualTo(DIALOGMOTEBEHOV_1.getOpprettetDato());
        assertThat(dialogmotebehovFraDb.getOpprettetAv()).isEqualTo(DIALOGMOTEBEHOV_1.getOpprettetAv());
        assertThat(dialogmotebehovFraDb.getAktoerId()).isEqualTo(DIALOGMOTEBEHOV_1.getAktoerId());
        assertThat(dialogmotebehovFraDb.getFriskmeldingForventning()).isEqualTo(DIALOGMOTEBEHOV_1.getFriskmeldingForventning());
        assertThat(dialogmotebehovFraDb.getTiltak()).isEqualTo(DIALOGMOTEBEHOV_1.getTiltak());
        assertThat(dialogmotebehovFraDb.getTiltakResultat()).isEqualTo(DIALOGMOTEBEHOV_1.getTiltakResultat());
        assertThat(dialogmotebehovFraDb.isHarMotebehov()).isTrue();
        assertThat(dialogmotebehovFraDb.getForklaring()).isEqualTo(DIALOGMOTEBEHOV_1.getForklaring());

    }

    @Test
    public void lagreDialogmotebehov() {
        dialogmotebehovDAO.create(DIALOGMOTEBEHOV_1);

        List<PDialogmotebehov> dialogmotebehovListe = jdbcTemplate.query("SELECT * FROM dialogmotebehov", DialogmotebehovDAO.getInnsendingRowMapper());
        assertThat(dialogmotebehovListe.size()).isEqualTo(1);
        assertThat(dialogmotebehovListe.get(0).getTiltak()).isEqualTo(TILTAK);
    }
}