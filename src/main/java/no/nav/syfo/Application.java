package no.nav.syfo;

import no.nav.security.spring.oidc.api.EnableOIDCTokenValidation;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;

@SpringBootApplication
@EnableAutoConfiguration(exclude = {JpaRepositoriesAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
@EnableOIDCTokenValidation(ignore = "org.springframework")
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
