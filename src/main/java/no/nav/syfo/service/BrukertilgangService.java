package no.nav.syfo.service;

import no.nav.syfo.aktorregister.AktorregisterConsumer;
import no.nav.syfo.aktorregister.domain.Fodselsnummer;
import no.nav.syfo.brukertilgang.BrukertilgangConsumer;
import no.nav.syfo.consumer.ws.PersonConsumer;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.ws.rs.ForbiddenException;

import static no.nav.syfo.config.CacheConfig.CACHENAME_TILGANG_IDENT;

@Service
public class BrukertilgangService {

    private final AktorregisterConsumer aktorregisterConsumer;
    private final BrukertilgangConsumer brukertilgangConsumer;
    private final PersonConsumer personConsumer;

    @Inject
    public BrukertilgangService(
            AktorregisterConsumer aktorregisterConsumer,
            BrukertilgangConsumer brukertilgangConsumer,
            PersonConsumer personConsumer
    ) {
        this.aktorregisterConsumer = aktorregisterConsumer;
        this.brukertilgangConsumer = brukertilgangConsumer;
        this.personConsumer = personConsumer;
    }

    public boolean harTilgangTilOppslaattBruker(String innloggetIdent, String fnr) {
        String oppslaattAktoerId = aktorregisterConsumer.getAktorIdForFodselsnummer(new Fodselsnummer(fnr));
        try {
            return !(sporOmNoenAndreEnnSegSelvEllerEgneAnsatte(innloggetIdent, fnr)
                    || personConsumer.erBrukerKode6(oppslaattAktoerId));
        } catch (ForbiddenException e) {
            return false;
        }
    }

    @Cacheable(cacheNames = CACHENAME_TILGANG_IDENT, key = "#innloggetIdent.concat(#oppslaattFnr)", condition = "#innloggetIdent != null && #oppslaattFnr != null")
    public boolean sporOmNoenAndreEnnSegSelvEllerEgneAnsatte(String innloggetIdent, String oppslaattFnr) {
        return !(oppslaattFnr.equals(innloggetIdent) || brukertilgangConsumer.hasAccessToAnsatt(oppslaattFnr));
    }
}
