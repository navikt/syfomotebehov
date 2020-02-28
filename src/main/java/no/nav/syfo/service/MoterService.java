package no.nav.syfo.service;

import lombok.extern.slf4j.Slf4j;
import no.nav.syfo.sts.StsConsumer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.time.LocalDateTime;

import static java.util.Optional.ofNullable;
import static org.springframework.web.util.UriComponentsBuilder.fromHttpUrl;

@Service
@Slf4j
public class MoterService {

    private final RestTemplate template;
    private final StsConsumer stsConsumer;
    private String syfomoteadminUrl;


    private static final String SYFOMOTEADMIN_FEILMELDING_NULL = "Fikk null fra syfomoteadmin ved henting av om arbeidstaker har mote i oppfolgingstilfellet";
    private static final String SYFOMOTEADMIN_FEILMELDING_GENERELL = "Klarte ikke hente om det er mote i oppfolgingstilfelle fra syfomoteadmin";

    public MoterService(
            RestTemplate template,
            StsConsumer stsConsumer,
            @Value("${syfomoteadminapi.url}") String syfomoteadminUrl) {
        this.template = template;
        this.stsConsumer = stsConsumer;
        this.syfomoteadminUrl = syfomoteadminUrl;
    }

    boolean erMoteOpprettetForArbeidstakerEtterDato(String aktorId, LocalDateTime startDato) {
        String stsToken = stsConsumer.token();
        HttpEntity<LocalDateTime> requestEntity = new HttpEntity<>(startDato, authorizationHeader(stsToken));

        String url = fromHttpUrl(syfomoteadminUrl)
                .pathSegment("system", aktorId, "harAktivtMote")
                .toUriString();

        try {
            Boolean erMoteOpprettetEtterDato = template.postForObject(url, requestEntity, Boolean.class);
            if (!ofNullable(erMoteOpprettetEtterDato).isPresent()) {
                throw new RuntimeException(SYFOMOTEADMIN_FEILMELDING_NULL);
            }
            return erMoteOpprettetEtterDato;
        } catch (Exception e) {
            log.error("Det skjedde en feil ved henting av om det er mote i oppfolgingstilfelle fra syfomoteadmin", e);
            throw new RuntimeException(SYFOMOTEADMIN_FEILMELDING_GENERELL, e);
        }
    }

    private HttpHeaders authorizationHeader(String token) {
        String bearerTokenCredentials = "Bearer " + token;
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.AUTHORIZATION, bearerTokenCredentials);
        return headers;
    }

}
