package no.nav.syfo

import org.springframework.boot.fromApplication
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

@TestConfiguration(proxyBeanMethods = false)
class LocalApplication {
    @Bean
    @ServiceConnection
    fun postgresContainer(): PostgreSQLContainer<*> =
        PostgreSQLContainer<Nothing>(DockerImageName.parse("postgres:14-alpine")).apply {
            withDatabaseName("syfomotebehov")
            withUsername("postgres")
            withPassword("postgres")
            withReuse(true)
        }
}

fun main(args: Array<String>) {
    fromApplication<Application>().with(LocalApplication::class.java).run(*args)
}
