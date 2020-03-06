package no.nav.syfo.service;

import lombok.extern.slf4j.Slf4j;
import no.nav.syfo.aktorregister.AktorregisterConsumer;
import no.nav.syfo.aktorregister.domain.AktorId;
import no.nav.syfo.aktorregister.domain.Fodselsnummer;
import no.nav.syfo.behandlendeenhet.BehandlendeEnhetConsumer;
import no.nav.syfo.domain.rest.*;
import no.nav.syfo.repository.dao.MotebehovDAO;
import no.nav.syfo.repository.domain.PMotebehov;
import no.nav.syfo.util.Metrikk;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static no.nav.syfo.util.RestUtils.baseUrl;

/**
 * MøtebehovService har ansvaret for å knytte sammen og oversette mellom REST-grensesnittet, andre tjenester (aktør-registeret)
 * og database-koblingen, slik at de ikke trenger å vite noe om hverandre. (Low coupling - high cohesion)
 * <p>
 * Det er også nyttig å ha mappingen her (så lenge klassen er under en skjermlengde), slik at man ser den i sammenheng med stedet den blir brukt.
 */
@Service
@Slf4j
public class MotebehovService {

    private final Metrikk metrikk;
    private final AktorregisterConsumer aktorregisterConsumer;
    private final BehandlendeEnhetConsumer behandlendeEnhetConsumer;
    private final OversikthendelseService oversikthendelseService;
    private final MotebehovDAO motebehovDAO;

    @Inject
    public MotebehovService(
            Metrikk metrikk,
            AktorregisterConsumer aktorregisterConsumer,
            BehandlendeEnhetConsumer behandlendeEnhetConsumer,
            OversikthendelseService oversikthendelseService,
            MotebehovDAO motebehovDAO
    ) {
        this.metrikk = metrikk;
        this.aktorregisterConsumer = aktorregisterConsumer;
        this.behandlendeEnhetConsumer = behandlendeEnhetConsumer;
        this.oversikthendelseService = oversikthendelseService;
        this.motebehovDAO = motebehovDAO;
    }

    @Transactional
    public void behandleUbehandledeMotebehov(final Fnr arbeidstakerFnr, final String veilederIdent) {
        int antallOppdateringer = motebehovDAO.oppdaterUbehandledeMotebehovTilBehandlet(aktorregisterConsumer.getAktorIdForFodselsnummer(new Fodselsnummer(arbeidstakerFnr.getFnr())), veilederIdent);

        if (antallOppdateringer > 0) {
            String behandlendeEnhet = behandlendeEnhetConsumer.getBehandlendeEnhet(arbeidstakerFnr.getFnr()).getEnhetId();
            oversikthendelseService.sendOversikthendelse(arbeidstakerFnr.getFnr(), behandlendeEnhet);
        } else {
            metrikk.tellHendelse("feil_behandle_motebehov_svar_eksiterer_ikke");
            log.error("Ugyldig tilstand: Veileder {} forsøkte å behandle motebehovsvar som ikke eksisterer", veilederIdent);
            throw new RuntimeException();

        }
    }

    public List<Motebehov> hentMotebehovListe(final Fodselsnummer arbeidstakerFnr) {
        final String arbeidstakerAktoerId = aktorregisterConsumer.getAktorIdForFodselsnummer(arbeidstakerFnr);
        return motebehovDAO.hentMotebehovListeForAktoer(arbeidstakerAktoerId)
                .orElse(emptyList())
                .stream()
                .map(dbMotebehov -> mapPMotebehovToMotebehov(Fnr.of(arbeidstakerFnr.getValue()), dbMotebehov))
                .collect(toList());
    }

    public List<Motebehov> hentMotebehovListeForOgOpprettetAvArbeidstaker(final Fnr arbeidstakerFnr) {
        final String arbeidstakerAktoerId = aktorregisterConsumer.getAktorIdForFodselsnummer(new Fodselsnummer(arbeidstakerFnr.getFnr()));
        return motebehovDAO.hentMotebehovListeForOgOpprettetAvArbeidstaker(arbeidstakerAktoerId)
                .orElse(emptyList())
                .stream()
                .map(dbMotebehov -> mapPMotebehovToMotebehov(arbeidstakerFnr, dbMotebehov))
                .collect(toList());
    }

