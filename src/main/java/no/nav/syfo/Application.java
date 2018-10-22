package no.nav.syfo;

import no.nav.security.spring.oidc.validation.api.EnableOIDCTokenValidation;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
//import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
//@EnableCaching
@EnableOIDCTokenValidation(ignore="org.springframework")
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
