package no.nav.syfo.controller;

import no.nav.security.oidc.context.OIDCRequestContextHolder;
import no.nav.syfo.LocalApplication;
import no.nav.syfo.domain.rest.VeilederOppgaveFeedItem;
import no.nav.syfo.repository.dao.MotebehovDAO;
import no.nav.syfo.testhelper.MotebehovGenerator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.inject.Inject;
import java.util.List;

import static java.time.LocalDateTime.now;
import static no.nav.syfo.testhelper.OidcTestHelper.loggInnBruker;
import static no.nav.syfo.testhelper.OidcTestHelper.loggUtAlle;
import static no.nav.syfo.testhelper.UserConstants.ARBEIDSTAKER_AKTORID;
import static no.nav.syfo.testhelper.UserConstants.ARBEIDSTAKER_FNR;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = LocalApplication.class)
public class MotebehovOppgaveFeedControllerTest {

    @Inject
    private MotebehovOppgaveFeedController motebehovOppgaveFeedController;

    @Inject
    private OIDCRequestContextHolder oidcRequestContextHolder;

    @Inject
    private MotebehovBrukerController motebehovBrukerController;

    @Inject
    private MotebehovDAO motebehovDAO;

    private MotebehovGenerator motebehovGenerator = new MotebehovGenerator();

    @Before
    public void setUp() {
        loggInnBruker(oidcRequestContextHolder, ARBEIDSTAKER_FNR);
        cleanDB();
    }

    @After
    public void tearDown() {
        loggUtAlle(oidcRequestContextHolder);
        cleanDB();
    }

    @Test
    public void opprettVeilederoppgaverFraMotebehovMedBehov() {
        motebehovBrukerController.lagreMotebehov(motebehovGenerator.lagNyttMotebehovFraAT(true));
        List<VeilederOppgaveFeedItem> veilederOppgaveFeedItemListe = hentVeilederoppgaver();

        assertThat(veilederOppgaveFeedItemListe).size().isOne();
    }

    @Test
    public void ikkeOpprettVeilederoppgaverFraMotebehovUtenBehov() {
        motebehovBrukerController.lagreMotebehov(motebehovGenerator.lagNyttMotebehovFraAT(false));
        List<VeilederOppgaveFeedItem> veilederOppgaveFeedItemListe = hentVeilederoppgaver();

        assertThat(veilederOppgaveFeedItemListe).size().isZero();
    }

    private List<VeilederOppgaveFeedItem> hentVeilederoppgaver() {
        String dato = now().minusDays(1).toString();
        return motebehovOppgaveFeedController.hentMotebehovListe(dato);
    }


    private void cleanDB() {
        motebehovDAO.nullstillMotebehov(ARBEIDSTAKER_AKTORID);
    }

}
