package no.nav.syfo.controller.internal;


import no.nav.security.oidc.api.Unprotected;
import no.nav.syfo.repository.dao.MotebehovDAO;
import no.nav.syfo.util.Toggle;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Stream;

import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@Unprotected
@RequestMapping(value = "/internal")
public class NullstillController {

    private static final Logger log = getLogger(NullstillController.class);

    private final MotebehovDAO motebehovDAO;

    public NullstillController(MotebehovDAO motebehovDAO) {
        this.motebehovDAO = motebehovDAO;
    }

    @RequestMapping(value = "/nullstill/{aktoerId}", produces = APPLICATION_JSON_VALUE)
    public String slettMotebehov(@PathVariable String aktoerId, @Value("${nais.cluster.name:ukjent}") String env) {
        if (Toggle.enableNullstill || Stream.of("q1", "local").anyMatch(env::equals)) {
            log.info("Sletter alle møtebehov på aktørid: {}", aktoerId);
            int antallSlettedeMotebehov = motebehovDAO.nullstillMotebehov(aktoerId);
            return "Slettet " + antallSlettedeMotebehov + " møtebehov.";
        } else {
            log.info("Det ble gjort kall mot 'nullstill', men dette endepunktet er togglet av og skal aldri brukes i prod.");
            return "Toggle er av, eller er sletting ikke tilgjengelig i " + env;
        }
    }
}
