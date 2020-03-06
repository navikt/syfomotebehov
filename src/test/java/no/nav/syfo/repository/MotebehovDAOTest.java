package no.nav.syfo.repository;

import no.nav.syfo.LocalApplication;
import no.nav.syfo.repository.dao.MotebehovDAO;
import no.nav.syfo.repository.domain.PMotebehov;
import no.nav.syfo.testhelper.MotebehovGenerator;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import javax.inject.Inject;
import java.util.List;
import java.util.UUID;

import static no.nav.syfo.testhelper.UserConstants.*;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = LocalApplication.class)
@DirtiesContext
public class MotebehovDAOTest {

    @Inject
    private JdbcTemplate jdbcTemplate;

    @Inject
    private MotebehovDAO motebehovDAO;

    private MotebehovGenerator motebehovGenerator = new MotebehovGenerator();

    @Before
    public void cleanup() {
        String sqlDeleteAll = "DELETE FROM MOTEBEHOV";
        jdbcTemplate.update(sqlDeleteAll);
    }

    private void insertPMotebehov(PMotebehov motebehov) {
        String sqlInsert = "INSERT INTO MOTEBEHOV VALUES('bae778f2-a085-11e8-98d0-529269fb1459', '" + motebehov.opprettetDato + "', '" + motebehov.opprettetAv + "', '" + motebehov.aktoerId + "', '" + motebehov.virksomhetsnummer + "', '" + '1' + "', '" + motebehov.forklaring + "', '" + motebehov.tildeltEnhet + "', null, null)";
        jdbcTemplate.update(sqlInsert);
    }

    @Test
    public void hentMotebehovListeForAktoer() throws Exception {
        PMotebehov pMotebehov = motebehovGenerator.generatePmotebehov();
        insertPMotebehov(pMotebehov);

        List<PMotebehov> motebehovListe = motebehovDAO.hentMotebehovListeForAktoer(ARBEIDSTAKER_AKTORID).orElseThrow(Exception::new);

        assertThat(motebehovListe.size()).isEqualTo(1);
        final PMotebehov motebehovFraDb = motebehovListe.get(0);
        assertThat(motebehovFraDb.opprettetDato).isEqualTo(pMotebehov.opprettetDato);
        assertThat(motebehovFraDb.opprettetAv).isEqualTo(pMotebehov.opprettetAv);
        assertThat(motebehovFraDb.aktoerId).isEqualTo(pMotebehov.aktoerId);
        assertThat(motebehovFraDb.virksomhetsnummer).isEqualTo(pMotebehov.virksomhetsnummer);
        assertThat(motebehovFraDb.harMotebehov).isEqualTo(pMotebehov.harMotebehov);
        assertThat(motebehovFraDb.forklaring).isEqualTo(pMotebehov.forklaring);
        assertThat(motebehovFraDb.tildeltEnhet).isEqualTo(pMotebehov.tildeltEnhet);

    }

    @Test
    public void hentMotebehovListeForOgOpprettetAvArbeidstakerIkkeGyldig() throws Exception {
        PMotebehov pMotebehov = motebehovGenerator.generatePmotebehov()
                .opprettetDato(motebehovGenerator.getOpprettetDato(false))
                .opprettetAv(ARBEIDSTAKER_AKTORID);
        insertPMotebehov(pMotebehov);
        List<PMotebehov> motebehovListe = motebehovDAO.hentMotebehovListeForOgOpprettetAvArbeidstaker(ARBEIDSTAKER_AKTORID).orElseThrow(Exception::new);

        assertThat(motebehovListe.size()).isEqualTo(0);
    }

    @Test
    public void hentMotebehovListeForOgOpprettetAvArbeidstakerGyldig() throws Exception {
        PMotebehov pMotebehov = motebehovGenerator.generatePmotebehov()
                .opprettetDato(motebehovGenerator.getOpprettetDato(true))
                .opprettetAv(ARBEIDSTAKER_AKTORID);
        insertPMotebehov(pMotebehov);

        List<PMotebehov> motebehovListe = motebehovDAO.hentMotebehovListeForOgOpprettetAvArbeidstaker(ARBEIDSTAKER_AKTORID).orElseThrow(Exception::new);

        assertThat(motebehovListe.size()).isEqualTo(1);

        final PMotebehov motebehovFraDb = motebehovListe.get(0);
        assertThat(motebehovFraDb.opprettetDato).isEqualTo(pMotebehov.opprettetDato);
        assertThat(motebehovFraDb.opprettetAv).isEqualTo(pMotebehov.opprettetAv);
        assertThat(motebehovFraDb.aktoerId).isEqualTo(pMotebehov.aktoerId);
    }

