package no.nav.syfo.config.ws;

import no.nav.syfo.consumer.util.ws.LogErrorHandler;
import no.nav.syfo.consumer.util.ws.STSClientConfig;
import no.nav.syfo.consumer.util.ws.WsClient;
import no.nav.tjeneste.domene.digisyfo.brukeroppgave.v1.BrukeroppgaveV1;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.Collections;

@Configuration
public class BrukeroppgaveConfig {
    @SuppressWarnings("unchecked")
    @Bean
    @ConditionalOnProperty(value = "mockBrukeroppgave_V1", havingValue = "false", matchIfMissing = true)
    @Primary
    public BrukeroppgaveV1 brukeroppgaveV1(@Value("${brukeroppgave.v1.endpointurl}") String serviceUrl) {
        BrukeroppgaveV1 port = new WsClient<BrukeroppgaveV1>().createPort(serviceUrl, BrukeroppgaveV1.class, Collections.singletonList(new LogErrorHandler()));
        STSClientConfig.configureRequestSamlToken(port);
        return port;
    }
}
