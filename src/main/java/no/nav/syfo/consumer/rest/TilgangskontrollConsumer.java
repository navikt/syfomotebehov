package no.nav.syfo.consumer.rest;

import lombok.extern.slf4j.Slf4j;
import no.nav.security.oidc.OIDCConstants;
import no.nav.security.oidc.context.OIDCRequestContextHolder;
import no.nav.security.oidc.context.OIDCValidationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.inject.Inject;
import javax.ws.rs.core.Response;

import static org.springframework.web.util.UriComponentsBuilder.fromHttpUrl;

@Service
@Slf4j
public class TilgangskontrollConsumer {

    private OIDCRequestContextHolder contextHolder;

    private final RestTemplate template;
    private final String dev;
    private final String syfotilgangskontrollUrl;

    @Inject
    public TilgangskontrollConsumer(final OIDCRequestContextHolder contextHolder,
                                    @Value("${dev}") String dev,
                                    RestTemplate template,
                                    @Value("${tilgangskontrollapi.url}") String syfotilgangskontrollUrl) {
        this.contextHolder = contextHolder;
        this.dev = dev;
        this.template = template;
        this.syfotilgangskontrollUrl = syfotilgangskontrollUrl;
    }

    public boolean harTilgangTilOppslaattBruker(String fnr) {
        if ("true".equals(dev)) {
            return true;
        }

        String url = fromHttpUrl(syfotilgangskontrollUrl)
                .path("/bruker/selvEllerEgneAnsatte")
                .queryParam("fnr", fnr)
                .toUriString();

        log.info("Sjekker tilgang på URL: {} for {}", url, fnr);

        Response response = template.getForObject(
                url,
                Response.class
        );

        int status = response.getStatus();
        if (403 == status) {
            return false;
        } else if (200 != status) {
            log.error("Sjekking av tilgang til bruker {} feiler med HTTP-{}", status, fnr);
            throw new RuntimeException("Sjekking av tilgang til bruker feiler med HTTP-" + status);
        } else {
            return true;
        }
    }

//    private HttpHeaders getHeaders() {
//        final HttpHeaders headers = new HttpHeaders();
//        headers.setContentType(MediaType.APPLICATION_JSON);
//        headers.set(OIDCConstants.AUTHORIZATION_HEADER, "Bearer " + tokenFraOIDC());
//        return headers;
//    }
//
//    private String tokenFraOIDC() {
//        OIDCValidationContext context = (OIDCValidationContext) contextHolder
//                .getRequestAttribute(OIDCConstants.AUTHORIZATION_HEADER);
//        return context.getClaims("selvbetjening").getClaimSet().getSubject();
//    }
}