    @Test
    public void hentMotebehovListeForArbeidstakerOpprettetAvLederIkkeGyldig() throws Exception {
        PMotebehov pMotebehov = motebehovGenerator.generatePmotebehov()
                .opprettetDato(motebehovGenerator.getOpprettetDato(false))
                .opprettetAv(LEDER_AKTORID);
        insertPMotebehov(pMotebehov);

        List<PMotebehov> motebehovListe = motebehovDAO.hentMotebehovListeForArbeidstakerOpprettetAvLeder(ARBEIDSTAKER_AKTORID, VIRKSOMHETSNUMMER).orElseThrow(Exception::new);

        assertThat(motebehovListe.size()).isEqualTo(0);
    }

    @Test
    public void hentMotebehovListeForArbeidstakerOpprettetAvLederGyldig() throws Exception {
        PMotebehov pMotebehov = motebehovGenerator.generatePmotebehov()
                .opprettetDato(motebehovGenerator.getOpprettetDato(true))
                .opprettetAv(LEDER_AKTORID);
        insertPMotebehov(pMotebehov);

        List<PMotebehov> motebehovListe = motebehovDAO.hentMotebehovListeForArbeidstakerOpprettetAvLeder(ARBEIDSTAKER_AKTORID, VIRKSOMHETSNUMMER).orElseThrow(Exception::new);

        assertThat(motebehovListe.size()).isEqualTo(1);

        final PMotebehov motebehovFraDb = motebehovListe.get(0);
        assertThat(motebehovFraDb.opprettetDato).isEqualTo(pMotebehov.opprettetDato);
        assertThat(motebehovFraDb.opprettetAv).isEqualTo(LEDER_AKTORID);
        assertThat(motebehovFraDb.aktoerId).isEqualTo(pMotebehov.aktoerId);
    }

    @Test
    public void hentMotebehovListeForAktoerOgVirksomhetsnummerIkkeGyldig() throws Exception {
        PMotebehov pMotebehov = motebehovGenerator.generatePmotebehov()
                .opprettetDato(motebehovGenerator.getOpprettetDato(false))
                .opprettetAv(LEDER_AKTORID);
        insertPMotebehov(pMotebehov);

        List<PMotebehov> motebehovListe = motebehovDAO.hentMotebehovListeForAktoerOgVirksomhetsnummer(ARBEIDSTAKER_AKTORID, VIRKSOMHETSNUMMER).orElseThrow(Exception::new);

        assertThat(motebehovListe.size()).isEqualTo(0);
    }

    @Test
    public void hentMotebehovListeForAktoerOgVirksomhetsnummerGyldig() throws Exception {
        PMotebehov pMotebehov = motebehovGenerator.generatePmotebehov()
                .opprettetDato(motebehovGenerator.getOpprettetDato(true))
                .opprettetAv(LEDER_AKTORID);
        insertPMotebehov(pMotebehov);

        List<PMotebehov> motebehovListe = motebehovDAO.hentMotebehovListeForAktoerOgVirksomhetsnummer(ARBEIDSTAKER_AKTORID, VIRKSOMHETSNUMMER).orElseThrow(Exception::new);

        assertThat(motebehovListe.size()).isEqualTo(1);

        final PMotebehov motebehovFraDb = motebehovListe.get(0);
        assertThat(motebehovFraDb.opprettetDato).isEqualTo(pMotebehov.opprettetDato);
        assertThat(motebehovFraDb.opprettetAv).isEqualTo(pMotebehov.opprettetAv);
        assertThat(motebehovFraDb.aktoerId).isEqualTo(pMotebehov.aktoerId);
    }

    @Test
    public void lagreMotebehov() throws Exception {
        UUID uuid = motebehovDAO.create(motebehovGenerator.generatePmotebehov());

        List<PMotebehov> motebehovListe = motebehovDAO.hentMotebehovListeForAktoer(ARBEIDSTAKER_AKTORID).orElseThrow(Exception::new);
        assertThat(motebehovListe.size()).isEqualTo(1);
        assertThat(motebehovListe.get(0).uuid).isEqualTo(uuid);
        ;
    }
}
