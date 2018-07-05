package no.nav.syfo.repository;

import no.nav.syfo.LocalApplication;
import no.nav.syfo.domain.Dialogmotebehov;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import javax.inject.Inject;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = LocalApplication.class)
@DirtiesContext
public class DialogmotebehovDAOTest {

    private static final String TILTAK = "Hvilerom";
    private static final Dialogmotebehov DIALOGMOTEBEHOV_1 = Dialogmotebehov.builder()
            .tidspunktFriskmelding("Snart")
            .tiltak(TILTAK)
            .resultatTiltak("Mindre smerter")
            .trengerMote(true)
            .behovDialogmote("Megling")
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
    public void hentDialogmotebehovListe() {
        jdbcTemplate.update("INSERT INTO dialogmotebehov VALUES('id', 'Snart', '" + TILTAK + "', " +
                "'Mindre smerter', '1', 'Megling')");
        List<Dialogmotebehov> dialogmotebehovListe = dialogmotebehovDAO.hentDialogmotebehovListe();

        assertThat(dialogmotebehovListe.size()).isEqualTo(1);
        final Dialogmotebehov dialogmotebehovFraDb = dialogmotebehovListe.get(0);
        assertThat(dialogmotebehovFraDb.getTidspunktFriskmelding()).isEqualTo(DIALOGMOTEBEHOV_1.getTidspunktFriskmelding());
        assertThat(dialogmotebehovFraDb.getTiltak()).isEqualTo(DIALOGMOTEBEHOV_1.getTiltak());
        assertThat(dialogmotebehovFraDb.getResultatTiltak()).isEqualTo(DIALOGMOTEBEHOV_1.getResultatTiltak());
        assertThat(dialogmotebehovFraDb.isTrengerMote()).isTrue();
        assertThat(dialogmotebehovFraDb.getBehovDialogmote()).isEqualTo(DIALOGMOTEBEHOV_1.getBehovDialogmote());

    }

    @Test
    public void lagreDialogmotebehov() {
        dialogmotebehovDAO.lagreDialogmotebehov(DIALOGMOTEBEHOV_1);

        List<Dialogmotebehov> dialogmotebehovListe = jdbcTemplate.query("SELECT * FROM dialogmotebehov", DialogmotebehovDAO.getInnsendingRowMapper());
        assertThat(dialogmotebehovListe.size()).isEqualTo(1);
        assertThat(dialogmotebehovListe.get(0).getTiltak()).isEqualTo(TILTAK);
    }
}