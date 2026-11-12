package no.nav.syfo.config

import io.kotest.core.extensions.ApplyExtension
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.shouldBe
import no.nav.syfo.IntegrationTest
import no.nav.syfo.LocalApplication
import org.flywaydb.core.Flyway
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.jdbc.core.JdbcTemplate

private const val DEV_DVH_USER = "disykefravar-developme-4c2f9@nav-data-union-restricted-prod.iam"
private const val PROD_DVH_USER = "disykefravar-sa@nav-data-union-restricted-prod.iam"

@TestConfiguration
@SpringBootTest(classes = [LocalApplication::class])
@ApplyExtension(SpringExtension::class)
class DvhGrantMigrationTest : IntegrationTest() {
    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    private lateinit var flyway: Flyway

    private fun migrate(naisCluster: String) {
        Flyway
            .configure()
            .configuration(flyway.configuration)
            .placeholders(mapOf("naisCluster" to naisCluster))
            .load()
            .migrate()
    }

    private fun canSelect(
        user: String,
        table: String,
    ): Boolean = jdbcTemplate.queryForObject("SELECT has_table_privilege(?, ?, 'SELECT')", Boolean::class.java, user, table)!!

    init {
        beforeEach {
            listOf(DEV_DVH_USER, PROD_DVH_USER).forEach { jdbcTemplate.execute("CREATE ROLE \"$it\"") }
        }
        afterEach {
            listOf(DEV_DVH_USER, PROD_DVH_USER).forEach {
                jdbcTemplate.execute("DROP OWNED BY \"$it\"")
                jdbcTemplate.execute("DROP ROLE \"$it\"")
            }
        }

        describe("DVH grants in afterMigrate") {
            it("grants only motebehov_dvh to the DVH user for the current cluster") {
                jdbcTemplate.execute("GRANT SELECT ON motebehov TO \"$DEV_DVH_USER\"")

                migrate("dev-gcp")

                canSelect(DEV_DVH_USER, "motebehov_dvh") shouldBe true
                canSelect(DEV_DVH_USER, "motebehov") shouldBe false
                canSelect(PROD_DVH_USER, "motebehov_dvh") shouldBe false
            }

            it("grants nothing outside a known cluster") {
                migrate("local")

                canSelect(DEV_DVH_USER, "motebehov_dvh") shouldBe false
                canSelect(PROD_DVH_USER, "motebehov_dvh") shouldBe false
            }

            it("skips the grant when the DVH user does not exist yet") {
                jdbcTemplate.execute("DROP ROLE \"$PROD_DVH_USER\"")

                migrate("prod-gcp")

                jdbcTemplate.execute("CREATE ROLE \"$PROD_DVH_USER\"")
                canSelect(PROD_DVH_USER, "motebehov_dvh") shouldBe false
            }
        }
    }
}
