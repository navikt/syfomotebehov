package no.nav.syfo.service;

import no.nav.syfo.consumer.ws.AktoerConsumer;
import no.nav.syfo.domain.rest.*;
import no.nav.syfo.repository.dao.MotebehovDAO;
import no.nav.syfo.repository.domain.PMotebehov;
import org.springframework.stereotype.Service;

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
 *
 * Det er også nyttig å ha mappingen her (så lenge klassen er under en skjermlengde), slik at man ser den i sammenheng med stedet den blir brukt.
 */
@Service
public class MotebehovService {

    private final AktoerConsumer aktoerConsumer;
    private final MotebehovDAO motebehovDAO;

    @Inject
    public MotebehovService(final AktoerConsumer aktoerConsumer, final MotebehovDAO motebehovDAO) {
        this.aktoerConsumer = aktoerConsumer;
        this.motebehovDAO = motebehovDAO;
    }

    public List<Motebehov> hentMotebehovListe(final Fnr arbeidstakerFnr) {
        final String arbeidstakerAktoerId = aktoerConsumer.hentAktoerIdForFnr(arbeidstakerFnr.getFnr());

        return motebehovDAO.hentMotebehovListeForAktoer(arbeidstakerAktoerId).orElse(emptyList())
                .stream()
                .map(dbMotebehov -> mapPMotebehovToMotebehov(arbeidstakerFnr, dbMotebehov))
                .collect(toList());
    }

    public UUID lagreMotebehov(Fnr innloggetFNR, final NyttMotebehov lagreMotebehov) {
        final String innloggetBrukerAktoerId = aktoerConsumer.hentAktoerIdForFnr(innloggetFNR.getFnr());
        final String arbeidstakerAktoerId = aktoerConsumer.hentAktoerIdForFnr(lagreMotebehov.arbeidstakerFnr.getFnr());
        final PMotebehov motebehov = mapLagreMotebehovToPMotebehov(innloggetBrukerAktoerId, arbeidstakerAktoerId, lagreMotebehov);

        return motebehovDAO.create(motebehov);
    }

    public List<VeilederOppgaveFeedItem> hentMotebehovListe(final String timestamp) {
        return motebehovDAO.finnMotebehovOpprettetSiden(LocalDateTime.parse(timestamp))
                .stream()
                .map(motebehov -> {
                    String fnr = aktoerConsumer.hentFnrForAktoerId(motebehov.aktoerId);
                    return mapPMotebehovToVeilederOppgaveFeedItem(motebehov, fnr);
                })
                .collect(toList());
    }

    private PMotebehov mapLagreMotebehovToPMotebehov(String innloggetAktoerId, String arbeidstakerAktoerId, NyttMotebehov lagreMotebehov) {
        return new PMotebehov()
                .opprettetAv(innloggetAktoerId)
                .aktoerId(arbeidstakerAktoerId)
                .virksomhetsnummer(lagreMotebehov.virksomhetsnummer)
                .harMotebehov(lagreMotebehov.motebehovSvar().harMotebehov)
                .friskmeldingForventning(lagreMotebehov.motebehovSvar().friskmeldingForventning)
                .tiltak(lagreMotebehov.motebehovSvar().tiltak)
                .tiltakResultat(lagreMotebehov.motebehovSvar().tiltakResultat)
                .forklaring(lagreMotebehov.motebehovSvar().forklaring);
    }

    private Motebehov mapPMotebehovToMotebehov(Fnr arbeidstakerFnr, PMotebehov pMotebehov) {
        return new Motebehov()
                .id(pMotebehov.uuid)
                .opprettetDato(pMotebehov.opprettetDato)
                .opprettetAv(pMotebehov.opprettetAv)
                .arbeidstakerFnr(arbeidstakerFnr)
                .virksomhetsnummer(pMotebehov.virksomhetsnummer)
                .motebehovSvar(new MotebehovSvar()
                        .friskmeldingForventning(pMotebehov.friskmeldingForventning)
                        .tiltak(pMotebehov.tiltak)
                        .tiltakResultat(pMotebehov.tiltakResultat)
                        .harMotebehov(pMotebehov.harMotebehov)
                        .forklaring(pMotebehov.forklaring)
                );
    }

    private VeilederOppgaveFeedItem mapPMotebehovToVeilederOppgaveFeedItem(PMotebehov motebehov, String fnr) {
        return new VeilederOppgaveFeedItem()
                .uuid(motebehov.uuid.toString())
                .fnr(fnr)
                .lenke(baseUrl() + "/sykefravaer/" + fnr + "/motebehov/")
                .type(VeilederOppgaveFeedItem.FeedHendelseType.MOTEBEHOV_MOTTATT.toString())
                .created(motebehov.opprettetDato)
                .status("IKKE_STARTET")
                .virksomhetsnummer(motebehov.virksomhetsnummer);
    }

}