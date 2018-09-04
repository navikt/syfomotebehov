package no.nav.syfo.service;

import no.nav.syfo.consumer.ws.AktoerConsumer;
import no.nav.syfo.consumer.ws.PersonConsumer;
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
    private final PersonConsumer personConsumer;

    @Inject
    public HistorikkService(final MotebehovService motebehovService,
                            final VeilederOppgaverService veilederOppgaverService,
                            final PersonConsumer personConsumer) {
        this.veilederOppgaverService = veilederOppgaverService;
        this.motebehovService = motebehovService;
        this.personConsumer = personConsumer;
    }

    public List<Historikk> hentHistorikkListe(final Fnr arbeidstakerFnr) {
        List<Motebehov> motebehovListe = motebehovService.hentMotebehovListe(arbeidstakerFnr);

        List<Historikk> utfoertHistorikk = veilederOppgaverService.get(arbeidstakerFnr.getFnr()).stream()
                .filter(veilederOppgave -> veilederOppgave.type.equals("MOTEBEHOV_MOTTATT") && veilederOppgave.status.equals("FERDIG"))
                .map(veilederOppgave -> new Historikk()
                        .tekst("Møtebehovet ble lest av " + veilederOppgave.sistEndretAv)
                        .tidspunkt(veilederOppgave.getSistEndret())
                )
                .collect(toList());
        List<Historikk> opprettetHistorikk = mapListe(
                motebehovListe,
                motebehov -> new Historikk()
                        .opprettetAv(motebehov.opprettetAv)
                        .tekst("Møtebehovet ble opprettet av " + personConsumer.hentNavnFraAktoerId(motebehov.opprettetAv()) + ".")
                        .tidspunkt(motebehov.opprettetDato)
        );
        return concat(opprettetHistorikk.stream(), utfoertHistorikk.stream()).collect(toList());
    }
}
