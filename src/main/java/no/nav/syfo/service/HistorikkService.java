package no.nav.syfo.service;

import no.nav.syfo.consumer.ws.AktoerConsumer;
import no.nav.syfo.consumer.ws.BrukerprofilConsumer;
import no.nav.syfo.domain.rest.Fnr;
import no.nav.syfo.domain.rest.Historikk;
import no.nav.syfo.domain.rest.Motebehov;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.concat;
import static no.nav.syfo.util.MapUtil.mapListe;

@Service
public class HistorikkService {

    private final MotebehovService motebehovService;
    private final VeilederOppgaverService veilederOppgaverService;
    private final BrukerprofilConsumer brukerprofilConsumer;
    private final AktoerConsumer aktoerConsumer;

    @Inject
    public HistorikkService(final MotebehovService motebehovService,
                            final VeilederOppgaverService veilederOppgaverService,
                            final BrukerprofilConsumer brukerprofilConsumer,
                            final AktoerConsumer aktoerConsumer) {
        this.veilederOppgaverService = veilederOppgaverService;
        this.motebehovService = motebehovService;
        this.brukerprofilConsumer = brukerprofilConsumer;
        this.aktoerConsumer = aktoerConsumer;
    }

    public List<Historikk> hentHistorikkListe(final Fnr arbeidstakerFnr) {
        List<Motebehov> motebehovListe = motebehovService.hentMotebehovListe(arbeidstakerFnr);

        List<Historikk> utfoertHistorikk = veilederOppgaverService.get(arbeidstakerFnr.getFnr()).stream()
                .filter(veilederOppgave -> veilederOppgave.status.equals("FERDIG"))
                .map(veilederOppgave -> new Historikk()
                        .tekst("Møtebehovet ble lest av " + veilederOppgave.sistEndretAv)
                        .tidspunkt(veilederOppgave.getSistEndret())
                )
                .collect(toList());
        List<Historikk> opprettetHistorikk = mapListe(
                motebehovListe,
                motebehov -> new Historikk()
                        .tekst("Møtebehovet ble opprettet av " + brukerprofilConsumer.hentBrukersNavn(aktoerConsumer.hentFnrForAktoerId(motebehov.opprettetAv())) + ".")
                        .tidspunkt(motebehov.opprettetDato)
        );
        return concat(opprettetHistorikk.stream(), utfoertHistorikk.stream()).collect(toList());
    }
}
