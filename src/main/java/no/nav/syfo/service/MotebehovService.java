package no.nav.syfo.service;

import no.nav.syfo.aktorregister.AktorregisterConsumer;
import no.nav.syfo.aktorregister.domain.Fodselsnummer;
import no.nav.syfo.behandlendeenhet.BehandlendeEnhetConsumer;
import no.nav.syfo.domain.rest.*;
import no.nav.syfo.exception.ConflictException;
import no.nav.syfo.repository.dao.MotebehovDAO;
import no.nav.syfo.repository.domain.PMotebehov;
import no.nav.syfo.util.Metrikk;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;
import java.util.*;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.slf4j.LoggerFactory.getLogger;

@Service
public class MotebehovService {

    private static final Logger log = getLogger(MotebehovService.class);

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
    public void behandleUbehandledeMotebehov(final Fodselsnummer arbeidstakerFnr, final String veilederIdent) {
        int antallOppdateringer = motebehovDAO.oppdaterUbehandledeMotebehovTilBehandlet(aktorregisterConsumer.getAktorIdForFodselsnummer(arbeidstakerFnr), veilederIdent);

        if (antallOppdateringer > 0) {
            String behandlendeEnhet = behandlendeEnhetConsumer.getBehandlendeEnhet(arbeidstakerFnr.getValue(), null).getEnhetId();
            oversikthendelseService.sendOversikthendelse(arbeidstakerFnr.getValue(), behandlendeEnhet);
        } else {
            metrikk.tellHendelse("feil_behandle_motebehov_svar_eksiterer_ikke");
            log.warn("Ugyldig tilstand: Veileder {} forsøkte å behandle motebehovsvar som ikke eksisterer. Kaster Http-409", veilederIdent);
            throw new ConflictException();
        }
    }

    public List<Motebehov> hentMotebehovListe(final Fodselsnummer arbeidstakerFnr) {
        final String arbeidstakerAktoerId = aktorregisterConsumer.getAktorIdForFodselsnummer(arbeidstakerFnr);
        return motebehovDAO.hentMotebehovListeForAktoer(arbeidstakerAktoerId)
                .orElse(emptyList())
                .stream()
                .map(dbMotebehov -> mapPMotebehovToMotebehov(arbeidstakerFnr, dbMotebehov))
                .collect(toList());
    }

    public List<Motebehov> hentMotebehovListeForOgOpprettetAvArbeidstaker(final Fodselsnummer arbeidstakerFnr) {
        final String arbeidstakerAktoerId = aktorregisterConsumer.getAktorIdForFodselsnummer(new Fodselsnummer(arbeidstakerFnr.getValue()));
        return motebehovDAO.hentMotebehovListeForOgOpprettetAvArbeidstaker(arbeidstakerAktoerId)
                .orElse(emptyList())
                .stream()
                .map(dbMotebehov -> mapPMotebehovToMotebehov(arbeidstakerFnr, dbMotebehov))
                .collect(toList());
    }

    public List<Motebehov> hentMotebehovListeForArbeidstakerOpprettetAvLeder(final Fodselsnummer arbeidstakerFnr, String virksomhetsnummer) {
        final String arbeidstakerAktoerId = aktorregisterConsumer.getAktorIdForFodselsnummer(new Fodselsnummer(arbeidstakerFnr.getValue()));
        return motebehovDAO.hentMotebehovListeForArbeidstakerOpprettetAvLeder(arbeidstakerAktoerId, virksomhetsnummer)
                .orElse(emptyList())
                .stream()
                .map(dbMotebehov -> mapPMotebehovToMotebehov(arbeidstakerFnr, dbMotebehov))
                .collect(toList());
    }

    @Transactional
    public UUID lagreMotebehov(Fodselsnummer innloggetFNR, Fodselsnummer arbeidstakerFnr, final NyttMotebehov nyttMotebehov) {
        final String innloggetBrukerAktoerId = aktorregisterConsumer.getAktorIdForFodselsnummer(innloggetFNR);
        final String arbeidstakerAktoerId = aktorregisterConsumer.getAktorIdForFodselsnummer(arbeidstakerFnr);
        final String arbeidstakerBehandlendeEnhet = behandlendeEnhetConsumer.getBehandlendeEnhet(arbeidstakerFnr.getValue(), null).getEnhetId();
        final PMotebehov motebehov = mapNyttMotebehovToPMotebehov(innloggetBrukerAktoerId, arbeidstakerAktoerId, arbeidstakerBehandlendeEnhet, nyttMotebehov);

        UUID id = motebehovDAO.create(motebehov);

        if (nyttMotebehov.motebehovSvar.harMotebehov) {
            oversikthendelseService.sendOversikthendelse(nyttMotebehov
                    .arbeidstakerFnr(arbeidstakerFnr.getValue())
                    .tildeltEnhet(arbeidstakerBehandlendeEnhet)
            );
        }
        return id;
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

    private Motebehov mapPMotebehovToMotebehov(Fodselsnummer arbeidstakerFnr, PMotebehov pMotebehov) {
        return new Motebehov()
                .id(pMotebehov.uuid)
                .opprettetDato(pMotebehov.opprettetDato)
                .aktorId(pMotebehov.aktoerId)
                .opprettetAv(pMotebehov.opprettetAv)
                .arbeidstakerFnr(arbeidstakerFnr.getValue())
                .virksomhetsnummer(pMotebehov.virksomhetsnummer)
                .motebehovSvar(new MotebehovSvar()
                        .harMotebehov(pMotebehov.harMotebehov)
                        .forklaring(pMotebehov.forklaring)
                )
                .tildeltEnhet(pMotebehov.tildeltEnhet)
                .behandletTidspunkt(pMotebehov.behandletTidspunkt)
                .behandletVeilederIdent(pMotebehov.behandletVeilederIdent);
    }
}
