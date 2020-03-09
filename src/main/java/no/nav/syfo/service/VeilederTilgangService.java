package no.nav.syfo.service;

import no.nav.security.oidc.context.OIDCRequestContextHolder;
import no.nav.syfo.domain.rest.Fnr;
import no.nav.syfo.oidc.OIDCIssuer;
import no.nav.syfo.util.OIDCUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Collections;

import static java.util.Collections.singletonMap;
import static no.nav.syfo.util.CredentialUtilKt.bearerCredentials;
import static org.springframework.web.util.UriComponentsBuilder.fromHttpUrl;

@Service
public class VeilederTilgangService {

    private final OIDCRequestContextHolder oidcContextHolder;

    public static final String FNR = "fnr";
    public static final String TILGANG_TIL_BRUKER_VIA_AZURE_PATH = "/bruker";
    private static final String FNR_PLACEHOLDER = "{" + FNR + "}";
    private final RestTemplate template;
    private final UriComponentsBuilder tilgangTilBrukerViaAzureUriTemplate;

    public VeilederTilgangService(
            @Value("${tilgangskontrollapi.url}") String tilgangskontrollUrl,
            RestTemplate template,
            OIDCRequestContextHolder oidcContextHolder
    ) {
        tilgangTilBrukerViaAzureUriTemplate = fromHttpUrl(tilgangskontrollUrl)
                .path(TILGANG_TIL_BRUKER_VIA_AZURE_PATH)
                .queryParam(FNR, FNR_PLACEHOLDER);
        this.template = template;
        this.oidcContextHolder = oidcContextHolder;
    }

    public boolean sjekkVeiledersTilgangTilPersonViaAzure(Fnr fnr) {
        URI tilgangTilBrukerViaAzureUriMedFnr = tilgangTilBrukerViaAzureUriTemplate.build(singletonMap(FNR, fnr.getFnr()));
        return checkAccess(tilgangTilBrukerViaAzureUriMedFnr, OIDCIssuer.AZURE);
    }

    private boolean checkAccess(URI uri, String oidcIssuer) {
        try {
            template.exchange(
                    uri,
                    HttpMethod.GET,
                    createEntity(oidcIssuer),
                    String.class
            );
            return true;
        } catch (HttpClientErrorException e) {
            if (e.getRawStatusCode() == 403) {
                return false;
            } else {
                throw e;
            }
        }
    }

    private HttpEntity<String> createEntity(String issuer) {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set(HttpHeaders.AUTHORIZATION, bearerCredentials(OIDCUtil.tokenFraOIDC(oidcContextHolder, issuer)));
        return new HttpEntity<>(headers);
    }
}
