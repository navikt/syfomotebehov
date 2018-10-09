package no.nav.syfo.mock;

import no.nav.tjeneste.virksomhet.aktoer.v2.AktoerV2;
import no.nav.tjeneste.virksomhet.aktoer.v2.meldinger.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(value = "mockAktoer_V2", havingValue = "true")
public class AktoerMock implements AktoerV2 {

    private static final String MOCK_AKTORID_PREFIX = "10";

    public WSHentAktoerIdForIdentListeResponse hentAktoerIdForIdentListe(WSHentAktoerIdForIdentListeRequest wsHentAktoerIdForIdentListeRequest) {
        throw new RuntimeException("Ikke implementert i mock");
    }

    public WSHentAktoerIdForIdentResponse hentAktoerIdForIdent(WSHentAktoerIdForIdentRequest wsHentAktoerIdForIdentRequest) {
        return new WSHentAktoerIdForIdentResponse()
                .withAktoerId(mockAktorId(wsHentAktoerIdForIdentRequest.getIdent()));
    }

    public WSHentIdentForAktoerIdListeResponse hentIdentForAktoerIdListe(WSHentIdentForAktoerIdListeRequest wsHentIdentForAktoerIdListeRequest) {
        throw new RuntimeException("Ikke implementert i mock.");
    }

    public WSHentIdentForAktoerIdResponse hentIdentForAktoerId(WSHentIdentForAktoerIdRequest wsHentIdentForAktoerIdRequest) {
        return new WSHentIdentForAktoerIdResponse()
                .withIdent(getFnrFromMockedAktorId(wsHentIdentForAktoerIdRequest.getAktoerId()));
    }

    public void ping() { }

    public static String mockAktorId(String fnr) {
        return MOCK_AKTORID_PREFIX.concat(fnr);
    }

    private static String getFnrFromMockedAktorId(String aktorId) {
        return aktorId.replace(MOCK_AKTORID_PREFIX, "");
    }

}
