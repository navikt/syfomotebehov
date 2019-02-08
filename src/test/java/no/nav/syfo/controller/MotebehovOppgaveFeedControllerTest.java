package no.nav.syfo.controller;

import no.nav.security.oidc.context.OIDCRequestContextHolder;
import no.nav.syfo.LocalApplication;
import no.nav.syfo.domain.rest.MotebehovSvar;
import no.nav.syfo.domain.rest.NyttMotebehov;
import no.nav.syfo.domain.rest.VeilederOppgaveFeedItem;
import no.nav.syfo.repository.dao.MotebehovDAO;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.inject.Inject;
import java.util.List;

import static java.time.LocalDateTime.now;
import static no.nav.syfo.controller.MotebehovComponentTest.*;
import static no.nav.syfo.util.OidcTestHelper.loggInnBruker;
import static no.nav.syfo.util.OidcTestHelper.loggUtAlle;
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
        motebehovBrukerController.lagreMotebehov(sykmeldtMotebehovSvar(true));
        List<VeilederOppgaveFeedItem> veilederOppgaveFeedItemListe = hentVeilederoppgaver();

        assertThat(veilederOppgaveFeedItemListe).size().isOne();
    }

    @Test
    public void ikkeOpprettVeilederoppgaverFraMotebehovUtenBehov() {
        motebehovBrukerController.lagreMotebehov(sykmeldtMotebehovSvar(false));
        List<VeilederOppgaveFeedItem> veilederOppgaveFeedItemListe = hentVeilederoppgaver();

        assertThat(veilederOppgaveFeedItemListe).size().isZero();
    }

    private NyttMotebehov sykmeldtMotebehovSvar(boolean harMotebehov) {
        final MotebehovSvar motebehovSvar = new MotebehovSvar()
                .harMotebehov(harMotebehov)
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
        return nyttMotebehov;
    }

    private List<VeilederOppgaveFeedItem> hentVeilederoppgaver() {
        String dato = now().minusDays(1).toString();
        return motebehovOppgaveFeedController.hentMotebehovListe(dato);
    }


    private void cleanDB() {
        motebehovDAO.nullstillMotebehov(ARBEIDSTAKER_AKTORID);
    }

}
