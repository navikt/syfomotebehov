package no.nav.syfo.controller;

import no.nav.security.oidc.context.OIDCRequestContextHolder;
import no.nav.syfo.LocalApplication;
import no.nav.syfo.domain.rest.Motebehov;
import no.nav.syfo.domain.rest.MotebehovSvar;
import no.nav.syfo.repository.dao.MotebehovDAO;
import no.nav.syfo.testhelper.MotebehovGenerator;
import org.junit.*;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import javax.inject.Inject;
import java.util.List;

import static no.nav.syfo.testhelper.OidcTestHelper.loggInnBruker;
import static no.nav.syfo.testhelper.OidcTestHelper.loggUtAlle;
import static no.nav.syfo.testhelper.UserConstants.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Komponent / blackbox test av møtebehovsfunskjonaliteten - test at input til endepunktet (controlleren, for enkelhets skyld)
 * lagres og hentes riktig fra minnedatabasen.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = LocalApplication.class)
@DirtiesContext
public class MotebehovComponentTest {

    @Inject
    private MotebehovBrukerController motebehovController;

    @Inject
    private OIDCRequestContextHolder oidcRequestContextHolder;

    @Inject
    private MotebehovDAO motebehovDAO;

    private MotebehovGenerator motebehovGenerator = new MotebehovGenerator();

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
        final MotebehovSvar motebehovSvar = motebehovGenerator.lagMotebehovSvar(true);

        // Lagre
        motebehovController.lagreMotebehov(motebehovGenerator.lagNyttMotebehovFraAT());

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
