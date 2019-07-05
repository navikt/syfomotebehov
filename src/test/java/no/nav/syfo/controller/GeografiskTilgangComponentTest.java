package no.nav.syfo.controller;

import no.nav.syfo.consumer.ws.OrganisasjonEnhetConsumer;
import no.nav.syfo.consumer.ws.PersonConsumer;
import no.nav.syfo.service.GeografiskTilgangService;
import no.nav.syfo.util.Toggle;
import no.nav.tjeneste.virksomhet.organisasjonenhet.v2.FinnNAVKontorUgyldigInput;
import no.nav.tjeneste.virksomhet.organisasjonenhet.v2.OrganisasjonEnhetV2;
import no.nav.tjeneste.virksomhet.organisasjonenhet.v2.informasjon.WSEnhetsstatus;
import no.nav.tjeneste.virksomhet.organisasjonenhet.v2.informasjon.WSOrganisasjonsenhet;
import no.nav.tjeneste.virksomhet.organisasjonenhet.v2.meldinger.WSFinnNAVKontorResponse;
import no.nav.tjeneste.virksomhet.person.v3.binding.*;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Kommune;
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentGeografiskTilknytningResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.test.context.junit4.SpringRunner;

import javax.ws.rs.ForbiddenException;
import javax.ws.rs.core.Response;

import static no.nav.syfo.mock.PersonMock.GEOGRAFISK_TILKNYTNING;
import static no.nav.syfo.testhelper.UserConstants.ARBEIDSTAKER_FNR;
import static no.nav.syfo.testhelper.UserConstants.NAV_ENHET_NAVN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;


@RunWith(SpringRunner.class)
public class GeografiskTilgangComponentTest {

    @Mock
    private PersonV3 personV3Mock;

    @Mock
    private OrganisasjonEnhetV2 organisasjonEnhetV2Mock;

    private GeografiskTilgangController geografiskTilgangController;

    private static final String PILOTKONTOR = "0330";
    private static final String IKKE_PILOTKONTOR = "1234";

    @Before
    public void setup() {
        Toggle.pilotKontorer = PILOTKONTOR;
        Toggle.endepunkterForMotebehov = true;
        Toggle.enableMotebehovNasjonal = false;

        OrganisasjonEnhetConsumer organisasjonEnhetConsumer = new OrganisasjonEnhetConsumer(organisasjonEnhetV2Mock);
        PersonConsumer personConsumer = new PersonConsumer(personV3Mock);
        GeografiskTilgangService geografiskTilgangService = new GeografiskTilgangService(personConsumer, organisasjonEnhetConsumer);
        geografiskTilgangController = new GeografiskTilgangController(geografiskTilgangService);
    }

    @Test
    public void hentGeografiskTilgang() {
        Toggle.enableMotebehovNasjonal = true;
        mockHentPilotkontorFraOrganisasjonEnhet();

        Response response = geografiskTilgangController.hentGeografiskTilgang(ARBEIDSTAKER_FNR);

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    public void harTilgangMedNasjonalLanseringUavhengigGeografiskTilgang() {
        Toggle.enableMotebehovNasjonal = true;
        mockHentIkkePilotkontorFraOrganisasjonEnhet();

        Response response = geografiskTilgangController.hentGeografiskTilgang(ARBEIDSTAKER_FNR);

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test(expected = ForbiddenException.class)
    public void hentGeografiskTilgangIkkePilot() {
        mockHentIkkePilotkontorFraOrganisasjonEnhet();

        geografiskTilgangController.hentGeografiskTilgang(ARBEIDSTAKER_FNR);
    }

    @Test(expected = ForbiddenException.class)
    public void hentGeografiskTilgangToggleSkruddAv() {
        mockHentIkkePilotkontorFraOrganisasjonEnhet();
        Toggle.endepunkterForMotebehov = false;

        geografiskTilgangController.hentGeografiskTilgang(ARBEIDSTAKER_FNR);
    }

    @Test(expected = ForbiddenException.class)
    public void hentGeografiskTilgangGeografiskTilknytningErNull() {
        mockGeografiskTilknytningErNull();

        geografiskTilgangController.hentGeografiskTilgang(ARBEIDSTAKER_FNR);
    }

    private void mockHentPilotkontorFraOrganisasjonEnhet() {
        try {
            when(personV3Mock.hentGeografiskTilknytning(any())).thenReturn(new HentGeografiskTilknytningResponse()
                    .withGeografiskTilknytning(new Kommune()
                            .withGeografiskTilknytning(GEOGRAFISK_TILKNYTNING)));

            when(organisasjonEnhetV2Mock.finnNAVKontor(any())).thenReturn(new WSFinnNAVKontorResponse()
                    .withNAVKontor(new WSOrganisasjonsenhet()
                            .withEnhetId(PILOTKONTOR)
                            .withEnhetNavn(NAV_ENHET_NAVN)
                            .withStatus(WSEnhetsstatus.AKTIV)));
        } catch (HentGeografiskTilknytningPersonIkkeFunnet | HentGeografiskTilknytningSikkerhetsbegrensing | FinnNAVKontorUgyldigInput e) {
            throw new RuntimeException(e);
        }
    }

    private void mockHentIkkePilotkontorFraOrganisasjonEnhet() {
        try {
            when(personV3Mock.hentGeografiskTilknytning(any())).thenReturn(new HentGeografiskTilknytningResponse()
                    .withGeografiskTilknytning(new Kommune()
                            .withGeografiskTilknytning(GEOGRAFISK_TILKNYTNING)));

            when(organisasjonEnhetV2Mock.finnNAVKontor(any())).thenReturn(new WSFinnNAVKontorResponse()
                    .withNAVKontor(new WSOrganisasjonsenhet()
                            .withEnhetId(IKKE_PILOTKONTOR)
                            .withEnhetNavn(NAV_ENHET_NAVN)
                            .withStatus(WSEnhetsstatus.AKTIV)));
        } catch (HentGeografiskTilknytningPersonIkkeFunnet | HentGeografiskTilknytningSikkerhetsbegrensing | FinnNAVKontorUgyldigInput e) {
            throw new RuntimeException(e);
        }
    }

    private void mockGeografiskTilknytningErNull() {
        try {
            when(personV3Mock.hentGeografiskTilknytning(any())).thenReturn(new HentGeografiskTilknytningResponse()
                    .withGeografiskTilknytning(null));
        } catch (HentGeografiskTilknytningPersonIkkeFunnet | HentGeografiskTilknytningSikkerhetsbegrensing e) {
            throw new RuntimeException(e);
        }
    }

}
