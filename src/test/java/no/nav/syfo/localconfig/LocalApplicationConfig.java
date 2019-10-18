package no.nav.syfo.localconfig;

import no.nav.security.spring.oidc.test.TokenGeneratorConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;

import static java.util.Arrays.asList;

@Configuration
@Import(TokenGeneratorConfiguration.class)
public class LocalApplicationConfig {

    public LocalApplicationConfig(Environment environment) {
        /*
            Her kan du ta inn properties som normalt settes av platformen slik at de er tilgjengelige runtime lokalt
            Eks: System.setProperty("syfomotebehov_USERNAME", environment.getProperty("syfomotebehov.username"));
         */

        System.setProperty("SECURITYTOKENSERVICE_URL", environment.getProperty("securitytokenservice.url"));
        System.setProperty("SRV_USERNAME", environment.getProperty("srvsyfomotebehov.username"));
        System.setProperty("SRV_PASSWORD", environment.getProperty("srvsyfomotebehov.password"));
    }

    @Bean
    public RestTemplate restTemplate(ClientHttpRequestInterceptor... interceptors) {
        RestTemplate template = new RestTemplate();
        template.setInterceptors(asList(interceptors));
        return template;
    }
}
