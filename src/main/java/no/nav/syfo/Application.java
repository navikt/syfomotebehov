package no.nav.syfo;

import no.nav.security.spring.oidc.validation.api.EnableOIDCTokenValidation;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import static java.lang.System.getProperty;
import static java.lang.System.setProperty;

@SpringBootApplication
@EnableOIDCTokenValidation(ignore="org.springframework")
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
