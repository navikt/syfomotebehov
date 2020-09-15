package no.nav.syfo

import no.nav.security.token.support.spring.api.EnableJwtTokenValidation
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@EnableJwtTokenValidation
@SpringBootApplication
class LocalApplication

fun main(args: Array<String>) {
    runApplication<LocalApplication>(*args)
}
