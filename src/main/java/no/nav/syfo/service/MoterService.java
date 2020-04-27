package no.nav.syfo.service;

import no.nav.syfo.sts.StsConsumer;
import no.nav.syfo.metric.Metrikk;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.*;

import java.time.LocalDateTime;

import static no.nav.syfo.util.CredentialUtilKt.bearerCredentials;
import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.web.util.UriComponentsBuilder.fromHttpUrl;

@Service
public class MoterService {

    private static final Logger log = getLogger(MoterService.class);

    private final RestTemplate template;
    private final StsConsumer stsConsumer;
    private final Metrikk metrikk;
    private final String syfomoteadminUrl;
    private static final String SYFOMOTEADMIN_FEILMELDING_GENERELL = "Klarte ikke hente om det er mote i oppfolgingstilfelle fra syfomoteadmin";
    private static final String SYFOMOTEADMIN_FEILMELDING_KLIENT = "Fikk 4XX-feil ved henting av om det er mote i oppfolgingstilfelle fra syfomoteadmin";
    private static final String SYFOMOTEADMIN_FEILMELDING_SERVER = "Fikk 5XX-feil ved henting av om det er mote i oppfolgingstilfelle fra syfomoteadmin";

    public MoterService(
            RestTemplate template,
            StsConsumer stsConsumer,
            Metrikk metrikk,
            @Value("${syfomoteadminapi.url}") String syfomoteadminUrl) {
        this.template = template;
        this.stsConsumer = stsConsumer;
        this.metrikk = metrikk;
        this.syfomoteadminUrl = syfomoteadminUrl;
    }

    public boolean erMoteOpprettetForArbeidstakerEtterDato(String aktorId, LocalDateTime startDato) {
        String stsToken = stsConsumer.token();
        HttpEntity<LocalDateTime> requestEntity = new HttpEntity<>(startDato, authorizationHeader(stsToken));

        String url = fromHttpUrl(syfomoteadminUrl)
                .pathSegment("system", aktorId, "harAktivtMote")
                .toUriString();

        try {
            metrikk.tellHendelse("call_syfomoteadmin");
            Boolean erMoteOpprettetEtterDato = template.postForObject(url, requestEntity, Boolean.class);
            metrikk.tellHendelse("call_syfomoteadmin_success");
            return erMoteOpprettetEtterDato;
        } catch (HttpClientErrorException e) {
            log.warn(SYFOMOTEADMIN_FEILMELDING_KLIENT);
            throw e;
        } catch (HttpServerErrorException e) {
            metrikk.tellHendelse("call_syfomoteadmin_fail");
            log.error(SYFOMOTEADMIN_FEILMELDING_SERVER, e);
            throw e;
        } catch (RuntimeException e) {
            log.error(SYFOMOTEADMIN_FEILMELDING_GENERELL, e);
            throw e;
        }
    }

    private HttpHeaders authorizationHeader(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.AUTHORIZATION, bearerCredentials(token));
        return headers;
    }
}
