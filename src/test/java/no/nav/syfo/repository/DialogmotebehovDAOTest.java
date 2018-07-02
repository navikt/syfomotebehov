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

    public static final String TILTAK = "Ståmatte";
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
        jdbcTemplate.update("INSERT INTO dialogmotebehov VALUES('id', 'Snart', 'Hvilerom', " +
                "'Mindre smerter', '1', 'Megling')");

        List<Dialogmotebehov> dialogmotebehovListe = dialogmotebehovDAO.hentDialogmotebehovListe();

    }

    @Test
    public void lagreDialogmotebehov() {
        Dialogmotebehov dialogmotebehov = Dialogmotebehov.builder()
                .tidspunktFriskmelding("Om et par måneder. Svært sannsynlig")
                .tiltak(TILTAK)
                .resultatTiltak("Rygg og skulderproblemene ble umiddelbart bedre")
            //    .trengerMote(true)
                .behovDialogmote("Fordi")
                .build();

        dialogmotebehovDAO.lagreDialogmotebehov(dialogmotebehov);

        List<Dialogmotebehov> dialogmotebehovListe = jdbcTemplate.query("SELECT * FROM dialogmotebehov", DialogmotebehovDAO.getInnsendingRowMapper());
        assertThat(dialogmotebehovListe.size()).isEqualTo(1);
        assertThat(dialogmotebehovListe.get(0).getTiltak()).isEqualTo(TILTAK);
    }
}