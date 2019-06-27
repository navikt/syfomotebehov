package no.nav.syfo.service;

import lombok.extern.slf4j.Slf4j;
import no.nav.syfo.domain.rest.OppfolgingstilfelleDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.inject.Inject;

import static org.springframework.web.util.UriComponentsBuilder.fromHttpUrl;

@Slf4j
@Service
public class SyketilfelleService {

    private RestTemplate restTemplate;
    private String syfosyketilfelleUrl;

    @Inject
    public SyketilfelleService(
            RestTemplate restTemplate,
            @Value("${syfosyketilfelleApi.url}") String syfosyketilfelleUrl
    ) {
        this.restTemplate = restTemplate;
        this.syfosyketilfelleUrl = syfosyketilfelleUrl;
    }


    public OppfolgingstilfelleDTO hentNyesteOppfolgingstilfelle(String aktorId) {
        log.info("L-TRACE: Skal hente oppfolgingstilfelle fra syfosyketilfelle");
        String url = fromHttpUrl(syfosyketilfelleUrl)
                .pathSegment("oppfolgingstilfelle", "beregn", "syfomotebehov", aktorId)
                .toUriString();
        log.info("L-TRACE: Henter oppfolgingstilfelle med url: {}", url);

        try {
            return restTemplate.getForObject(url, OppfolgingstilfelleDTO.class);
        } catch (Exception e) {
            log.info("L-TRACE: Fikk exception fra syfosyketilfelle!", e);
            log.error("Det skjedde en feil ved henting av oppfolgingstilfelle fra syfosyketilfelle");
            throw new RuntimeException("Klarte ikke hente oppfolgingstilfelle fra syfosyketilfelle", e);
        }
    }
}
