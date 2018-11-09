package no.nav.syfo.consumer.ws;

import lombok.extern.slf4j.Slf4j;
import no.nav.syfo.mappers.domain.Brukeroppgave;
import no.nav.tjeneste.domene.digisyfo.brukeroppgave.v1.BrukeroppgaveV1;
import no.nav.tjeneste.domene.digisyfo.brukeroppgave.v1.informasjon.WSBrukeroppgave;
import no.nav.tjeneste.domene.digisyfo.brukeroppgave.v1.meldinger.WSHentBrukeroppgaveListeRequest;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.List;

import static no.nav.syfo.mappers.WSBrukeroppgaveMapper.ws2Brukeroppgave;
import static no.nav.syfo.util.MapUtil.mapListe;
import static org.apache.commons.lang3.StringUtils.isBlank;

@Component
@Slf4j
public class BrukeroppgaveConsumer implements InitializingBean {

    private static BrukeroppgaveConsumer instance;
    private final BrukeroppgaveV1 brukeroppgaveV1;

    @Override
    public void afterPropertiesSet() {
        instance = this;
    }

    public static BrukeroppgaveConsumer brukeroppgaveConsumer() {
        return instance;
    }

    @Inject
    public BrukeroppgaveConsumer(BrukeroppgaveV1 brukeroppgaveV1) {
        this.brukeroppgaveV1 = brukeroppgaveV1;
    }

    public List<Brukeroppgave> hentBrukerOppgaver(String aktorId) {
        if (isBlank(aktorId) || !aktorId.matches("\\d{13}$")) {
            log.error("Ugyldig format på aktorId: " + aktorId);
            throw new IllegalArgumentException();
        }

        try {
            List<WSBrukeroppgave> brukerOppgaver = brukeroppgaveV1.hentBrukeroppgaveListe(
                    new WSHentBrukeroppgaveListeRequest().withIdent(aktorId)
            ).getBrukeroppgaveListe();

            return mapListe(brukerOppgaver, ws2Brukeroppgave);
        } catch (RuntimeException e) {
            throw e;
        }
    }

}
