package no.nav.syfo.mock;

import no.nav.tjeneste.domene.digisyfo.brukeroppgave.v1.BrukeroppgaveV1;
import no.nav.tjeneste.domene.digisyfo.brukeroppgave.v1.informasjon.WSBrukeroppgave;
import no.nav.tjeneste.domene.digisyfo.brukeroppgave.v1.meldinger.WSHentBrukeroppgaveListeRequest;
import no.nav.tjeneste.domene.digisyfo.brukeroppgave.v1.meldinger.WSHentBrukeroppgaveListeResponse;
import no.nav.tjeneste.domene.digisyfo.brukeroppgave.v1.meldinger.WSSlettBrukeroppgaveRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.UUID;

@Service
@ConditionalOnProperty(value = "mockBrukeroppgave_V1", havingValue = "true")
public class BrukeroppgaveMock implements BrukeroppgaveV1 {

    @Override
    public void ping() { return; }

    @Override
    public WSHentBrukeroppgaveListeResponse hentBrukeroppgaveListe(WSHentBrukeroppgaveListeRequest wsHentBrukeroppgaveListeRequest) {
        return new WSHentBrukeroppgaveListeResponse().withBrukeroppgaveListe(Arrays.asList(
                new WSBrukeroppgave()
                        .withOppgaveUuid(UUID.randomUUID().toString())
                        .withOpprettettidspunkt(LocalDateTime.now())
                        .withOppgavetype("NAERMESTE_LEDER_LES_SYKMELDING")
                        .withRessursId("sykmelding1")
                        .withRessurseier("1"),
                new WSBrukeroppgave()
                        .withOppgaveUuid(UUID.randomUUID().toString())
                        .withOpprettettidspunkt(LocalDateTime.now().minusMinutes(2))
                        .withOppgavetype("NAERMESTE_LEDER_SVAR_MOTEBEHOV")
                        .withRessursId("motebehov2")
                        .withRessurseier("1")
                )
        );
    }

    @Override
    public void slettBrukeroppgave(WSSlettBrukeroppgaveRequest wsSlettBrukeroppgaveRequest) { return; }

}