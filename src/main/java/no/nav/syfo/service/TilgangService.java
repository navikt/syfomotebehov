package no.nav.syfo.service;

import lombok.extern.slf4j.Slf4j;
import no.nav.syfo.consumer.ws.AktoerConsumer;
import no.nav.syfo.consumer.ws.PersonConsumer;
import no.nav.syfo.consumer.ws.SykefravaeroppfoelgingConsumer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.inject.Inject;

@Service
@Slf4j
public class TilgangService {

    private String dev;
    private AktoerConsumer aktoerConsumer;
    private PersonConsumer personConsumer;
    private SykefravaeroppfoelgingConsumer sykefravaeroppfoelgingConsumer;

    @Inject
    public TilgangService(@Value("${dev}") String dev,
                          final AktoerConsumer aktoerConsumer,
                          final PersonConsumer personConsumer,
                          final SykefravaeroppfoelgingConsumer sykefravaeroppfoelgingConsumer
    ) {
        this.dev = dev;
        this.aktoerConsumer = aktoerConsumer;
        this.personConsumer = personConsumer;
        this.sykefravaeroppfoelgingConsumer = sykefravaeroppfoelgingConsumer;
    }

    public boolean harTilgangTilOppslaattBruker(String fnr) {
        if ("true".equals(dev)) {
            return true;
        }
        String oppslaattAktoerId = aktoerConsumer.hentAktoerIdForFnr(fnr);
        String innloggetIdent = "03097043123";
        log.error("fnr {}", fnr);
        log.error("Iident {}", innloggetIdent);
        log.error("1 {}", sporOmNoenAndreEnnSegSelvEllerEgneAnsatte(innloggetIdent, fnr));
        log.error("2 {}", personConsumer.erBrukerKode6(oppslaattAktoerId));
        log.error("3 {}", !(sporOmNoenAndreEnnSegSelvEllerEgneAnsatte(innloggetIdent, fnr)
                || personConsumer.erBrukerKode6(oppslaattAktoerId)));
        log.error("4 {}", sporInnloggetBrukerOmSegSelv(innloggetIdent, fnr));
        log.error("5 {}", sporInnloggetBrukerOmEnAnsatt(innloggetIdent, fnr));
        return !(sporOmNoenAndreEnnSegSelvEllerEgneAnsatte(innloggetIdent, fnr)
                || personConsumer.erBrukerKode6(oppslaattAktoerId));
    }

    private boolean sporOmNoenAndreEnnSegSelvEllerEgneAnsatteEllerLedere(String innloggetIdent, String oppslaattFnr) {
        return !(sporInnloggetBrukerOmSegSelv(innloggetIdent, oppslaattFnr) || sporInnloggetBrukerOmEnAnsatt(innloggetIdent, oppslaattFnr) || sporInnloggetBrukerOmEnLeder(innloggetIdent, oppslaattFnr));
    }


    private boolean sporOmNoenAndreEnnSegSelvEllerEgneAnsatte(String innloggetIdent, String oppslaattFnr) {
        return !(sporInnloggetBrukerOmSegSelv(innloggetIdent, oppslaattFnr) || sporInnloggetBrukerOmEnAnsatt(innloggetIdent, oppslaattFnr));
    }

    private boolean sporInnloggetBrukerOmEnAnsatt(String innloggetIdent, String oppslaattFnr) {
        String innloggetAktoerId = aktoerConsumer.hentAktoerIdForFnr(innloggetIdent);
        log.error("innloggetAktoerId {}", innloggetAktoerId);
        String oppslaattAktoerId = aktoerConsumer.hentAktoerIdForFnr(oppslaattFnr);
        log.error("oppslaattAktoerId {}", oppslaattAktoerId);
        log.error("ansatt {}", sykefravaeroppfoelgingConsumer.hentAnsatteAktorId(innloggetAktoerId).get(0));
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
