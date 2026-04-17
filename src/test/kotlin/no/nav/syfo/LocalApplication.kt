package no.nav.syfo

import no.nav.security.token.support.spring.test.MockLoginController
import no.nav.security.token.support.spring.test.MockOAuth2ServerAutoConfiguration
import org.springframework.boot.fromApplication
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.core.env.Environment
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

@TestConfiguration(proxyBeanMethods = false)
@Import(MockOAuth2ServerAutoConfiguration::class, MockLoginController::class)
class LocalApplication {
    @Bean
    @ServiceConnection
    fun postgresContainer(environment: Environment): PostgreSQLContainer<*> =
        PostgreSQLContainer<Nothing>(DockerImageName.parse("postgres:14-alpine")).apply {
            withDatabaseName("syfomotebehov")
            withUsername("postgres")
            withPassword("postgres")
            if (environment.activeProfiles.contains("local")) {
                withReuse(true)
            }
        }
}

fun main(args: Array<String>) {
    fromApplication<Application>().with(LocalApplication::class.java).run(*args)
}
