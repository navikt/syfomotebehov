package no.nav.syfo.service;

import lombok.extern.slf4j.Slf4j;
import no.nav.syfo.consumer.ws.PersonConsumer;
import no.nav.syfo.domain.rest.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static no.nav.syfo.util.MapUtil.mapListe;

@Slf4j
@Service
public class HistorikkService {

    private final MotebehovService motebehovService;
    private final VeilederOppgaverService veilederOppgaverService;
    private final PersonConsumer personConsumer;

    public static final String HAR_SVART_PAA_MOTEBEHOV = " har svart på møtebehov";
    public static final String MOTEBEHOVET_BLE_LEST_AV = "Møtebehovet ble lest av ";

    @Inject
    public HistorikkService(
            MotebehovService motebehovService,
            VeilederOppgaverService veilederOppgaverService,
            PersonConsumer personConsumer
    ) {
        this.veilederOppgaverService = veilederOppgaverService;
        this.motebehovService = motebehovService;
        this.personConsumer = personConsumer;
    }

    public List<Historikk> hentHistorikkListe(final Fnr arbeidstakerFnr) {
        String fnr = arbeidstakerFnr.getFnr();

        List<Historikk> historikkListe = hentOpprettetMotebehov(arbeidstakerFnr);
        historikkListe.addAll(hentLesteMotebehovHistorikk(fnr));

        return historikkListe;
    }

    private List<Historikk> hentOpprettetMotebehov(Fnr arbeidstakerFnr) {
        List<Motebehov> motebehovListe = motebehovService.hentMotebehovListe(arbeidstakerFnr);

        return mapListe(
                motebehovListe,
                motebehov -> new Historikk()
                        .opprettetAv(motebehov.opprettetAv)
                        .tekst(personConsumer.hentNavnFraAktoerId(motebehov.opprettetAv) + HAR_SVART_PAA_MOTEBEHOV)
                        .tidspunkt(motebehov.opprettetDato)
        );
    }

    private List<Historikk> hentLesteMotebehovHistorikk(String sykmeldtFnr) {
        try {
            return veilederOppgaverService.get(sykmeldtFnr)
                    .stream()
                    .filter(veilederOppgave -> veilederOppgave.type.equals("MOTEBEHOV_MOTTATT") && veilederOppgave.status.equals("FERDIG"))
                    .map(veilederOppgave -> new Historikk()
                            .tekst(MOTEBEHOVET_BLE_LEST_AV + veilederOppgave.sistEndretAv)
                            .tidspunkt(veilederOppgave.getSistEndretAsLocalDateTime())
                    )
                    .collect(toList());
        } catch (RestClientException e) {
            log.error("Klarte ikke hente ut varselhistorikk på leste møtebehov");
            return new ArrayList<>();
        }
    }

}
