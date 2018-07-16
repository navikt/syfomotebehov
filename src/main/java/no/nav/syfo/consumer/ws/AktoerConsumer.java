package no.nav.syfo.consumer.ws;

import lombok.extern.slf4j.Slf4j;
import no.nav.tjeneste.virksomhet.aktoer.v2.AktoerV2;
import no.nav.tjeneste.virksomhet.aktoer.v2.HentAktoerIdForIdentPersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.aktoer.v2.HentIdentForAktoerIdPersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.aktoer.v2.meldinger.WSHentAktoerIdForIdentRequest;
import no.nav.tjeneste.virksomhet.aktoer.v2.meldinger.WSHentIdentForAktoerIdRequest;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

import static org.apache.commons.lang3.StringUtils.isBlank;

@Component
@Slf4j
public class AktoerConsumer  implements InitializingBean {

    private static AktoerConsumer instance;

    private final AktoerV2 aktoerV2;

    @Override
    public void afterPropertiesSet() throws Exception {
        instance = this;
    }

    public static AktoerConsumer aktoerConsumer() {
        return instance;
    }

    @Inject
    public AktoerConsumer(AktoerV2 aktoerV2) {
        this.aktoerV2 = aktoerV2;
    }

    public String hentAktoerIdForFnr(String fnr) {
        log.info("Henter aktoerid for fnr");
        try {
            String aktoerid = aktoerV2.hentAktoerIdForIdent(new WSHentAktoerIdForIdentRequest()
                    .withIdent(fnr)
            ).getAktoerId();
            log.info("Fant aktoerid for fnr: {}", aktoerid);
            return aktoerid;
        } catch (HentAktoerIdForIdentPersonIkkeFunnet e) {
            log.error("Fant ikke person med gitt fnr");
            throw new RuntimeException(e);
        }
    }

    public String hentFnrForAktoerId(String aktoerId) {
        log.info("Henter fnr for aktoerid");
        try {
            String aktoerid = aktoerV2.hentIdentForAktoerId(
                    new WSHentIdentForAktoerIdRequest()
                            .withAktoerId(aktoerId)
            ).getIdent();
            log.info("Fant fnr for aktoerid: {}", aktoerid);
            return aktoerid;
        } catch (HentIdentForAktoerIdPersonIkkeFunnet e) {
            log.error("Fant ikke person med gitt aktoerId");
            throw new RuntimeException(e);
        }
    }
}
