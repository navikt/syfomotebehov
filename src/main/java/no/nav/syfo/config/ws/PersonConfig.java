package no.nav.syfo.config.ws;

import no.nav.syfo.consumer.util.ws.LogErrorHandler;
import no.nav.syfo.consumer.util.ws.STSClientConfig;
import no.nav.tjeneste.virksomhet.person.v3.binding.PersonV3;
import no.nav.syfo.consumer.util.ws.WsClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import static java.util.Collections.singletonList;

@Configuration
public class PersonConfig {
    @SuppressWarnings("unchecked")
    @Bean
    @ConditionalOnProperty(value = "mockPerson_V3", havingValue = "false", matchIfMissing = true)
    @Primary
    public PersonV3 personV3(@Value("${virksomhet.person.v3.endpointurl}") String serviceUrl) {
        PersonV3 port = new WsClient<PersonV3>().createPort(serviceUrl, PersonV3.class, singletonList(new LogErrorHandler()));
        STSClientConfig.configureRequestSamlToken(port);
        return port;
    }

}
