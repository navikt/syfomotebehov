package no.nav.syfo.repository;

import no.nav.syfo.LocalApplication;
import no.nav.syfo.repository.dao.MotebehovDAO;
import no.nav.syfo.repository.domain.PMotebehov;
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
import java.util.UUID;

import static no.nav.syfo.repository.DbUtil.convert;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = LocalApplication.class)
@DirtiesContext
public class MotebehovDAOTest {

    private static final String TILTAK = "Hvilerom";
    private static final String SYKMELDT_AKTOERID = "10123456789";
    private static final String ARBEIDSGIVER_AKTOERID = "10123456780";
    private static final String VIRKSOMHETSNUMMER = "951110345";
    private static final Timestamp OPPRETTET_DATO = Timestamp.valueOf("2018-03-07 15:10:50.112000");

    private static final PMotebehov MOTEBEHOV_1 = new PMotebehov()
            .opprettetDato(convert(OPPRETTET_DATO))
            .opprettetAv(ARBEIDSGIVER_AKTOERID)
            .aktoerId(SYKMELDT_AKTOERID)
            .virksomhetsnummer(VIRKSOMHETSNUMMER)
            .friskmeldingForventning("Snart")
            .tiltak(TILTAK)
            .tiltakResultat("Mindre smerter")
            .harMotebehov(true)
            .forklaring("Megling");

    @Inject
    private JdbcTemplate jdbcTemplate;

    @Inject
    private MotebehovDAO motebehovDAO;

    @Before
    public void cleanup() {
        jdbcTemplate.update("DELETE FROM MOTEBEHOV");
    }

    @Test
    public void hentMotebehovListeForAktoer() throws Exception {
        jdbcTemplate.update("INSERT INTO MOTEBEHOV VALUES('bae778f2-a085-11e8-98d0-529269fb1459', '2018-03-07 15:10:50.112000', '" + ARBEIDSGIVER_AKTOERID + "', '" + SYKMELDT_AKTOERID + "', '" + VIRKSOMHETSNUMMER + "', 'Snart', '" + TILTAK + "', " +
                "'Mindre smerter', '1', 'Megling')");
        List<PMotebehov> motebehovListe = motebehovDAO.hentMotebehovListeForAktoer(SYKMELDT_AKTOERID).orElseThrow(Exception::new);

        assertThat(motebehovListe.size()).isEqualTo(1);
        final PMotebehov motebehovFraDb = motebehovListe.get(0);
        assertThat(motebehovFraDb.opprettetDato).isEqualTo(MOTEBEHOV_1.opprettetDato);
        assertThat(motebehovFraDb.opprettetAv).isEqualTo(MOTEBEHOV_1.opprettetAv);
        assertThat(motebehovFraDb.aktoerId).isEqualTo(MOTEBEHOV_1.aktoerId);
        assertThat(motebehovFraDb.virksomhetsnummer).isEqualTo(MOTEBEHOV_1.virksomhetsnummer);
        assertThat(motebehovFraDb.friskmeldingForventning).isEqualTo(MOTEBEHOV_1.friskmeldingForventning);
        assertThat(motebehovFraDb.tiltak).isEqualTo(MOTEBEHOV_1.tiltak);
        assertThat(motebehovFraDb.tiltakResultat).isEqualTo(MOTEBEHOV_1.tiltakResultat);
        assertThat(motebehovFraDb.harMotebehov).isTrue();
        assertThat(motebehovFraDb.forklaring).isEqualTo(MOTEBEHOV_1.forklaring);

    }

    @Test
    public void lagreMotebehov() throws Exception {
        UUID uuid = motebehovDAO.create(MOTEBEHOV_1);

        List<PMotebehov> motebehovListe = motebehovDAO.hentMotebehovListeForAktoer(SYKMELDT_AKTOERID).orElseThrow(Exception::new);
        assertThat(motebehovListe.size()).isEqualTo(1);
        assertThat(motebehovListe.get(0).uuid).isEqualTo(uuid);
        assertThat(motebehovListe.get(0).tiltak).isEqualTo(TILTAK);
    }
}