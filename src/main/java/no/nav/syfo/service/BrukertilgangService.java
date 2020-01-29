package no.nav.syfo.service;

import no.nav.syfo.consumer.ws.*;
import no.nav.syfo.oidc.OIDCIssuer;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.ws.rs.ForbiddenException;

@Service
public class BrukertilgangService {

    private final AktoerConsumer aktoerConsumer;
    private final PersonConsumer personConsumer;
    private final SykefravaeroppfoelgingConsumer sykefravaeroppfoelgingConsumer;

    @Inject
    public BrukertilgangService(
            AktoerConsumer aktoerConsumer,
            PersonConsumer personConsumer,
            SykefravaeroppfoelgingConsumer sykefravaeroppfoelgingConsumer
    ) {
        this.aktoerConsumer = aktoerConsumer;
        this.personConsumer = personConsumer;
        this.sykefravaeroppfoelgingConsumer = sykefravaeroppfoelgingConsumer;
    }

    public boolean harTilgangTilOppslaattBruker(String innloggetIdent, String fnr) {
        String oppslaattAktoerId = aktoerConsumer.hentAktoerIdForFnr(fnr);
        try {
            return !(sporOmNoenAndreEnnSegSelvEllerEgneAnsatte(innloggetIdent, fnr)
                    || personConsumer.erBrukerKode6(oppslaattAktoerId));
        } catch (ForbiddenException e) {
            return false;
        }
    }


    public boolean sporOmNoenAndreEnnSegSelvEllerEgneAnsatte(String innloggetIdent, String oppslaattFnr) {
        return !(sporInnloggetBrukerOmSegSelv(innloggetIdent, oppslaattFnr) || sporInnloggetBrukerOmEnAnsatt(innloggetIdent, oppslaattFnr));
    }

    private boolean sporInnloggetBrukerOmEnAnsatt(String innloggetIdent, String oppslaattFnr) {
        String innloggetAktoerId = aktoerConsumer.hentAktoerIdForFnr(innloggetIdent);
        String oppslaattAktoerId = aktoerConsumer.hentAktoerIdForFnr(oppslaattFnr);
        return sykefravaeroppfoelgingConsumer.hentAnsatteAktorId(innloggetAktoerId, OIDCIssuer.EKSTERN)
                .stream()
                .anyMatch(oppslaattAktoerId::equals);
    }

    private boolean sporInnloggetBrukerOmSegSelv(String innloggetIdent, String fnr) {
        return fnr.equals(innloggetIdent);
    }
}
