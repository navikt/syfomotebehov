package no.nav.syfo.service;

import lombok.extern.slf4j.Slf4j;
import no.nav.security.oidc.context.OIDCRequestContextHolder;
import no.nav.syfo.consumer.ws.AktoerConsumer;
import no.nav.syfo.consumer.ws.PersonConsumer;
import no.nav.syfo.consumer.ws.SykefravaeroppfoelgingConsumer;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.ws.rs.ForbiddenException;

import static no.nav.syfo.util.OIDCUtil.fnrFraOIDCEkstern;

@Service
@Slf4j
public class BrukertilgangService {

    private OIDCRequestContextHolder contextHolder;
    private AktoerConsumer aktoerConsumer;
    private PersonConsumer personConsumer;
    private SykefravaeroppfoelgingConsumer sykefravaeroppfoelgingConsumer;

    @Inject
    public BrukertilgangService(final OIDCRequestContextHolder contextHolder,
                                final AktoerConsumer aktoerConsumer,
                                final PersonConsumer personConsumer,
                                final SykefravaeroppfoelgingConsumer sykefravaeroppfoelgingConsumer
    ) {
        this.contextHolder = contextHolder;
        this.aktoerConsumer = aktoerConsumer;
        this.personConsumer = personConsumer;
        this.sykefravaeroppfoelgingConsumer = sykefravaeroppfoelgingConsumer;
    }

    public boolean harTilgangTilOppslaattBruker(String fnr) {
        String oppslaattAktoerId = aktoerConsumer.hentAktoerIdForFnr(fnr);
        String innloggetIdent = fnrFraOIDCEkstern(contextHolder).getFnr();
        try {
            return !(sporOmNoenAndreEnnSegSelvEllerEgneAnsatte(innloggetIdent, fnr)
                    || personConsumer.erBrukerKode6(oppslaattAktoerId));
        } catch (ForbiddenException e) {
            return false;
        }
    }

    private boolean sporOmNoenAndreEnnSegSelvEllerEgneAnsatteEllerLedere(String innloggetIdent, String oppslaattFnr) {
        return !(sporInnloggetBrukerOmSegSelv(innloggetIdent, oppslaattFnr) || sporInnloggetBrukerOmEnAnsatt(innloggetIdent, oppslaattFnr) || sporInnloggetBrukerOmEnLeder(innloggetIdent, oppslaattFnr));
    }


    private boolean sporOmNoenAndreEnnSegSelvEllerEgneAnsatte(String innloggetIdent, String oppslaattFnr) {
        return !(sporInnloggetBrukerOmSegSelv(innloggetIdent, oppslaattFnr) || sporInnloggetBrukerOmEnAnsatt(innloggetIdent, oppslaattFnr));
    }

    private boolean sporInnloggetBrukerOmEnAnsatt(String innloggetIdent, String oppslaattFnr) {
        String innloggetAktoerId = aktoerConsumer.hentAktoerIdForFnr(innloggetIdent);
        String oppslaattAktoerId = aktoerConsumer.hentAktoerIdForFnr(oppslaattFnr);
        return sykefravaeroppfoelgingConsumer.hentAnsatteAktorId(innloggetAktoerId)
                .stream()
                .anyMatch(oppslaattAktoerId::equals);
    }

    private boolean sporInnloggetBrukerOmEnLeder(String innloggetIdent, String oppslaattFnr) {
        String innloggetAktoerId = aktoerConsumer.hentAktoerIdForFnr(innloggetIdent);
        String oppslaattAktoerId = aktoerConsumer.hentAktoerIdForFnr(oppslaattFnr);
        return sykefravaeroppfoelgingConsumer.hentNaermesteLederAktoerIdListe(innloggetAktoerId)
                .stream()
                .anyMatch(oppslaattAktoerId::equals);
    }

    private boolean sporInnloggetBrukerOmSegSelv(String innloggetIdent, String fnr) {
        return fnr.equals(innloggetIdent);
    }
}
