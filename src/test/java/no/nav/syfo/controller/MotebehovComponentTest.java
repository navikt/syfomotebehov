package no.nav.syfo.controller;

import no.nav.security.oidc.context.OIDCRequestContextHolder;
import no.nav.syfo.LocalApplication;
import no.nav.syfo.domain.rest.Fnr;
import no.nav.syfo.domain.rest.Motebehov;
import no.nav.syfo.domain.rest.MotebehovSvar;
import no.nav.syfo.domain.rest.NyttMotebehov;
import no.nav.syfo.mock.AktoerMock;
import no.nav.syfo.repository.dao.MotebehovDAO;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import javax.inject.Inject;
import java.util.List;

import static no.nav.syfo.util.OidcTestHelper.loggInnBruker;
import static no.nav.syfo.util.OidcTestHelper.loggUtAlle;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Komponent / blackbox test av møtebehovsfunskjonaliteten - test at input til endepunktet (controlleren, for enkelhets skyld)
 * lagres og hentes riktig fra minnedatabasen.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = LocalApplication.class)
@DirtiesContext
public class MotebehovComponentTest {

    private static final String ARBEIDSTAKER_FNR = "12345678910";
    public static final String ARBEIDSTAKER_AKTORID = AktoerMock.mockAktorId(ARBEIDSTAKER_FNR);
    private static final String LEDER_FNR = "10987654321";
    public static final String LEDER_AKTORID = AktoerMock.mockAktorId(LEDER_FNR);
    private static final String VIRKSOMHETSNUMMER = "1234";
    private static final String TILDELT_ENHET = "0330";

    @Inject
    private MotebehovBrukerController motebehovController;

    @Inject
    private OIDCRequestContextHolder oidcRequestContextHolder;

    @Inject
    private MotebehovDAO motebehovDAO;

    @Before
    public void setUp() {
        loggInnBruker(oidcRequestContextHolder, LEDER_FNR);
        cleanDB();
    }

    @After
    public void tearDown() {
        loggUtAlle(oidcRequestContextHolder);
        cleanDB();
    }

    @Test
    public void lagreOgHentMotebehov() {
        final MotebehovSvar motebehovSvar = new MotebehovSvar()
                .harMotebehov(true)
                .friskmeldingForventning("Om en uke")
                .tiltak("Krykker")
                .tiltakResultat("Kommer seg fremover")
                .forklaring("");

        final NyttMotebehov nyttMotebehov = new NyttMotebehov()
                .arbeidstakerFnr(ARBEIDSTAKER_FNR)
                .virksomhetsnummer(VIRKSOMHETSNUMMER)
                .motebehovSvar(
                        motebehovSvar
                )
                .tildeltEnhet(TILDELT_ENHET);

        // Lagre
        motebehovController.lagreMotebehov(nyttMotebehov);

        // Hent
        List<Motebehov> motebehovListe = motebehovController.hentMotebehovListe(ARBEIDSTAKER_FNR, VIRKSOMHETSNUMMER);
        assertThat(motebehovListe).size().isOne();

        Motebehov motebehov = motebehovListe.get(0);
        assertThat(motebehov.opprettetAv).isEqualTo(LEDER_AKTORID);
        assertThat(motebehov.arbeidstakerFnr).isEqualTo(ARBEIDSTAKER_FNR);
        assertThat(motebehov.virksomhetsnummer).isEqualTo(VIRKSOMHETSNUMMER);
        assertThat(motebehov.motebehovSvar).isEqualToComparingFieldByField(motebehovSvar);
    }


    private void cleanDB() {
        motebehovDAO.nullstillMotebehov(LEDER_AKTORID);
    }

}
