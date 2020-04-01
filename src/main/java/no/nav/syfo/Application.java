package no.nav.syfo;

import no.nav.security.spring.oidc.api.EnableOIDCTokenValidation;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;

@SpringBootApplication(exclude = {
        JpaRepositoriesAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class,
        FlywayAutoConfiguration.class
})
@EnableOIDCTokenValidation
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
