package no.nav.syfo.localconfig;

import no.nav.security.spring.oidc.test.TokenGeneratorConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;

@Configuration
@Import(TokenGeneratorConfiguration.class)
public class LocalApplicationConfig {

    public LocalApplicationConfig(Environment environment) {
        /*
            Her kan du ta inn properties som normalt settes av platformen slik at de er tilgjengelige runtime lokalt
            Eks: System.setProperty("syfo-dialogmotebehov_USERNAME", environment.getProperty("syfo-dialogmotebehov.username"));
         */

        System.setProperty("SECURITYTOKENSERVICE_URL", environment.getProperty("securitytokenservice.url"));
        System.setProperty("SRVSYFO-DIALOGMOTEBEHOV_USERNAME", environment.getProperty("srvsyfo-dialogmotebehov.username"));
        System.setProperty("SRVSYFO-DIALOGMOTEBEHOV_PASSWORD", environment.getProperty("srvsyfo-dialogmotebehov.password"));
    }
}
