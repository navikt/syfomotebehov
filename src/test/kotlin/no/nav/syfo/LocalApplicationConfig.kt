package no.nav.syfo

import com.opentable.db.postgres.embedded.EmbeddedPostgres
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.token.support.spring.test.MockOAuth2ServerAutoConfiguration
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import javax.sql.DataSource

@Configuration
@Import(MockOAuth2ServerAutoConfiguration::class)
class LocalApplicationConfig {

    private var embeddedPostgres: EmbeddedPostgres = EmbeddedPostgres.builder().start()

    @Bean
    fun embeddedPostgres(): DataSource {
        return embeddedPostgres.postgresDatabase
    }

    @Bean
    fun mockOAuthServer(@Value("\${mock.token.server.port}") mockTokenServerPort: Int): MockOAuth2Server {
        val server = MockOAuth2Server()
        server.start(mockTokenServerPort)
        return server
    }
}