    public List<Motebehov> hentMotebehovListeForArbeidstakerOpprettetAvLeder(final Fnr arbeidstakerFnr, String virksomhetsnummer) {
        final String arbeidstakerAktoerId = aktorregisterConsumer.getAktorIdForFodselsnummer(new Fodselsnummer(arbeidstakerFnr.getFnr()));
        return motebehovDAO.hentMotebehovListeForArbeidstakerOpprettetAvLeder(arbeidstakerAktoerId, virksomhetsnummer)
                .orElse(emptyList())
                .stream()
                .map(dbMotebehov -> mapPMotebehovToMotebehov(arbeidstakerFnr, dbMotebehov))
                .collect(toList());
    }

    @Transactional
    public UUID lagreMotebehov(Fnr innloggetFNR, Fnr arbeidstakerFnr, final NyttMotebehov nyttMotebehov) {
        final String innloggetBrukerAktoerId = aktorregisterConsumer.getAktorIdForFodselsnummer(new Fodselsnummer(innloggetFNR.getFnr()));
        final String arbeidstakerAktoerId = aktorregisterConsumer.getAktorIdForFodselsnummer(new Fodselsnummer(arbeidstakerFnr.getFnr()));
        final String arbeidstakerBehandlendeEnhet = behandlendeEnhetConsumer.getBehandlendeEnhet(arbeidstakerFnr.getFnr()).getEnhetId();
        final PMotebehov motebehov = mapNyttMotebehovToPMotebehov(innloggetBrukerAktoerId, arbeidstakerAktoerId, arbeidstakerBehandlendeEnhet, nyttMotebehov);

        UUID id = motebehovDAO.create(motebehov);

        if (nyttMotebehov.motebehovSvar.harMotebehov) {
            oversikthendelseService.sendOversikthendelse(nyttMotebehov
                    .arbeidstakerFnr(arbeidstakerFnr.getFnr())
                    .tildeltEnhet(arbeidstakerBehandlendeEnhet)
            );
        }
        return id;
    }

    public List<VeilederOppgaveFeedItem> hentMotebehovListe(final String timestamp) {
        return motebehovDAO.finnMotebehovMedBehovOpprettetSiden(LocalDateTime.parse(timestamp))
                .stream()
                .map(motebehov -> {
                    String fnr = aktorregisterConsumer.getFnrForAktorId(new AktorId(motebehov.aktoerId));
                    return mapPMotebehovToVeilederOppgaveFeedItem(motebehov, fnr);
                })
                .collect(toList());
    }

    private PMotebehov mapNyttMotebehovToPMotebehov(String innloggetAktoerId, String arbeidstakerAktoerId, String tildeltEnhet, NyttMotebehov nyttMotebehov) {
        return new PMotebehov()
                .opprettetAv(innloggetAktoerId)
                .aktoerId(arbeidstakerAktoerId)
                .virksomhetsnummer(nyttMotebehov.virksomhetsnummer)
                .harMotebehov(nyttMotebehov.motebehovSvar().harMotebehov)
                .forklaring(nyttMotebehov.motebehovSvar().forklaring)
                .tildeltEnhet(tildeltEnhet)
                .behandletTidspunkt(null);
    }

    private Motebehov mapPMotebehovToMotebehov(Fnr arbeidstakerFnr, PMotebehov pMotebehov) {
        return new Motebehov()
                .id(pMotebehov.uuid)
                .opprettetDato(pMotebehov.opprettetDato)
                .aktorId(pMotebehov.aktoerId)
                .opprettetAv(pMotebehov.opprettetAv)
                .arbeidstakerFnr(arbeidstakerFnr)
                .virksomhetsnummer(pMotebehov.virksomhetsnummer)
                .motebehovSvar(new MotebehovSvar()
                        .harMotebehov(pMotebehov.harMotebehov)
                        .forklaring(pMotebehov.forklaring)
                )
                .tildeltEnhet(pMotebehov.tildeltEnhet)
                .behandletTidspunkt(pMotebehov.behandletTidspunkt)
                .behandletVeilederIdent(pMotebehov.behandletVeilederIdent);
    }

    private VeilederOppgaveFeedItem mapPMotebehovToVeilederOppgaveFeedItem(PMotebehov motebehov, String fnr) {
        return new VeilederOppgaveFeedItem()
                .uuid(motebehov.uuid.toString())
                .tildeltEnhet(motebehov.tildeltEnhet)
                .fnr(fnr)
                .lenke(baseUrl() + "/sykefravaer/" + fnr + "/motebehov/")
                .type(VeilederOppgaveFeedItem.FeedHendelseType.MOTEBEHOV_MOTTATT.toString())
                .created(motebehov.opprettetDato)
                .status("IKKE_STARTET")
                .virksomhetsnummer(motebehov.virksomhetsnummer);
    }
}
