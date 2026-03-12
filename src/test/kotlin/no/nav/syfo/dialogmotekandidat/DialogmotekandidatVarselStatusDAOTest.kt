package no.nav.syfo.dialogmotekandidat

import io.kotest.core.extensions.ApplyExtension
import io.kotest.extensions.spring.SpringExtension
import no.nav.syfo.IntegrationTest
import no.nav.syfo.LocalApplication
import no.nav.syfo.dialogmotekandidat.database.DialogmotekandidatVarselStatusDAO
import no.nav.syfo.dialogmotekandidat.database.DialogmotekandidatVarselType
import org.assertj.core.api.Assertions.assertThat
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.jdbc.core.JdbcTemplate
import java.time.LocalDateTime
import java.util.UUID

@TestConfiguration
@SpringBootTest(classes = [LocalApplication::class])
@ApplyExtension(SpringExtension::class)
class DialogmotekandidatVarselStatusDAOTest : IntegrationTest() {

    @Autowired
    private lateinit var varselStatusDAO: DialogmotekandidatVarselStatusDAO

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    init {
        beforeTest {
            jdbcTemplate.update("DELETE FROM dialogkandidat_varsel_status")
        }

        describe("DialogmotekandidatVarselStatusDAO") {

            it("create oppretter en PENDING-rad med riktig type") {
                val uuid = UUID.randomUUID().toString()
                val fnr = "12345678901"

                varselStatusDAO.create(uuid, fnr, DialogmotekandidatVarselType.VARSEL)

                val pending = varselStatusDAO.getPendingByType(DialogmotekandidatVarselType.VARSEL)
                assertThat(pending).hasSize(1)
                assertThat(pending[0].kafkaMeldingUuid).isEqualTo(uuid)
                assertThat(pending[0].fnr).isEqualTo(fnr)
                assertThat(pending[0].type).isEqualTo(DialogmotekandidatVarselType.VARSEL)
                assertThat(pending[0].status).isEqualTo(DialogmotekandidatVarselStatusDAO.STATUS_PENDING)
                assertThat(pending[0].retryCount).isEqualTo(0)
            }

            it("create med duplikat uuid gjør ingenting") {
                val uuid = UUID.randomUUID().toString()
                val fnr = "12345678901"

                varselStatusDAO.create(uuid, fnr, DialogmotekandidatVarselType.VARSEL)
                varselStatusDAO.create(uuid, fnr, DialogmotekandidatVarselType.VARSEL)

                val pending = varselStatusDAO.getPendingByType(DialogmotekandidatVarselType.VARSEL)
                assertThat(pending).hasSize(1)
            }

            it("updateStatusToSent setter status til SENT") {
                val uuid = UUID.randomUUID().toString()
                val fnr = "12345678901"

                varselStatusDAO.create(uuid, fnr, DialogmotekandidatVarselType.VARSEL)
                val created = varselStatusDAO.getPendingByType(DialogmotekandidatVarselType.VARSEL)
                assertThat(created).hasSize(1)

                varselStatusDAO.updateStatusToSent(created[0].id)

                val afterUpdate = varselStatusDAO.getPendingByType(DialogmotekandidatVarselType.VARSEL)
                assertThat(afterUpdate).isEmpty()

                val sentCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM dialogkandidat_varsel_status WHERE status = 'SENT'",
                    Int::class.java
                )
                assertThat(sentCount).isEqualTo(1)
            }

            it("incrementRetryCount øker retry_count") {
                val uuid = UUID.randomUUID().toString()
                val fnr = "12345678901"

                varselStatusDAO.create(uuid, fnr, DialogmotekandidatVarselType.VARSEL)
                val created = varselStatusDAO.getPendingByType(DialogmotekandidatVarselType.VARSEL)
                assertThat(created[0].retryCount).isEqualTo(0)

                varselStatusDAO.incrementRetryCount(created[0].id)
                varselStatusDAO.incrementRetryCount(created[0].id)

                val retryCount = jdbcTemplate.queryForObject(
                    "SELECT retry_count FROM dialogkandidat_varsel_status WHERE kafka_melding_uuid = ?",
                    Int::class.java,
                    uuid
                )
                assertThat(retryCount).isEqualTo(2)
            }

            it("getPendingByType returnerer kun riktig type") {
                varselStatusDAO.create(UUID.randomUUID().toString(), "11111111111", DialogmotekandidatVarselType.VARSEL)
                varselStatusDAO.create(UUID.randomUUID().toString(), "22222222222", DialogmotekandidatVarselType.FERDIGSTILL)

                val varselPending = varselStatusDAO.getPendingByType(DialogmotekandidatVarselType.VARSEL)
                val ferdigstillPending = varselStatusDAO.getPendingByType(DialogmotekandidatVarselType.FERDIGSTILL)

                assertThat(varselPending).hasSize(1)
                assertThat(varselPending[0].type).isEqualTo(DialogmotekandidatVarselType.VARSEL)
                assertThat(ferdigstillPending).hasSize(1)
                assertThat(ferdigstillPending[0].type).isEqualTo(DialogmotekandidatVarselType.FERDIGSTILL)
            }

            it("countPendingOlderThan teller korrekt") {
                val uuid = UUID.randomUUID().toString()
                varselStatusDAO.create(uuid, "12345678901", DialogmotekandidatVarselType.VARSEL)

                // Bakdaterer created_at til 2 dager siden
                jdbcTemplate.update(
                    "UPDATE dialogkandidat_varsel_status SET created_at = NOW() - INTERVAL '2 days' WHERE kafka_melding_uuid = ?",
                    uuid
                )

                val count = varselStatusDAO.countPendingOlderThan(
                    DialogmotekandidatVarselType.VARSEL,
                    LocalDateTime.now().minusMinutes(1)
                )
                assertThat(count).isEqualTo(1)

                // Rad som er ny skal ikke telles
                val countNew = varselStatusDAO.countPendingOlderThan(
                    DialogmotekandidatVarselType.VARSEL,
                    LocalDateTime.now().minusDays(3)
                )
                assertThat(countNew).isEqualTo(0)
            }

            it("deleteSentOlderThan sletter riktige rader") {
                val uuid = UUID.randomUUID().toString()
                varselStatusDAO.create(uuid, "12345678901", DialogmotekandidatVarselType.VARSEL)
                val created = varselStatusDAO.getPendingByType(DialogmotekandidatVarselType.VARSEL)
                varselStatusDAO.updateStatusToSent(created[0].id)

                // Bakdaterer updated_at slik at raden er eldre enn cutoff
                jdbcTemplate.update(
                    "UPDATE dialogkandidat_varsel_status SET updated_at = NOW() - INTERVAL '2 days' WHERE kafka_melding_uuid = ?",
                    uuid
                )

                val deleted = varselStatusDAO.deleteSentOlderThan(LocalDateTime.now().minusMinutes(1))
                assertThat(deleted).isEqualTo(1)

                val remaining = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM dialogkandidat_varsel_status",
                    Int::class.java
                )
                assertThat(remaining).isEqualTo(0)
            }

            it("deletePendingOlderThan sletter riktige rader") {
                val uuid = UUID.randomUUID().toString()
                varselStatusDAO.create(uuid, "12345678901", DialogmotekandidatVarselType.VARSEL)

                // Bakdaterer created_at slik at raden er eldre enn cutoff
                jdbcTemplate.update(
                    "UPDATE dialogkandidat_varsel_status SET created_at = NOW() - INTERVAL '2 days' WHERE kafka_melding_uuid = ?",
                    uuid
                )

                val deleted = varselStatusDAO.deletePendingOlderThan(LocalDateTime.now().minusMinutes(1))
                assertThat(deleted).isEqualTo(1)

                val remaining = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM dialogkandidat_varsel_status",
                    Int::class.java
                )
                assertThat(remaining).isEqualTo(0)
            }
        }
    }
}
