package no.nav.syfo.config;

import no.nav.security.oidc.context.OIDCRequestContextHolder;
import no.nav.security.spring.oidc.validation.api.EnableOIDCTokenValidation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.client.RestTemplate;

import static java.util.Arrays.asList;


@Configuration
@EnableTransactionManagement
@EnableOIDCTokenValidation(ignore = "org.springframework")
public class ApplicationConfig {

    @Bean
    public RestTemplate restTemplate(ClientHttpRequestInterceptor... interceptors) {
        RestTemplate template = new RestTemplate();
        template.setInterceptors(asList(interceptors));
        return template;
    }

    @Autowired
    private OIDCRequestContextHolder oidcRequestContextHolder;
}


