package no.nav.syfo

import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.fromApplication
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.springframework.core.env.Environment
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

@TestConfiguration(proxyBeanMethods = false)
@EnableMockOAuth2Server
class LocalApplication {
    @Value("\${mock-oauth2-server.port}")
    lateinit var oauth2Port: String

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
