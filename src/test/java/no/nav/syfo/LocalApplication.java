package no.nav.syfo;

import no.nav.security.spring.oidc.validation.api.EnableOIDCTokenValidation;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableOIDCTokenValidation(ignore="org.springframework")
public class LocalApplication {
    public static void main(String[] args) {
        SpringApplication.run(LocalApplication.class, args);
    }
}

