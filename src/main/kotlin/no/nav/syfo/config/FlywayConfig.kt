package no.nav.syfo.config

import org.flywaydb.core.Flyway
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationInitializer
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import javax.sql.DataSource

@Configuration
class FlywayConfig {
    @Bean
    fun flyway(dataSource: DataSource) = Flyway().apply {
        setDataSource(dataSource)
    }

    @Bean
    fun flywayMigrationInitializer(flyway: Flyway, flywayMigrationStrategy: FlywayMigrationStrategy): FlywayMigrationInitializer =
            FlywayMigrationInitializer(flyway, null)

}
