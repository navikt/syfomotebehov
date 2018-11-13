package no.nav.syfo.consumer.ws;

import lombok.extern.slf4j.Slf4j;
import no.nav.tjeneste.pip.egen.ansatt.v1.EgenAnsattV1;
import no.nav.tjeneste.pip.egen.ansatt.v1.WSHentErEgenAnsattEllerIFamilieMedEgenAnsattRequest;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
@Slf4j
public class EgenAnsattConsumer implements InitializingBean {
    private static EgenAnsattConsumer instance;
    private final EgenAnsattV1 egenAnsattV1;

    @Override
    public void afterPropertiesSet() { instance = this; }

    public static EgenAnsattConsumer egenAnsattConsumer() { return instance; }

    @Inject
    public EgenAnsattConsumer(EgenAnsattV1 egenAnsattV1) {
        this.egenAnsattV1 = egenAnsattV1;
    }

    public boolean erEgenAnsatt(String fnr) {
        try {
            return egenAnsattV1.hentErEgenAnsattEllerIFamilieMedEgenAnsatt(new WSHentErEgenAnsattEllerIFamilieMedEgenAnsattRequest()
                    .withIdent(fnr)
            ).isEgenAnsatt();
        } catch (RuntimeException e) {
            log.error("Klarte ikke hente egenansatt status på sykmeldt");
            throw e;
        }
    }

}
