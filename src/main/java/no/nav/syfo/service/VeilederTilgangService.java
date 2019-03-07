package no.nav.syfo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

import static java.util.Collections.singletonMap;
import static org.springframework.web.util.UriComponentsBuilder.fromHttpUrl;

@Service
public class VeilederTilgangService {

    public static final String FNR = "fnr";
    public static final String ENHET = "enhet";
    public static final String TILGANG_TIL_BRUKER_PATH = "/tilgangtilbruker";
    public static final String TILGANG_TIL_ENHET_PATH = "/tilgangtilenhet";
    private static final String FNR_PLACEHOLDER = "{" + FNR + "}";
    private static final String ENHET_PLACEHOLDER = "{" + ENHET + "}";
    private final RestTemplate template;
    private final UriComponentsBuilder tilgangTilBrukerUriTemplate;
    private final UriComponentsBuilder tilgangTilEnhetUriTemplate;

    public VeilederTilgangService(
            @Value("${tilgangskontrollapi.url}") String tilgangskontrollUrl, RestTemplate template
    ) {
        tilgangTilBrukerUriTemplate = fromHttpUrl(tilgangskontrollUrl)
                .path(TILGANG_TIL_BRUKER_PATH)
                .queryParam(FNR, FNR_PLACEHOLDER);
        tilgangTilEnhetUriTemplate = fromHttpUrl(tilgangskontrollUrl)
                .path(TILGANG_TIL_ENHET_PATH)
                .queryParam(ENHET, ENHET_PLACEHOLDER);
        this.template = template;
    }

    public boolean sjekkVeiledersTilgangTilPerson(String fnr) {
        URI tilgangTilBrukerUriMedFnr = tilgangTilBrukerUriTemplate.build(singletonMap(FNR, fnr));
        return kallUriMedTemplate(tilgangTilBrukerUriMedFnr);
    }

    public boolean sjekkVeiledersTilgangTilEnhet(String enhet) {
        URI tilgangTilEnhetUriMedFnr = tilgangTilEnhetUriTemplate.build(singletonMap(ENHET, enhet));
        return kallUriMedTemplate(tilgangTilEnhetUriMedFnr);
    }

    private boolean kallUriMedTemplate(URI uri) {
        try {
            template.getForObject(uri, Object.class);
            return true;
        } catch (HttpClientErrorException e) {
            if (e.getRawStatusCode() == 403) {
                return false;
            } else {
                throw e;
            }
        }
    }

}
