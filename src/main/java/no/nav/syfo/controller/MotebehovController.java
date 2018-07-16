package no.nav.syfo.controller;

import lombok.extern.slf4j.Slf4j;
import no.nav.security.oidc.OIDCConstants;
import no.nav.security.oidc.context.OIDCRequestContextHolder;
import no.nav.security.oidc.context.OIDCValidationContext;
import no.nav.security.spring.oidc.validation.api.ProtectedWithClaims;
import no.nav.syfo.consumer.ws.AktoerConsumer;
import no.nav.syfo.domain.rest.LagreMotebehov;
import no.nav.syfo.domain.rest.Motebehov;
import no.nav.syfo.domain.rest.Person;
import no.nav.syfo.repository.dao.DialogmotebehovDAO;
import no.nav.syfo.util.Toggle;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static no.nav.syfo.mappers.PersistencyMappers.rsMotebehov2p;
import static no.nav.syfo.mappers.RestMappers.dialogmotebehov2rs;
import static no.nav.syfo.util.MapUtil.map;
import static no.nav.syfo.util.MapUtil.mapListe;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Slf4j
@RestController
@ProtectedWithClaims(issuer = "selvbetjening", claimMap = {"acr=Level4"})
@RequestMapping(value = "/api")
public class MotebehovController {

    private OIDCRequestContextHolder contextHolder;
    private AktoerConsumer aktoerConsumer;
    private DialogmotebehovDAO dialogmotebehovDAO;

    public MotebehovController(final OIDCRequestContextHolder contextHolder,
                               final AktoerConsumer aktoerConsumer,
                               final DialogmotebehovDAO dialogmotebehovDAO) {
        this.contextHolder = contextHolder;
        this.aktoerConsumer = aktoerConsumer;
        this.dialogmotebehovDAO = dialogmotebehovDAO;
    }

    @ResponseBody
    @RequestMapping(value = "/motebehov", produces = APPLICATION_JSON_VALUE)
    public List<Motebehov> hentMotebehovListe(@PathVariable String fnr) {
        if (Toggle.endepunkterForMotebehov) {
            String arbeidstakerFnr = fnr.isEmpty() ? fnrFraOIDC() : fnr;
            return mapListe(dialogmotebehovDAO.hentDialogmotebehovListeForAktoer(aktoerConsumer.hentAktoerIdForFnr(arbeidstakerFnr)), dialogmotebehov2rs);
        } else {
            log.info("Det ble gjort kall mot 'motebehov', men dette endepunktet er togglet av.");
            return null;
        }
    }

    @RequestMapping(value = "/motebehov", consumes = APPLICATION_JSON_VALUE)
    public void opprettMotebehov(@RequestBody final LagreMotebehov lagreMotebehov) {
        if (Toggle.endepunkterForMotebehov) {
            Motebehov motebehov = mapLagremotebehovTilMotebehov(lagreMotebehov);

            dialogmotebehovDAO.create(map(motebehov, rsMotebehov2p));
        } else {
            log.info("Det ble gjort kall mot 'motebehov', men dette endepunktet er togglet av.");
        }
    }

    private Motebehov mapLagremotebehovTilMotebehov(LagreMotebehov lagreMotebehov) {
        String arbeidstakerFnr = lagreMotebehov.arbeidstakerFnr;
        lagreMotebehov.arbeidstakerFnr(arbeidstakerFnr.isEmpty() ? fnrFraOIDC() : arbeidstakerFnr);

        String innloggetAktoerId = aktoerConsumer.hentAktoerIdForFnr(fnrFraOIDC());

        return new Motebehov()
                .opprettetAv(innloggetAktoerId)
                .arbeidstaker(new Person()
                        .fnr(arbeidstakerFnr)
                )
                .motebehovSvar(lagreMotebehov.motebehovSvar());
    }

    private String fnrFraOIDC() {
        OIDCValidationContext context = (OIDCValidationContext) contextHolder
                .getRequestAttribute(OIDCConstants.OIDC_VALIDATION_CONTEXT);
        return context.getClaims("selvbetjening").getClaimSet().getSubject();
    }
}
