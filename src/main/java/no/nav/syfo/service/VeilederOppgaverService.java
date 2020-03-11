package no.nav.syfo.service;

import no.nav.syfo.domain.rest.VeilederOppgave;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.inject.Inject;
import java.util.List;

import static no.nav.syfo.util.AuthorizationFilterUtils.basicCredentials;
import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.web.util.UriComponentsBuilder.fromHttpUrl;

@Service
public class VeilederOppgaverService {

    private static final Logger log = getLogger(VeilederOppgaverService.class);

    private final RestTemplate restTemplate;
    private final String syfoveilederoppgaverUrl;
    private final String credUsername;
    private final String credPassword;

    @Inject
    public VeilederOppgaverService(
            @Value("${syfoveilederoppgaver.system.v1.url}") String syfoveilederoppgaverUrl,
            @Value("${syfoveilederoppgaver.systemapi.username}") String credUsername,
            @Value("${syfoveilederoppgaver.systemapi.password}") String credPassword,
            RestTemplate restTemplate
    ) {
        this.restTemplate = restTemplate;
        this.syfoveilederoppgaverUrl = syfoveilederoppgaverUrl;
        this.credUsername = credUsername;
        this.credPassword = credPassword;
    }

    public List<VeilederOppgave> getVeilederoppgave(String fnr) {
        String url = fromHttpUrl(syfoveilederoppgaverUrl)
                .queryParam("fnr", fnr)
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", basicCredentials(credUsername, credPassword));
        HttpEntity<String> request = new HttpEntity<>(headers);

        log.info("Henter møtebehov på URL: {}", maskerFnrFraUrl(url));

        ResponseEntity<List<VeilederOppgave>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                request,
                new ParameterizedTypeReference<List<VeilederOppgave>>() {
                });

        return response.getBody();
    }

    private String maskerFnrFraUrl(String url) {
        return url.replaceAll("[0-9]", "*");
    }
}
