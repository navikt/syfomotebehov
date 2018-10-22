package no.nav.syfo.service;

import no.nav.syfo.consumer.ws.AktoerConsumer;
import no.nav.syfo.consumer.ws.BrukeroppgaveConsumer;
import no.nav.syfo.consumer.ws.PersonConsumer;
import no.nav.syfo.domain.rest.Brukeroppgave;
import no.nav.syfo.domain.rest.Fnr;
import no.nav.syfo.domain.rest.Historikk;
import no.nav.syfo.domain.rest.Motebehov;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.concat;
import static java.util.stream.Stream.empty;

@Service
public class HistorikkService {

    private final MotebehovService motebehovService;
    private final VeilederOppgaverService veilederOppgaverService;
    private final PersonConsumer personConsumer;
    private final BrukeroppgaveConsumer brukeroppgaveConsumer;
    private final AktoerConsumer aktoerConsumer;

    @Inject
    public HistorikkService(final MotebehovService motebehovService,
                            final VeilederOppgaverService veilederOppgaverService,
                            final PersonConsumer personConsumer,
                            final BrukeroppgaveConsumer brukeroppgaveConsumer,
                            final AktoerConsumer aktoerConsumer) {
        this.veilederOppgaverService = veilederOppgaverService;
        this.motebehovService = motebehovService;
        this.personConsumer = personConsumer;
        this.brukeroppgaveConsumer = brukeroppgaveConsumer;
        this.aktoerConsumer = aktoerConsumer;
    }

    public List<Historikk> hentHistorikkListe(final Fnr arbeidstakerFnr) {
        List<Motebehov> motebehovListe = motebehovService.hentMotebehovListe(arbeidstakerFnr);
        List<Brukeroppgave> brukeroppgaver = brukeroppgaveConsumer.hentBrukerOppgaver(aktoerConsumer.hentAktoerIdForFnr(arbeidstakerFnr.getFnr()));

        List<Historikk> utfoertHistorikk = veilederOppgaverService.get(arbeidstakerFnr.getFnr()).stream()
                .filter(veilederOppgave -> veilederOppgave.type.equals("MOTEBEHOV_MOTTATT") && veilederOppgave.status.equals("FERDIG"))
                .map(veilederOppgave -> new Historikk()
                        .tekst("Møtebehovet ble lest av " + veilederOppgave.sistEndretAv)
                        .tidspunkt(veilederOppgave.getSistEndret())
                )
                .collect(toList());

        Function<Motebehov, Historikk> fraMotebehovTilHistorikk = motebehov -> new Historikk()
                .opprettetAv(motebehov.opprettetAv)
                .tekst("Møtebehovet ble opprettet av " + personConsumer.hentNavnFraAktoerId(motebehov.opprettetAv()) + ".")
                .tidspunkt(motebehov.opprettetDato);

        List<Historikk> opprettetHistorikk = ofNullable(motebehovListe).map(f -> ofNullable(f.stream())
                .map(g -> g.map(fraMotebehovTilHistorikk))
                .orElse(empty())
                .collect(toList()))
                .orElse(new ArrayList<>());
        return concat(opprettetHistorikk.stream(), utfoertHistorikk.stream()).collect(toList());
    }

}
