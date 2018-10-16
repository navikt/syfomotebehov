package no.nav.syfo.config.ws;

import no.nav.syfo.consumer.util.ws.LogErrorHandler;
import no.nav.syfo.consumer.util.ws.WsClient;
import no.nav.tjeneste.virksomhet.organisasjonenhet.v2.OrganisasjonEnhetV2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import static java.util.Collections.singletonList;

@Configuration
public class OrganisasjonEnhetConfig {
    @SuppressWarnings("unchecked")
    @Bean
    @ConditionalOnProperty(value = "mockOrganisasjonEnhet_V2", havingValue = "false", matchIfMissing = true)
    @Primary
    public OrganisasjonEnhetV2 organisasjonEnhetV2 (@Value("${virksomhet.organisasjonEnhet.v2.endpointurl}") String serviceUrl) {
        return new WsClient<OrganisasjonEnhetV2>().createPort(serviceUrl, OrganisasjonEnhetV2.class, singletonList(new LogErrorHandler()));
    }
}
