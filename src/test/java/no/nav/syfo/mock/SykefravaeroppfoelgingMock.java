package no.nav.syfo.mock;

import no.nav.tjeneste.virksomhet.sykefravaersoppfoelging.v1.*;
import no.nav.tjeneste.virksomhet.sykefravaersoppfoelging.v1.informasjon.*;
import no.nav.tjeneste.virksomhet.sykefravaersoppfoelging.v1.meldinger.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

import static java.util.Arrays.asList;
import static no.nav.syfo.testhelper.UserConstants.ARBEIDSTAKER_AKTORID;
import static no.nav.syfo.testhelper.UserConstants.LEDER_AKTORID;

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
    }

    @Override
    public WSHentNaermesteLederResponse hentNaermesteLeder(WSHentNaermesteLederRequest request) throws HentNaermesteLederSikkerhetsbegrensning {
        return null;
    }

    @Override
    public WSHentNaermesteLedersAnsattListeResponse hentNaermesteLedersAnsattListe(WSHentNaermesteLedersAnsattListeRequest request) throws HentNaermesteLedersAnsattListeSikkerhetsbegrensning {
        return new WSHentNaermesteLedersAnsattListeResponse()
                .withAnsattListe(asList(
                        new WSAnsatt()
                                .withAktoerId(ARBEIDSTAKER_AKTORID)
                ));
    }

    @Override
    public WSHentNaermesteLederListeResponse hentNaermesteLederListe(WSHentNaermesteLederListeRequest request) throws HentNaermesteLederListeSikkerhetsbegrensning {
        return new WSHentNaermesteLederListeResponse()
                .withNaermesteLederListe(asList(
                        new WSNaermesteLeder()
                                .withNaermesteLederId(1111L)
                                .withNaermesteLederAktoerId("1000000000001")
                                .withNaermesteLederStatus(new WSNaermesteLederStatus()
                                        .withErAktiv(true)
                                )
                                .withNavn("Arbeidsgiver 1")
                                .withOrgnummer("1234")
                                .withEpost("test@nav.no")
                                .withMobil("11223344")
                ));
    }

    @Override
    public WSHentSykeforlopperiodeResponse hentSykeforlopperiode(WSHentSykeforlopperiodeRequest request) throws HentSykeforlopperiodeSikkerhetsbegrensning {
        return new WSHentSykeforlopperiodeResponse()
                .withSykeforlopperiodeListe(asList(
                        new WSSykeforlopperiode()
                                .withFom(LocalDate.now().minusDays(30))
                                .withTom(LocalDate.now().minusDays(14))
                                .withGrad(100)
                                .withAktivitet("Aktivitet"),
                        new WSSykeforlopperiode()
                                .withFom(LocalDate.now().minusDays(20))
                                .withTom(LocalDate.now().minusDays(4))
                                .withGrad(100)
                                .withAktivitet("Aktivitet")
                ));
    }

    @Override
    public WSHentHendelseListeResponse hentHendelseListe(WSHentHendelseListeRequest request) throws HentHendelseListeSikkerhetsbegrensning {
        return new WSHentHendelseListeResponse()
                .withHendelseListe(
                        new WSHendelse()
                                .withAktoerId(LEDER_AKTORID)
                                .withId(1L)
                                .withType("NAERMESTE_LEDER_SVAR_MOTEBEHOV")
                );
    }

}
