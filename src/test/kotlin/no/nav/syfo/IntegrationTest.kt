package no.nav.syfo

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.extensions.testcontainers.perSpec
import io.mockk.clearAllMocks
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

@DirtiesContext
abstract class IntegrationTest : DescribeSpec() {
    companion object {
        private val postgresContainer =
            PostgreSQLContainer<Nothing>(DockerImageName.parse("postgres:15-alpine")).apply {
                withDatabaseName("syfomotebehov")
                withUsername("postgres")
                withPassword("postgres")
                withReuse(true)
            }

        @JvmStatic
        @DynamicPropertySource
        fun registerPgProperties(registry: DynamicPropertyRegistry) {
            postgresContainer.start()
            registry.add("spring.datasource.url", postgresContainer::getJdbcUrl)
            registry.add("spring.datasource.username", postgresContainer::getUsername)
            registry.add("spring.datasource.password", postgresContainer::getPassword)
        }
    }
    init {
        listener(postgresContainer.perSpec())
        afterTest {
            clearAllMocks()
        }
    }
}
