package no.nav.syfo.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import org.slf4j.LoggerFactory
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy
import java.sql.Connection

class PsqlContainer : PostgreSQLContainer<PsqlContainer>("postgres:15-alpine")

private val log = LoggerFactory.getLogger(TestDB::class.qualifiedName)

class TestDatabase(
    private val connectionName: String,
    private val dbUsername: String,
    private val dbPassword: String,
) {
    private val dataSource: HikariDataSource =
        HikariDataSource(
            HikariConfig().apply {
                jdbcUrl = connectionName
                username = dbUsername
                password = dbPassword
                maximumPoolSize = 1
                minimumIdle = 1
                isAutoCommit = false
                connectionTimeout = 10_000
                transactionIsolation = "TRANSACTION_REPEATABLE_READ"
                validate()
            },
        )
    val connection: Connection
        get() = dataSource.connection

    init {
        runFlywayMigrations()
    }

    private fun runFlywayMigrations() =
        Flyway.configure().run {
            locations("db")
            configuration(mapOf("flyway.postgresql.transactional.lock" to "false"))
            dataSource(connectionName, dbUsername, dbPassword)
            load().migrate()
        }
}

class TestDB private constructor() {
    companion object {
        val database: TestDatabase

        private val psqlContainer: PsqlContainer

        init {
            try {
                psqlContainer =
                    PsqlContainer()
                        .withExposedPorts(5432)
                        .withUsername("username")
                        .withPassword("password")
                        .withDatabaseName("database")

                psqlContainer.waitingFor(HostPortWaitStrategy())
                psqlContainer.start()
                val username = "username"
                val password = "password"
                val connectionName = psqlContainer.jdbcUrl

                database = TestDatabase(connectionName, username, password)
            } catch (ex: Exception) {
                log.error("Error", ex)
                throw ex
            }
        }

        fun clearAllData() =
            database.connection.use {
                it
                    .prepareStatement(
                        """
                    DELETE FROM FOLLOW_UP_PLAN_LPS_V1;
                    DELETE FROM SYKMELDINGSPERIODE;
                    DELETE FROM ALTINN_LPS;
                """,
                    ).use { ps -> ps.executeUpdate() }
                it.commit()
            }
    }
}
