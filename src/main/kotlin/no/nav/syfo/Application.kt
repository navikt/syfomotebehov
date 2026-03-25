package no.nav.syfo

import no.nav.security.token.support.spring.api.EnableJwtTokenValidation
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration
import org.springframework.boot.runApplication

@SpringBootApplication(
    exclude = [
        DataJpaRepositoriesAutoConfiguration::class,
        HibernateJpaAutoConfiguration::class,
    ],
)
@EnableJwtTokenValidation
class Application

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}
