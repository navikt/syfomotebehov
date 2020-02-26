package no.nav.syfo.controller;

import no.nav.security.oidc.context.OIDCRequestContextHolder;
import no.nav.syfo.LocalApplication;
import no.nav.syfo.aktorregister.AktorregisterConsumer;
import no.nav.syfo.aktorregister.domain.Fodselsnummer;
import no.nav.syfo.domain.rest.VeilederOppgaveFeedItem;
import no.nav.syfo.repository.dao.MotebehovDAO;
import no.nav.syfo.testhelper.MotebehovGenerator;
import org.junit.*;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import javax.inject.Inject;
import java.util.List;

import static java.time.LocalDateTime.now;
import static no.nav.syfo.testhelper.OidcTestHelper.loggInnBruker;
import static no.nav.syfo.testhelper.OidcTestHelper.loggUtAlle;
import static no.nav.syfo.testhelper.UserConstants.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = LocalApplication.class)
@DirtiesContext
public class MotebehovOppgaveFeedControllerTest {

    @Inject
    private MotebehovOppgaveFeedController motebehovOppgaveFeedController;

    @Inject
    private OIDCRequestContextHolder oidcRequestContextHolder;

    @Inject
    private MotebehovDAO motebehovDAO;

    @MockBean
    private AktorregisterConsumer aktorregisterConsumer;

    private MotebehovGenerator motebehovGenerator = new MotebehovGenerator();

    @Before
    public void setUp() {
        when(aktorregisterConsumer.getAktorIdForFodselsnummer(new Fodselsnummer(LEDER_FNR))).thenReturn(LEDER_AKTORID);
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
        motebehovDAO.create(motebehovGenerator.lagNyttPMotebehovFraAT(true));

        List<VeilederOppgaveFeedItem> veilederOppgaveFeedItemListe = hentVeilederoppgaver();

        assertThat(veilederOppgaveFeedItemListe).size().isOne();
    }

    @Test
    public void ikkeOpprettVeilederoppgaverFraMotebehovUtenBehov() {
        motebehovDAO.create(motebehovGenerator.lagNyttPMotebehovFraAT(false));

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
