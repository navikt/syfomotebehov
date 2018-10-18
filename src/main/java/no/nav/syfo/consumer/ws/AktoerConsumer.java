package no.nav.syfo.consumer.ws;

import lombok.extern.slf4j.Slf4j;
import no.nav.tjeneste.virksomhet.aktoer.v2.AktoerV2;
import no.nav.tjeneste.virksomhet.aktoer.v2.HentAktoerIdForIdentPersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.aktoer.v2.HentIdentForAktoerIdPersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.aktoer.v2.meldinger.WSHentAktoerIdForIdentRequest;
import no.nav.tjeneste.virksomhet.aktoer.v2.meldinger.WSHentIdentForAktoerIdRequest;
import org.springframework.beans.factory.InitializingBean;
//import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
@Slf4j
public class AktoerConsumer implements InitializingBean {

    private static AktoerConsumer instance;

    private final AktoerV2 aktoerV2;

    @Override
    public void afterPropertiesSet() {
        instance = this;
    }

    public static AktoerConsumer aktoerConsumer() {
        return instance;
    }

    @Inject
    public AktoerConsumer(AktoerV2 aktoerV2) {
        this.aktoerV2 = aktoerV2;
    }

//    @Cacheable("aktoerid")
    public String hentAktoerIdForFnr(String fnr) {
        log.info("JTRACE: hentAktoerIdForFnr med fnr {}", fnr);
        try {
            return aktoerV2.hentAktoerIdForIdent(new WSHentAktoerIdForIdentRequest()
                    .withIdent(fnr)
            ).getAktoerId();
        } catch (HentAktoerIdForIdentPersonIkkeFunnet e) {
            log.error("Fant ikke person med gitt fnr");
            throw new RuntimeException(e);
        }
    }

//    @Cacheable("aktoerfnr")
    public String hentFnrForAktoerId(String aktoerId) {
        log.info("JTRACE: hentFnrForAktoerId med aktoerId {}", aktoerId);
        try {
            return aktoerV2.hentIdentForAktoerId(
                    new WSHentIdentForAktoerIdRequest()
                            .withAktoerId(aktoerId)
            ).getIdent();
        } catch (HentIdentForAktoerIdPersonIkkeFunnet e) {
            log.error("Fant ikke person med aktoerId: " + aktoerId);
            throw new RuntimeException(e);
        }
    }
}
