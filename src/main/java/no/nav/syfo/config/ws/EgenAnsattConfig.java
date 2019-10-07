package no.nav.syfo.config.ws;

import no.nav.syfo.consumer.util.ws.LogErrorHandler;
import no.nav.syfo.consumer.util.ws.STSClientConfig;
import no.nav.syfo.consumer.util.ws.WsClient;
import no.nav.tjeneste.pip.egen.ansatt.v1.EgenAnsattV1;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import static java.util.Collections.singletonList;

@Configuration
public class EgenAnsattConfig {
    /*createPort gir 'unchecked' warning ved kompilasjon pga. at det siste argumentet,
    er en vararg med generics som må implementere Message interfacet. For øyeblikket
    bruker ikke interceptors, så vi kan trygt suppresse den. */
    @SuppressWarnings("unchecked")
    @Bean
    @ConditionalOnProperty(value = "mockEgenAnsatt_V1", havingValue = "false", matchIfMissing = true)
    @Primary
    public EgenAnsattV1 egenAnsattV1(@Value("${egenansatt.v1.url}") String serviceUrl) {
        EgenAnsattV1 port = new WsClient<EgenAnsattV1>().createPort(serviceUrl, EgenAnsattV1.class, singletonList(new LogErrorHandler()));
        STSClientConfig.configureRequestSamlToken(port);
        return port;
    }
}
