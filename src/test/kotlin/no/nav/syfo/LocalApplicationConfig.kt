package no.nav.syfo

import no.nav.security.oidc.test.support.spring.TokenGeneratorConfiguration
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration
@Import(TokenGeneratorConfiguration::class)
class LocalApplicationConfig
