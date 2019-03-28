package no.nav.syfo.service;

import lombok.extern.slf4j.Slf4j;
import no.nav.syfo.consumer.ws.*;
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
import static no.nav.syfo.domain.rest.BrukerPaaEnhet.Skjermingskode.*;

/**
 * MøtebehovService har ansvaret for å knytte sammen og oversette mellom REST-grensesnittet, andre tjenester (aktør-registeret)
 * og database-koblingen, slik at de ikke trenger å vite noe om hverandre. (Low coupling - high cohesion)
 * <p>
 * Det er også nyttig å ha mappingen her (så lenge klassen er under en skjermlengde), slik at man ser den i sammenheng med stedet den blir brukt.
 */
@Service
@Slf4j
public class MotebehovService {

    private final AktoerConsumer aktoerConsumer;
    private final ArbeidsfordelingConsumer arbeidsfordelingConsumer;
    private final PersonConsumer personConsumer;
    private final EgenAnsattConsumer egenAnsattConsumer;
    private final OrganisasjonEnhetConsumer organisasjonEnhetConsumer;
    private final MotebehovDAO motebehovDAO;

    @Inject
    public MotebehovService(final AktoerConsumer aktoerConsumer,
                            final ArbeidsfordelingConsumer arbeidsfordelingConsumer,
                            final PersonConsumer personConsumer,
                            final EgenAnsattConsumer egenAnsattConsumer,
                            final OrganisasjonEnhetConsumer organisasjonEnhetConsumer,
                            final MotebehovDAO motebehovDAO) {
        this.aktoerConsumer = aktoerConsumer;
        this.arbeidsfordelingConsumer = arbeidsfordelingConsumer;
        this.personConsumer = personConsumer;
        this.egenAnsattConsumer = egenAnsattConsumer;
        this.organisasjonEnhetConsumer = organisasjonEnhetConsumer;
        this.motebehovDAO = motebehovDAO;
    }

    public List<BrukerPaaEnhet> hentSykmeldteMedMotebehovPaaEnhet(String enhetId) {
        return motebehovDAO.hentAktorIdMedMotebehovForEnhet(enhetId)
                .orElse(emptyList())
                .stream()
                .map(aktoerConsumer::hentFnrForAktoerId)
                .map(sykmeldtFnr -> new BrukerPaaEnhet()
                        .fnr(sykmeldtFnr)
                        .skjermetEllerEgenAnsatt(hentBrukersSkjermingskode(sykmeldtFnr)))
                .collect(toList());
    }

    public List<Motebehov> hentMotebehovListe(final Fnr arbeidstakerFnr) {
        final String arbeidstakerAktoerId = aktoerConsumer.hentAktoerIdForFnr(arbeidstakerFnr.getFnr());
        return motebehovDAO.hentMotebehovListeForAktoer(arbeidstakerAktoerId)
                .orElse(emptyList())
                .stream()
                .map(dbMotebehov -> mapPMotebehovToMotebehov(arbeidstakerFnr, dbMotebehov))
                .collect(toList());
    }
    public List<Motebehov> hentMotebehovListeForOgOpprettetAvArbeidstaker(final Fnr arbeidstakerFnr) {
        final String arbeidstakerAktoerId = aktoerConsumer.hentAktoerIdForFnr(arbeidstakerFnr.getFnr());
        return motebehovDAO.hentMotebehovListeForOgOpprettetAvArbeidstaker(arbeidstakerAktoerId)
                .orElse(emptyList())
                .stream()
                .map(dbMotebehov -> mapPMotebehovToMotebehov(arbeidstakerFnr, dbMotebehov))
                .collect(toList());
    }

    public List<Motebehov> hentMotebehovListeForArbeidstakerOpprettetAvLeder(final Fnr arbeidstakerFnr, String virksomhetsnummer) {
        final String arbeidstakerAktoerId = aktoerConsumer.hentAktoerIdForFnr(arbeidstakerFnr.getFnr());
        return motebehovDAO.hentMotebehovListeForArbeidstakerOpprettetAvLeder(arbeidstakerAktoerId, virksomhetsnummer)
                .orElse(emptyList())
                .stream()
                .map(dbMotebehov -> mapPMotebehovToMotebehov(arbeidstakerFnr, dbMotebehov))
                .collect(toList());
    }

    public UUID lagreMotebehov(Fnr innloggetFNR, Fnr arbeidstakerFnr, final NyttMotebehov nyttMotebehov) {
        final String innloggetBrukerAktoerId = aktoerConsumer.hentAktoerIdForFnr(innloggetFNR.getFnr());
        final String arbeidstakerAktoerId = aktoerConsumer.hentAktoerIdForFnr(arbeidstakerFnr.getFnr());
        final String arbeidstakerBehandlendeEnhet = finnArbeidstakersBehandlendeEnhet(arbeidstakerFnr.getFnr());
        final PMotebehov motebehov = mapNyttMotebehovToPMotebehov(innloggetBrukerAktoerId, arbeidstakerAktoerId, arbeidstakerBehandlendeEnhet, nyttMotebehov);

        return motebehovDAO.create(motebehov);
    }

    public List<VeilederOppgaveFeedItem> hentMotebehovListe(final String timestamp) {
        return motebehovDAO.finnMotebehovMedBehovOpprettetSiden(LocalDateTime.parse(timestamp))
                .stream()
                .map(motebehov -> {
                    String fnr = aktoerConsumer.hentFnrForAktoerId(motebehov.aktoerId);
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
                .friskmeldingForventning(nyttMotebehov.motebehovSvar().friskmeldingForventning)
                .tiltak(nyttMotebehov.motebehovSvar().tiltak)
                .tiltakResultat(nyttMotebehov.motebehovSvar().tiltakResultat)
                .forklaring(nyttMotebehov.motebehovSvar().forklaring)
                .tildeltEnhet(tildeltEnhet);
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

    private String finnArbeidstakersBehandlendeEnhet(String arbeidstakerFnr) {
        String geografiskTilknytning = personConsumer.hentGeografiskTilknytning(arbeidstakerFnr);
        if ("".equals(geografiskTilknytning)) {
            log.error("Klarte ikke hente geografisk tilknytning på sykmeldt");
            throw new RuntimeException();
        }
        Enhet enhet = arbeidsfordelingConsumer.finnAktivBehandlendeEnhet(geografiskTilknytning);
        if (egenAnsattConsumer.erEgenAnsatt(arbeidstakerFnr)) {
            Enhet overordnetEnhet = organisasjonEnhetConsumer.finnSetteKontor(enhet.enhetId()).orElse(enhet);
            return overordnetEnhet.enhetId();
        }
        return enhet.enhetId();
    }

    private BrukerPaaEnhet.Skjermingskode hentBrukersSkjermingskode(String fnr) {
        String aktorId = aktoerConsumer.hentAktoerIdForFnr(fnr);
        if (personConsumer.erBrukerDiskresjonsmerket(aktorId))
               return personConsumer.erBrukerKode6(aktorId) ? KODE_6 : KODE_7;
        return egenAnsattConsumer.erEgenAnsatt(fnr) ? EGEN_ANSATT : INGEN;
    }

}
