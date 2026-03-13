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
import org.springframework.jdbc.core.queryForObject
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

    private val testFnr = "12345678901"
    private lateinit var uuid: String

    init {
        beforeTest {
            jdbcTemplate.update("DELETE FROM dialogkandidat_varsel_status")
            uuid = UUID.randomUUID().toString()
        }

        it("create oppretter en PENDING-rad med riktig type") {
            dialogmotekandidatVarselStatusDao.create(uuid, testFnr, DialogmotekandidatVarselType.VARSEL)

            dialogmotekandidatVarselStatusDao.getPendingByType(DialogmotekandidatVarselType.VARSEL).apply {
                shouldHaveSize(1)
                with(first()) {
                    kafkaMeldingUuid shouldBe uuid
                    this.fnr shouldBe testFnr
                    type shouldBe DialogmotekandidatVarselType.VARSEL
                    status shouldBe DialogmotekandidatVarselStatusDao.STATUS_PENDING
                    retryCount shouldBe 0
                }
            }
        }

        it("create med duplikat uuid gjør ingenting") {
            dialogmotekandidatVarselStatusDao.create(uuid, testFnr, DialogmotekandidatVarselType.VARSEL)
            dialogmotekandidatVarselStatusDao.create(uuid, testFnr, DialogmotekandidatVarselType.VARSEL)

            dialogmotekandidatVarselStatusDao.getPendingByType(DialogmotekandidatVarselType.VARSEL).apply {
                shouldHaveSize(1)
            }
        }

        it("updateStatusToSent setter status til SENT") {
            dialogmotekandidatVarselStatusDao.create(uuid, testFnr, DialogmotekandidatVarselType.VARSEL)
            val created = dialogmotekandidatVarselStatusDao.getPendingByType(DialogmotekandidatVarselType.VARSEL)
            created shouldHaveSize 1

            dialogmotekandidatVarselStatusDao.updateStatusToSent(created.first().id)

            dialogmotekandidatVarselStatusDao.getPendingByType(DialogmotekandidatVarselType.VARSEL).shouldBeEmpty()

            jdbcTemplate.queryForObject<Int>(
                "SELECT COUNT(*) FROM dialogkandidat_varsel_status WHERE status = 'SENT'"
            ) shouldBe 1
        }

        it("incrementRetryCount øker retry_count") {
            dialogmotekandidatVarselStatusDao.create(uuid, testFnr, DialogmotekandidatVarselType.VARSEL)
            val created = dialogmotekandidatVarselStatusDao.getPendingByType(DialogmotekandidatVarselType.VARSEL)
            created.first().retryCount shouldBe 0

            dialogmotekandidatVarselStatusDao.incrementRetryCount(created.first().id)
            dialogmotekandidatVarselStatusDao.incrementRetryCount(created.first().id)

            jdbcTemplate.queryForObject(
                "SELECT retry_count FROM dialogkandidat_varsel_status WHERE kafka_melding_uuid = ?",
                Int::class.java,
                uuid
            ) shouldBe 2
        }

        it("getPendingByType returnerer kun riktig type") {
            dialogmotekandidatVarselStatusDao.create(UUID.randomUUID().toString(), "11111111111", DialogmotekandidatVarselType.VARSEL)
            dialogmotekandidatVarselStatusDao.create(UUID.randomUUID().toString(), "22222222222", DialogmotekandidatVarselType.FERDIGSTILL)

            dialogmotekandidatVarselStatusDao.getPendingByType(DialogmotekandidatVarselType.VARSEL).apply {
                shouldHaveSize(1)
                with(first()) { type shouldBe DialogmotekandidatVarselType.VARSEL }
            }
            dialogmotekandidatVarselStatusDao.getPendingByType(DialogmotekandidatVarselType.FERDIGSTILL).apply {
                shouldHaveSize(1)
                with(first()) { type shouldBe DialogmotekandidatVarselType.FERDIGSTILL }
            }
        }

        it("countPendingOlderThan teller korrekt") {
            dialogmotekandidatVarselStatusDao.create(uuid, testFnr, DialogmotekandidatVarselType.VARSEL)

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
            dialogmotekandidatVarselStatusDao.create(uuid, testFnr, DialogmotekandidatVarselType.VARSEL)
            val created = dialogmotekandidatVarselStatusDao.getPendingByType(DialogmotekandidatVarselType.VARSEL)
            dialogmotekandidatVarselStatusDao.updateStatusToSent(created.first().id)

            // Bakdaterer updated_at slik at raden er eldre enn cutoff
            jdbcTemplate.update(
                "UPDATE dialogkandidat_varsel_status SET updated_at = NOW() - INTERVAL '2 days' WHERE kafka_melding_uuid = ?",
                uuid
            )

            val deleted = dialogmotekandidatVarselStatusDao.deleteSentOlderThan(LocalDateTime.now().minusMinutes(1))
            deleted shouldBe 1

            jdbcTemplate.queryForObject<Int>(
                "SELECT COUNT(*) FROM dialogkandidat_varsel_status"
            ) shouldBe 0
        }

        it("deletePendingOlderThan sletter riktige rader") {
            dialogmotekandidatVarselStatusDao.create(uuid, testFnr, DialogmotekandidatVarselType.VARSEL)

            // Bakdaterer created_at slik at raden er eldre enn cutoff
            jdbcTemplate.update(
                "UPDATE dialogkandidat_varsel_status SET created_at = NOW() - INTERVAL '2 days' WHERE kafka_melding_uuid = ?",
                uuid
            )

            val deleted = dialogmotekandidatVarselStatusDao.deletePendingOlderThan(LocalDateTime.now().minusMinutes(1))
            deleted shouldBe 1

            jdbcTemplate.queryForObject<Int>(
                "SELECT COUNT(*) FROM dialogkandidat_varsel_status"
            ) shouldBe 0
        }
    }
}
