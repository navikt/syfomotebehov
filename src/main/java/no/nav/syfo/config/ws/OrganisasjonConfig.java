package no.nav.syfo.config.ws;

import no.nav.syfo.consumer.util.ws.LogErrorHandler;
import no.nav.syfo.consumer.util.ws.STSClientConfig;
import no.nav.syfo.consumer.util.ws.WsClient;
import no.nav.tjeneste.virksomhet.organisasjon.v4.OrganisasjonV4;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import static java.util.Collections.singletonList;

@Configuration
public class OrganisasjonConfig {
    /*createPort gir 'unchecked' warning ved kompilasjon pga. at det siste argumentet,
    er en vararg med generics som må implementere Message interfacet. For øyeblikket
    bruker ikke interceptors, så vi kan trygt suppresse den. */
    @SuppressWarnings("unchecked")
    @Bean
    @ConditionalOnProperty(value = "mockOrganisasjon_V4", havingValue = "false", matchIfMissing = true)
    @Primary
    public OrganisasjonV4 organisasjonV4 (@Value("${virksomhet.organisasjon.v4.endpointurl}") String serviceUrl) {
        OrganisasjonV4 port = new WsClient<OrganisasjonV4>().createPort(serviceUrl, OrganisasjonV4.class, singletonList(new LogErrorHandler()));
        STSClientConfig.configureRequestSamlToken(port);
        return port;
    }
}
