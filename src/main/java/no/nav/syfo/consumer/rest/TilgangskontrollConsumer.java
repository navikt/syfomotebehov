package no.nav.syfo.consumer.rest;

import lombok.extern.slf4j.Slf4j;
import no.nav.security.oidc.OIDCConstants;
import no.nav.security.oidc.context.OIDCRequestContextHolder;
import no.nav.syfo.domain.rest.Tilgang;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.inject.Inject;

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
                .path("/tilgangTilselvEllerEgneAnsatte")
                .queryParam("fnr", fnr)
                .toUriString();

        log.info("Sjekker tilgang på URL: {} for {}", url, fnr);


        ResponseEntity<Tilgang> response = template.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(getHeaders()),
                Tilgang.class
        );
        log.info("Fikk response med innhold {}", response.getBody().harTilgang);

        HttpStatus status = response.getStatusCode();
        log.info("Fikk response med stattus {}", status);
        if (status == HttpStatus.FORBIDDEN) {
            return false;
        } else if (status != HttpStatus.OK) {
            log.error("Sjekking av tilgang til bruker {} feiler med HTTP-{}", status, fnr);
            throw new RuntimeException("Sjekking av tilgang til bruker feiler med HTTP-" + status);
        } else {
            return true;
        }
    }

    private HttpHeaders getHeaders() {
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(OIDCConstants.AUTHORIZATION_HEADER, "Bearer " + tokenFraOIDC());
        return headers;
    }

    private String tokenFraOIDC() {
        return contextHolder
                .getOIDCValidationContext()
                .getToken("selvbetjening")
                .getIdToken();
    }
}
