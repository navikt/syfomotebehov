package no.nav.syfo.mock;

import no.nav.tjeneste.virksomhet.organisasjon.v4.*;
import no.nav.tjeneste.virksomhet.organisasjon.v4.informasjon.WSOrganisasjon;
import no.nav.tjeneste.virksomhet.organisasjon.v4.informasjon.WSOrganisasjonsDetaljer;
import no.nav.tjeneste.virksomhet.organisasjon.v4.informasjon.WSOrganisasjonsnavn;
import no.nav.tjeneste.virksomhet.organisasjon.v4.informasjon.WSUstrukturertNavn;
import no.nav.tjeneste.virksomhet.organisasjon.v4.meldinger.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.XMLGregorianCalendar;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.GregorianCalendar;

import static javax.xml.datatype.DatatypeFactory.newInstance;

@Service
@ConditionalOnProperty(value = "mockOrganisasjon_V4", havingValue = "true")
public class OrganisasjonMock implements OrganisasjonV4 {

    @Override
    public WSFinnOrganisasjonResponse finnOrganisasjon(WSFinnOrganisasjonRequest request) throws FinnOrganisasjonForMangeForekomster, FinnOrganisasjonUgyldigInput {
        throw new RuntimeException("Ikke implementert i mock. Se OrganisasjonMock");
    }

    @Override
    public WSHentOrganisasjonsnavnBolkResponse hentOrganisasjonsnavnBolk(WSHentOrganisasjonsnavnBolkRequest request) {
        throw new RuntimeException("Ikke implementert i mock. Se OrganisasjonMock");
    }

    @Override
    public WSHentOrganisasjonResponse hentOrganisasjon(WSHentOrganisasjonRequest request) throws HentOrganisasjonOrganisasjonIkkeFunnet, HentOrganisasjonUgyldigInput {
        return new WSHentOrganisasjonResponse()
                .withOrganisasjon(new WSOrganisasjon()
                        .withOrgnummer("000111222")
                        .withNavn(new WSUstrukturertNavn()
                                .withNavnelinje("NAV AS")
                                .withNavnelinje("Avdeling Økernveien"))
                        .withOrganisasjonDetaljer(new WSOrganisasjonsDetaljer()
                                .withOpphoersdato(null)
                                .withNavn(new WSOrganisasjonsnavn()
                                        .withNavn(new WSUstrukturertNavn().withNavnelinje("NAV AS"))
                                        .withFomGyldighetsperiode(convertToXmlGregorianCalendar(LocalDate.of(2013, 5, 6)))))
                );
    }

    @Override
    public WSHentNoekkelinfoOrganisasjonResponse hentNoekkelinfoOrganisasjon(WSHentNoekkelinfoOrganisasjonRequest request)
            throws HentNoekkelinfoOrganisasjonOrganisasjonIkkeFunnet, HentNoekkelinfoOrganisasjonUgyldigInput {
        throw new RuntimeException("Ikke implementert i mock. Se OrganisasjonMock");
    }

    @Override
    public WSValiderOrganisasjonResponse validerOrganisasjon(WSValiderOrganisasjonRequest request) throws ValiderOrganisasjonOrganisasjonIkkeFunnet, ValiderOrganisasjonUgyldigInput {
        throw new RuntimeException("Ikke implementert i mock. Se OrganisasjonMock");
    }

    @Override
    public WSHentVirksomhetsOrgnrForJuridiskOrgnrBolkResponse hentVirksomhetsOrgnrForJuridiskOrgnrBolk(WSHentVirksomhetsOrgnrForJuridiskOrgnrBolkRequest request) {
        throw new RuntimeException("Ikke implementert i mock. Se OrganisasjonMock");
    }

    @Override
    public WSFinnOrganisasjonsendringerListeResponse finnOrganisasjonsendringerListe(WSFinnOrganisasjonsendringerListeRequest request)
            throws FinnOrganisasjonsendringerListeUgyldigInput {
        throw new RuntimeException("Ikke implementert i mock. Se OrganisasjonMock");
    }

    @Override
    public void ping() {
    }

    private XMLGregorianCalendar convertToXmlGregorianCalendar(LocalDate dato) {
        try {
            GregorianCalendar gregorianCalendar = GregorianCalendar.from(dato.atStartOfDay(ZoneId.systemDefault()));
            return newInstance().newXMLGregorianCalendar(gregorianCalendar);
        } catch (DatatypeConfigurationException dce) {
            throw new RuntimeException(dce);
        }
    }
}
