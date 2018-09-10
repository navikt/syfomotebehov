package no.nav.syfo.config.ws;

import no.nav.syfo.consumer.util.ws.LogErrorHandler;
import no.nav.syfo.consumer.util.ws.WsClient;
import no.nav.tjeneste.virksomhet.brukerprofil.v3.BrukerprofilV3;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import static java.util.Collections.singletonList;

@Configuration
public class BrukerprofilConfig {
    @SuppressWarnings("unchecked")
    @Bean
    @ConditionalOnProperty(value = "mockBrukerprofil_V3", havingValue = "false", matchIfMissing = true)
    @Primary
    public BrukerprofilV3 brukerprofilV3(@Value("${brukerprofil.v3.endpointurl}") String serviceUrl) {
        return new WsClient<BrukerprofilV3>().createPort(serviceUrl, BrukerprofilV3.class, singletonList(new LogErrorHandler()));
    }

}
