package no.nav.syfo.dialogmotekandidat

import io.kotest.core.extensions.ApplyExtension
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import no.nav.syfo.IntegrationTest
import no.nav.syfo.LocalApplication
import no.nav.syfo.dialogmotekandidat.database.DialogmotekandidatVarselStatusDao
import no.nav.syfo.dialogmotekandidat.database.DialogmotekandidatVarselType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.jdbc.core.JdbcTemplate
import java.time.LocalDateTime
import java.util.UUID

@TestConfiguration
@SpringBootTest(classes = [LocalApplication::class])
@ApplyExtension(SpringExtension::class)
class DialogmotekandidatVarselStatusDaoTest : IntegrationTest() {

    @Autowired
    private lateinit var dialogmotekandidatVarselStatusDao: DialogmotekandidatVarselStatusDao

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    init {
        beforeTest {
            jdbcTemplate.update("DELETE FROM dialogkandidat_varsel_status")
        }

        describe("DialogmotekandidatVarselStatusDao") {

            it("create oppretter en PENDING-rad med riktig type") {
                val uuid = UUID.randomUUID().toString()
                val fnr = "12345678901"

                dialogmotekandidatVarselStatusDao.create(uuid, fnr, DialogmotekandidatVarselType.VARSEL)

                val pending = dialogmotekandidatVarselStatusDao.getPendingByType(DialogmotekandidatVarselType.VARSEL)
                pending shouldHaveSize 1
                with(pending[0]) {
                    kafkaMeldingUuid shouldBe uuid
                    this.fnr shouldBe fnr
                    type shouldBe DialogmotekandidatVarselType.VARSEL
                    status shouldBe DialogmotekandidatVarselStatusDao.STATUS_PENDING
                    retryCount shouldBe 0
                }
            }

            it("create med duplikat uuid gjør ingenting") {
                val uuid = UUID.randomUUID().toString()
                val fnr = "12345678901"

                dialogmotekandidatVarselStatusDao.create(uuid, fnr, DialogmotekandidatVarselType.VARSEL)
                dialogmotekandidatVarselStatusDao.create(uuid, fnr, DialogmotekandidatVarselType.VARSEL)

                val pending = dialogmotekandidatVarselStatusDao.getPendingByType(DialogmotekandidatVarselType.VARSEL)
                pending shouldHaveSize 1
            }

            it("updateStatusToSent setter status til SENT") {
                val uuid = UUID.randomUUID().toString()
                val fnr = "12345678901"

                dialogmotekandidatVarselStatusDao.create(uuid, fnr, DialogmotekandidatVarselType.VARSEL)
                val created = dialogmotekandidatVarselStatusDao.getPendingByType(DialogmotekandidatVarselType.VARSEL)
                created shouldHaveSize 1

                dialogmotekandidatVarselStatusDao.updateStatusToSent(created[0].id)

                val afterUpdate = dialogmotekandidatVarselStatusDao.getPendingByType(DialogmotekandidatVarselType.VARSEL)
                afterUpdate.shouldBeEmpty()

                val sentCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM dialogkandidat_varsel_status WHERE status = 'SENT'",
                    Int::class.java
                )
                sentCount shouldBe 1
            }

            it("incrementRetryCount øker retry_count") {
                val uuid = UUID.randomUUID().toString()
                val fnr = "12345678901"

                dialogmotekandidatVarselStatusDao.create(uuid, fnr, DialogmotekandidatVarselType.VARSEL)
                val created = dialogmotekandidatVarselStatusDao.getPendingByType(DialogmotekandidatVarselType.VARSEL)
                created[0].retryCount shouldBe 0

                dialogmotekandidatVarselStatusDao.incrementRetryCount(created[0].id)
                dialogmotekandidatVarselStatusDao.incrementRetryCount(created[0].id)

                val retryCount = jdbcTemplate.queryForObject(
                    "SELECT retry_count FROM dialogkandidat_varsel_status WHERE kafka_melding_uuid = ?",
                    Int::class.java,
                    uuid
                )
                retryCount shouldBe 2
            }

            it("getPendingByType returnerer kun riktig type") {
                dialogmotekandidatVarselStatusDao.create(UUID.randomUUID().toString(), "11111111111", DialogmotekandidatVarselType.VARSEL)
                dialogmotekandidatVarselStatusDao.create(UUID.randomUUID().toString(), "22222222222", DialogmotekandidatVarselType.FERDIGSTILL)

                val varselPending = dialogmotekandidatVarselStatusDao.getPendingByType(DialogmotekandidatVarselType.VARSEL)
                val ferdigstillPending = dialogmotekandidatVarselStatusDao.getPendingByType(DialogmotekandidatVarselType.FERDIGSTILL)

                varselPending shouldHaveSize 1
                varselPending[0].type shouldBe DialogmotekandidatVarselType.VARSEL
                ferdigstillPending shouldHaveSize 1
                ferdigstillPending[0].type shouldBe DialogmotekandidatVarselType.FERDIGSTILL
            }

            it("countPendingOlderThan teller korrekt") {
                val uuid = UUID.randomUUID().toString()
                dialogmotekandidatVarselStatusDao.create(uuid, "12345678901", DialogmotekandidatVarselType.VARSEL)

                // Bakdaterer created_at til 2 dager siden
                jdbcTemplate.update(
                    "UPDATE dialogkandidat_varsel_status SET created_at = NOW() - INTERVAL '2 days' WHERE kafka_melding_uuid = ?",
                    uuid
                )

                val count = dialogmotekandidatVarselStatusDao.countPendingOlderThan(
                    DialogmotekandidatVarselType.VARSEL,
                    LocalDateTime.now().minusMinutes(1)
                )
                count shouldBe 1

                // Rad som er ny skal ikke telles
                val countNew = dialogmotekandidatVarselStatusDao.countPendingOlderThan(
                    DialogmotekandidatVarselType.VARSEL,
                    LocalDateTime.now().minusDays(3)
                )
                countNew shouldBe 0
            }

            it("deleteSentOlderThan sletter riktige rader") {
                val uuid = UUID.randomUUID().toString()
                dialogmotekandidatVarselStatusDao.create(uuid, "12345678901", DialogmotekandidatVarselType.VARSEL)
                val created = dialogmotekandidatVarselStatusDao.getPendingByType(DialogmotekandidatVarselType.VARSEL)
                dialogmotekandidatVarselStatusDao.updateStatusToSent(created[0].id)

                // Bakdaterer updated_at slik at raden er eldre enn cutoff
                jdbcTemplate.update(
                    "UPDATE dialogkandidat_varsel_status SET updated_at = NOW() - INTERVAL '2 days' WHERE kafka_melding_uuid = ?",
                    uuid
                )

                val deleted = dialogmotekandidatVarselStatusDao.deleteSentOlderThan(LocalDateTime.now().minusMinutes(1))
                deleted shouldBe 1

                val remaining = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM dialogkandidat_varsel_status",
                    Int::class.java
                )
                remaining shouldBe 0
            }

            it("deletePendingOlderThan sletter riktige rader") {
                val uuid = UUID.randomUUID().toString()
                dialogmotekandidatVarselStatusDao.create(uuid, "12345678901", DialogmotekandidatVarselType.VARSEL)

                // Bakdaterer created_at slik at raden er eldre enn cutoff
                jdbcTemplate.update(
                    "UPDATE dialogkandidat_varsel_status SET created_at = NOW() - INTERVAL '2 days' WHERE kafka_melding_uuid = ?",
                    uuid
                )

                val deleted = dialogmotekandidatVarselStatusDao.deletePendingOlderThan(LocalDateTime.now().minusMinutes(1))
                deleted shouldBe 1

                val remaining = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM dialogkandidat_varsel_status",
                    Int::class.java
                )
                remaining shouldBe 0
            }
        }
    }
}
