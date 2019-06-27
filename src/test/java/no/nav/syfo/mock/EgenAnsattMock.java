package no.nav.syfo.mock;

import no.nav.tjeneste.pip.egen.ansatt.v1.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(value = "mockEgenAnsatt_V1", havingValue = "true")
public class EgenAnsattMock implements EgenAnsattV1 {

    @Override
    public void ping() {
    }

    @Override
    public WSHentErEgenAnsattEllerIFamilieMedEgenAnsattResponse hentErEgenAnsattEllerIFamilieMedEgenAnsatt(WSHentErEgenAnsattEllerIFamilieMedEgenAnsattRequest req) {
        return new WSHentErEgenAnsattEllerIFamilieMedEgenAnsattResponse()
                .withEgenAnsatt(false);
    }


}
