package no.nav.syfo.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;

import static java.util.Optional.ofNullable;
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

    boolean erMoteOpprettetForArbeidstakerEtterDato(String aktorId, LocalDateTime startDato) {
        log.info("L-TRACE: Skal hente om det finnes mote etter dato {}", startDato);
        String url = fromHttpUrl(syfomoteadminUrl)
                .pathSegment("system", aktorId, "harAktivtMote")
                .toUriString();

        log.info("L-TRACE: Kaller syfomoteadmin med url: {}", url);

        try {
            Boolean erMoteOpprettetEtterDato = template.postForObject(url, startDato, Boolean.class);
            if (!ofNullable(erMoteOpprettetEtterDato).isPresent()) {
                log.info("L-TRACE: Fikk null fra syfomoteadmin!");
                throw new RuntimeException(SYFOMOTEADMIN_FEILMELDING_NULL);
            }
            log.info("L-TRACE: Fikk erMoteOpprettet: {}", erMoteOpprettetEtterDato);
            return erMoteOpprettetEtterDato;
        } catch (Exception e) {
            log.info("L-TRACE: Fikk en exception fra syfomoteadmin", e);
            log.error("Det skjedde en feil ved henting av om det er mote i oppfolgingstilfelle fra syfomoteadmin");
            throw new RuntimeException(SYFOMOTEADMIN_FEILMELDING_GENERELL, e);
        }
    }
}
