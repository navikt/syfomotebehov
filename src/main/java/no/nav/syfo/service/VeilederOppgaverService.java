package no.nav.syfo.service;

import lombok.extern.slf4j.Slf4j;
import no.nav.syfo.domain.rest.VeilederOppgave;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.inject.Inject;
import java.util.List;

import static no.nav.syfo.util.AuthorizationFilterUtils.basicCredentials;
import static org.springframework.web.util.UriComponentsBuilder.fromHttpUrl;

@Slf4j
@Service
public class VeilederOppgaverService {

    private final RestTemplate restTemplate;
    private final String syfoveilederoppgaverUrl;

    @Inject
    public VeilederOppgaverService(
            @Value("${syfoveilederoppgaver_system_v1.url}") String syfoveilederoppgaverUrl,
            RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.syfoveilederoppgaverUrl = syfoveilederoppgaverUrl;
    }

    List<VeilederOppgave> get(String fnr) {
        String url = fromHttpUrl(syfoveilederoppgaverUrl)
                .queryParam("fnr", fnr)
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", basicCredentials("SYFOVEILEDEROPPGAVER_SYSTEMAPI"));
        HttpEntity<String> request = new HttpEntity<>(headers);

        log.info("Henter møtebehov på URL: {}", url);

        ResponseEntity<List<VeilederOppgave>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                request,
                new ParameterizedTypeReference<List<VeilederOppgave>>() {
                });

        return response.getBody();
    }
}
