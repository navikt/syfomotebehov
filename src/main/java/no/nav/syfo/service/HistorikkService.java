package no.nav.syfo.service;

import lombok.extern.slf4j.Slf4j;
import no.nav.syfo.OIDCIssuer;
import no.nav.syfo.consumer.ws.*;
import no.nav.syfo.domain.rest.*;
import no.nav.syfo.mappers.domain.Brukeroppgave;
import no.nav.syfo.mappers.domain.Hendelse;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static no.nav.syfo.util.MapUtil.mapListe;

@Slf4j
@Service
public class HistorikkService {

    private final MotebehovService motebehovService;
    private final VeilederOppgaverService veilederOppgaverService;
    private final PersonConsumer personConsumer;
    private final BrukeroppgaveConsumer brukeroppgaveConsumer;
    private final AktoerConsumer aktoerConsumer;
    private final SykefravaeroppfoelgingConsumer sykefravaeroppfoelgingConsumer;
    private final OrganisasjonConsumer organisasjonConsumer;

    @Inject
    public HistorikkService(final MotebehovService motebehovService,
                            final VeilederOppgaverService veilederOppgaverService,
                            final PersonConsumer personConsumer,
                            final BrukeroppgaveConsumer brukeroppgaveConsumer,
                            final AktoerConsumer aktoerConsumer,
                            final SykefravaeroppfoelgingConsumer sykefravaeroppfoelgingConsumer,
                            final OrganisasjonConsumer organisasjonConsumer) {
        this.veilederOppgaverService = veilederOppgaverService;
        this.motebehovService = motebehovService;
        this.personConsumer = personConsumer;
        this.brukeroppgaveConsumer = brukeroppgaveConsumer;
        this.aktoerConsumer = aktoerConsumer;
        this.sykefravaeroppfoelgingConsumer = sykefravaeroppfoelgingConsumer;
        this.organisasjonConsumer = organisasjonConsumer;
    }

    public List<Historikk> hentHistorikkListe(final Fnr arbeidstakerFnr) {
        String fnr = arbeidstakerFnr.getFnr();

        List<Historikk> historikkListe = hentOpprettetMotebehov(arbeidstakerFnr);
        historikkListe.addAll(hentLesteMotebehovHistorikk(fnr));
        historikkListe.addAll(hentNLMotebehovVarselHistorikk(fnr));

        return historikkListe;
    }

    private List<Historikk> hentOpprettetMotebehov(Fnr arbeidstakerFnr) {
        List<Motebehov> motebehovListe = motebehovService.hentMotebehovListe(arbeidstakerFnr);

        return mapListe(
                motebehovListe,
                motebehov -> new Historikk()
                        .opprettetAv(motebehov.opprettetAv)
                        .tekst("Møtebehovet ble opprettet av " + personConsumer.hentNavnFraAktoerId(motebehov.opprettetAv()) + ".")
                        .tidspunkt(motebehov.opprettetDato)
        );
    }

    private List<Historikk> hentNLMotebehovVarselHistorikk(String sykmeldtFnr) {
        try {
            String aktorId = aktoerConsumer.hentAktoerIdForFnr(sykmeldtFnr);
            List<NaermesteLeder> naermesteLedere = sykefravaeroppfoelgingConsumer.hentNaermesteLedere(aktorId, OIDCIssuer.INTERN);
            List<Hendelse> sykmeldtHendelser = sykefravaeroppfoelgingConsumer.hentHendelserForSykmeldt(aktorId, OIDCIssuer.INTERN);
            List<Brukeroppgave> nlBrukeroppgaver = new ArrayList<>();

            for (NaermesteLeder naermesteLeder : naermesteLedere) {
                log.info("NL: " + naermesteLeder.naermesteLederAktoerId() + " | " + naermesteLeder.orgnummer() + " | " + naermesteLeder.navn());
                nlBrukeroppgaver.addAll(brukeroppgaveConsumer.hentBrukerOppgaver(naermesteLeder.naermesteLederAktoerId()));
            }

            for (Hendelse hendelse : sykmeldtHendelser) {
                log.info("H: " + hendelse.hendelseId() + " | " + hendelse.type() + " | " + hendelse.aktorId());
            }

            for (Brukeroppgave bo : nlBrukeroppgaver) {
                log.info("BO: " + bo.ressursId() + " | " + bo.ident());
            }

            Function<NaermesteLeder, Stream<Historikk>> tilHistorikk = naermesteLeder ->
                    brukeroppgaveConsumer.hentBrukerOppgaver(naermesteLeder.naermesteLederAktoerId())
                            .stream()
                            .filter(brukeroppgave -> (brukeroppgave.oppgavetype.equals("NAERMESTE_LEDER_SVAR_MOTEBEHOV")
                                    && sykmeldtHendelser
                                            .stream()
                                            .anyMatch(hendelse -> hendelse.hendelseId() == Long.parseLong(brukeroppgave.ressursId())))
                            )
                            .map(brukeroppgave -> new Historikk()
                                    .tekst("Varsel om svar på motebehov har blitt sendt til nærmeste leder i bedrift "
                                            + organisasjonConsumer.hentBedriftnavn(naermesteLeder.orgnummer()) + '.')
                                    .tidspunkt(brukeroppgave.opprettetTidspunkt())
                            );

            return naermesteLedere
                    .stream()
                    .flatMap(tilHistorikk)
                    .collect(toList());
        } catch (RuntimeException e) {
            log.error("Klarte ikke hente ut varselhistorikk på nestleders møtebehov");
            return new ArrayList<>();
        }
    }
    
    private List<Historikk> hentLesteMotebehovHistorikk(String sykmeldtFnr) {
        try {
            return veilederOppgaverService.get(sykmeldtFnr)
                    .stream()
                    .filter(veilederOppgave -> veilederOppgave.type.equals("MOTEBEHOV_MOTTATT") && veilederOppgave.status.equals("FERDIG"))
                    .map(veilederOppgave -> new Historikk()
                            .tekst("Møtebehovet ble lest av " + veilederOppgave.sistEndretAv + ".")
                            .tidspunkt(veilederOppgave.getSistEndret())
                    )
                    .collect(toList());
        } catch (RestClientException e) {
            log.error("Klarte ikke hente ut varselhistorikk på leste møtebehov");
            return new ArrayList<>();
        }
    }

}
