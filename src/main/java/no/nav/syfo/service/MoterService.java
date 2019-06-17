package no.nav.syfo.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import static org.springframework.web.util.UriComponentsBuilder.fromHttpUrl;

@Service
@Slf4j
public class MoterService {

    private final RestTemplate template;
    private String syfomoteadminUrl;

    private static final String SYFOMOTEADMIN_FEILMELDING_NULL = "Fikk null fra syfomoteadmin ved henting av om arbeidstaker har mote i oppfolgingstilfellet";
    private static final String SYFOMOTEADMIN_FEILMELDING_GENERELL = "Klarte ikke hente om det er mote i oppfolgingstilfelle fra syfomoteadmin";

    public MoterService(
            RestTemplate template,
            @Value("${syfomoteadminapi.url}") String syfomoteadminUrl) {
        this.template = template;
        this.syfomoteadminUrl = syfomoteadminUrl;
    }

    boolean harArbeidstakerMoteIOppfolgingstilfelle(String aktorId) {
        String url = fromHttpUrl(syfomoteadminUrl)
                .pathSegment("system", aktorId, "harAktivtMote")
                .toUriString();

        try {
            Boolean harMoteIOppfolgingstilfellet = template.getForObject(url, Boolean.class);
            if (harMoteIOppfolgingstilfellet != null) {
                return harMoteIOppfolgingstilfellet;
            } else {
                throw new RuntimeException(SYFOMOTEADMIN_FEILMELDING_NULL);
            }
        } catch (Exception e) {
            log.error("Det skjedde en feil ved henting av om det er mote i oppfolgingstilfelle fra syfomoteadmin");
            throw new RuntimeException(SYFOMOTEADMIN_FEILMELDING_GENERELL, e);
        }
    }
}
