package no.nav.syfo.mock;

import no.nav.tjeneste.virksomhet.aktoer.v2.AktoerV2;
import no.nav.tjeneste.virksomhet.aktoer.v2.HentAktoerIdForIdentPersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.aktoer.v2.HentIdentForAktoerIdPersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.aktoer.v2.meldinger.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(value = "mockAktoer_V2", havingValue = "true")
public class AktoerMock implements AktoerV2 {

    public WSHentAktoerIdForIdentListeResponse hentAktoerIdForIdentListe(WSHentAktoerIdForIdentListeRequest wsHentAktoerIdForIdentListeRequest) {
        throw new RuntimeException("Ikke implementert i mock");
    }

    public WSHentAktoerIdForIdentResponse hentAktoerIdForIdent(WSHentAktoerIdForIdentRequest wsHentAktoerIdForIdentRequest) throws HentAktoerIdForIdentPersonIkkeFunnet {
        return new WSHentAktoerIdForIdentResponse()
                .withAktoerId("aktorId");
    }

    public WSHentIdentForAktoerIdListeResponse hentIdentForAktoerIdListe(WSHentIdentForAktoerIdListeRequest wsHentIdentForAktoerIdListeRequest) {
        throw new RuntimeException("Ikke implementert i mock.");
    }

    public WSHentIdentForAktoerIdResponse hentIdentForAktoerId(WSHentIdentForAktoerIdRequest wsHentIdentForAktoerIdRequest) throws HentIdentForAktoerIdPersonIkkeFunnet {
        throw new RuntimeException("Ikke implementert i mock.");
    }

    public void ping() { }
}
