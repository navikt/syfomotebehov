package no.nav.syfo.config.ws;

import no.nav.tjeneste.virksomhet.arbeidsfordeling.v1.ArbeidsfordelingV1;
import no.nav.syfo.consumer.util.ws.LogErrorHandler;
import no.nav.syfo.consumer.util.ws.STSClientConfig;
import no.nav.syfo.consumer.util.ws.WsClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.Collections;

@Configuration
public class ArbeidsfordelingConfig {
    /*createPort gir 'unchecked' warning ved kompilasjon pga. at det siste argumentet,
    er en vararg med generics som må implementere Message interfacet. For øyeblikket
    bruker ikke interceptors, så vi kan trygt suppresse den. */
    @SuppressWarnings("unchecked")
    @Bean
    @ConditionalOnProperty(value= "mockArbeidsfordeling_V1", havingValue = "false", matchIfMissing = true)
    @Primary
    public ArbeidsfordelingV1 arbeidsfordelingV1(@Value("${arbeidsfordeling.v1.url}") String serviceUrl) {
        ArbeidsfordelingV1 port = new WsClient<ArbeidsfordelingV1>().createPort(serviceUrl, ArbeidsfordelingV1.class, Collections.singletonList(new LogErrorHandler()));
        STSClientConfig.configureRequestSamlToken(port);
        return port;
    }
}
