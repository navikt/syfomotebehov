package no.nav.syfo.mock;

import no.nav.tjeneste.virksomhet.sykefravaersoppfoelging.v1.*;
import no.nav.tjeneste.virksomhet.sykefravaersoppfoelging.v1.meldinger.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(value = "mockSykefravaeroppfoelging_V1", havingValue = "true")
public class SykefravaeroppfoelgingMock implements SykefravaersoppfoelgingV1 {

    @Override
    public WSHentNaermesteLedersHendelseListeResponse hentNaermesteLedersHendelseListe(WSHentNaermesteLedersHendelseListeRequest request) throws HentNaermesteLedersHendelseListeSikkerhetsbegrensning {
        return null;
    }

    @Override
    public WSBerikNaermesteLedersAnsattBolkResponse berikNaermesteLedersAnsattBolk(WSBerikNaermesteLedersAnsattBolkRequest request) throws BerikNaermesteLedersAnsattBolkSikkerhetsbegrensning {
        return null;
    }

    @Override
    public void ping() {
        return;
    }

    @Override
    public WSHentNaermesteLederResponse hentNaermesteLeder(WSHentNaermesteLederRequest request) throws HentNaermesteLederSikkerhetsbegrensning {
        return null;
    }

    @Override
    public WSHentNaermesteLedersAnsattListeResponse hentNaermesteLedersAnsattListe(WSHentNaermesteLedersAnsattListeRequest request) throws HentNaermesteLedersAnsattListeSikkerhetsbegrensning {
        return null;
    }

    @Override
    public WSHentNaermesteLederListeResponse hentNaermesteLederListe(WSHentNaermesteLederListeRequest request) throws HentNaermesteLederListeSikkerhetsbegrensning {
        return null;
    }

    @Override
    public WSHentSykeforlopperiodeResponse hentSykeforlopperiode(WSHentSykeforlopperiodeRequest request) throws HentSykeforlopperiodeSikkerhetsbegrensning {
        return null;
    }

    @Override
    public WSHentHendelseListeResponse hentHendelseListe(WSHentHendelseListeRequest request) throws HentHendelseListeSikkerhetsbegrensning {
        return null;
    }

}
