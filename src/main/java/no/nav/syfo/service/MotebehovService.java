package no.nav.syfo.service;

import no.nav.syfo.consumer.ws.AktoerConsumer;
import no.nav.syfo.consumer.ws.ArbeidsfordelingConsumer;
import no.nav.syfo.consumer.ws.PersonConsumer;
import no.nav.syfo.domain.rest.*;
import no.nav.syfo.mappers.domain.Enhet;
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
 * <p>
 * Det er også nyttig å ha mappingen her (så lenge klassen er under en skjermlengde), slik at man ser den i sammenheng med stedet den blir brukt.
 */
@Service
public class MotebehovService {

    private final AktoerConsumer aktoerConsumer;
    private final ArbeidsfordelingConsumer arbeidsfordelingConsumer;
    private final PersonConsumer personConsumer;
    private final MotebehovDAO motebehovDAO;

    @Inject
    public MotebehovService(final AktoerConsumer aktoerConsumer,
                            final ArbeidsfordelingConsumer arbeidsfordelingConsumer,
                            final PersonConsumer personConsumer,
                            final MotebehovDAO motebehovDAO) {
        this.aktoerConsumer = aktoerConsumer;
        this.arbeidsfordelingConsumer = arbeidsfordelingConsumer;
        this.personConsumer = personConsumer;
        this.motebehovDAO = motebehovDAO;
    }

    public List<Motebehov> hentMotebehovListe(final Fnr arbeidstakerFnr) {
        final String arbeidstakerAktoerId = aktoerConsumer.hentAktoerIdForFnr(arbeidstakerFnr.getFnr());
        return motebehovDAO.hentMotebehovListeForAktoer(arbeidstakerAktoerId)
                .orElse(emptyList())
                .stream()
                .map(dbMotebehov -> mapPMotebehovToMotebehov(arbeidstakerFnr, dbMotebehov))
                .collect(toList());
    }

    public List<Motebehov> hentMotebehovListe(final Fnr arbeidstakerFnr, String virksomhetsnummer) {
        final String arbeidstakerAktoerId = aktoerConsumer.hentAktoerIdForFnr(arbeidstakerFnr.getFnr());
        return motebehovDAO.hentMotebehovListeForAktoerOgVirksomhetsnummer(arbeidstakerAktoerId, virksomhetsnummer)
                .orElse(emptyList())
                .stream()
                .map(dbMotebehov -> mapPMotebehovToMotebehov(arbeidstakerFnr, dbMotebehov))
                .collect(toList());
    }

    public UUID lagreMotebehov(Fnr innloggetFNR, Fnr arbeidstakerFnr, final NyttMotebehov nyttMotebehov) {
        final String innloggetBrukerAktoerId = aktoerConsumer.hentAktoerIdForFnr(innloggetFNR.getFnr());
        final String arbeidstakerAktoerId = aktoerConsumer.hentAktoerIdForFnr(arbeidstakerFnr.getFnr());
        final String arbeidstakerGeografiskTilknytning = personConsumer.hentGeografiskTilknytning(arbeidstakerFnr.getFnr());
        final Enhet arbeidstakerBehandlendeEnhet = arbeidsfordelingConsumer.finnAktivBehandlendeEnhet(arbeidstakerGeografiskTilknytning);
        final PMotebehov motebehov = mapNyttMotebehovToPMotebehov(innloggetBrukerAktoerId, arbeidstakerAktoerId, arbeidstakerBehandlendeEnhet, nyttMotebehov);

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

    private PMotebehov mapNyttMotebehovToPMotebehov(String innloggetAktoerId, String arbeidstakerAktoerId, Enhet tildeltEnhet, NyttMotebehov nyttMotebehov) {
        return new PMotebehov()
                .opprettetAv(innloggetAktoerId)
                .aktoerId(arbeidstakerAktoerId)
                .virksomhetsnummer(nyttMotebehov.virksomhetsnummer)
                .harMotebehov(nyttMotebehov.motebehovSvar().harMotebehov)
                .friskmeldingForventning(nyttMotebehov.motebehovSvar().friskmeldingForventning)
                .tiltak(nyttMotebehov.motebehovSvar().tiltak)
                .tiltakResultat(nyttMotebehov.motebehovSvar().tiltakResultat)
                .forklaring(nyttMotebehov.motebehovSvar().forklaring)
                .tildeltEnhet(tildeltEnhet.enhetId());
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
                )
                .tildeltEnhet(pMotebehov.tildeltEnhet);
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
