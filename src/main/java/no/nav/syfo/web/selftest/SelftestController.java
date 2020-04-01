package no.nav.syfo.web.selftest;

import no.nav.security.oidc.api.Unprotected;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/internal")
public class SelftestController {
    private static final String APPLICATION_LIVENESS = "Application is alive!";
    private static final String APPLICATION_READY = "Application is ready!";

    @Unprotected
    @RequestMapping(value = "/isAlive", produces = MediaType.TEXT_PLAIN_VALUE)
    public String isAlive() {
        return APPLICATION_LIVENESS;
    }

    @Unprotected
    @RequestMapping(value = "/isReady", produces = MediaType.TEXT_PLAIN_VALUE)
    public String isReady() {
        return APPLICATION_READY;
    }
}
