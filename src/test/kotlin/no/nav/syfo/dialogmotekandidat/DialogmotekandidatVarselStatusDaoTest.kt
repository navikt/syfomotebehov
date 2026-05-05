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
import no.nav.syfo.testhelper.UserConstants
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

    private val testFnr = UserConstants.ARBEIDSTAKER_FNR
    private lateinit var uuid: String

    init {
        beforeTest {
            jdbcTemplate.update("DELETE FROM dialogkandidat_varsel_status")
            uuid = UUID.randomUUID().toString()
        }

        it("create stores row with PENDING status, correct type, fnr and zero retryCount") {
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

        it("create with duplicate uuid is idempotent - does not insert a second row") {
            dialogmotekandidatVarselStatusDao.create(uuid, testFnr, DialogmotekandidatVarselType.VARSEL)
            dialogmotekandidatVarselStatusDao.create(uuid, testFnr, DialogmotekandidatVarselType.VARSEL)

            dialogmotekandidatVarselStatusDao.getPendingByType(DialogmotekandidatVarselType.VARSEL).apply {
                shouldHaveSize(1)
            }
        }

        it("updateStatusToSent moves row out of PENDING results and returns true") {
            dialogmotekandidatVarselStatusDao.create(uuid, testFnr, DialogmotekandidatVarselType.VARSEL)
            val created = dialogmotekandidatVarselStatusDao.getPendingByType(DialogmotekandidatVarselType.VARSEL)
            created shouldHaveSize 1

            val result = dialogmotekandidatVarselStatusDao.updateStatusToSent(created.first().id)

            result shouldBe true
            dialogmotekandidatVarselStatusDao.getPendingByType(DialogmotekandidatVarselType.VARSEL).shouldBeEmpty()

            jdbcTemplate.queryForObject<Int>(
                "SELECT COUNT(*) FROM dialogkandidat_varsel_status WHERE status = 'SENT'"
            ) shouldBe 1
        }

        it("updateStatusToSent returns false when row does not exist") {
            val nonExistentId = UUID.randomUUID()
            val result = dialogmotekandidatVarselStatusDao.updateStatusToSent(nonExistentId)
            result shouldBe false
        }

        it("incrementRetryCount increments retryCount by one for each call") {
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

            jdbcTemplate.queryForObject(
                "SELECT next_retry_at > NOW() FROM dialogkandidat_varsel_status WHERE kafka_melding_uuid = ?",
                Boolean::class.java,
                uuid,
            ) shouldBe true

            dialogmotekandidatVarselStatusDao.getPendingByType(DialogmotekandidatVarselType.VARSEL).shouldBeEmpty()
        }

        it("incrementRetryCount applies exponential backoff - later retries have longer delay") {
            dialogmotekandidatVarselStatusDao.create(uuid, testFnr, DialogmotekandidatVarselType.VARSEL)
            val created = dialogmotekandidatVarselStatusDao.getPendingByType(DialogmotekandidatVarselType.VARSEL)

            dialogmotekandidatVarselStatusDao.incrementRetryCount(created.first().id)
            val afterFirstRetry =
                jdbcTemplate.queryForObject(
                    "SELECT next_retry_at FROM dialogkandidat_varsel_status WHERE kafka_melding_uuid = ?",
                    LocalDateTime::class.java,
                    uuid,
                )!!

            dialogmotekandidatVarselStatusDao.incrementRetryCount(created.first().id)
            val afterSecondRetry =
                jdbcTemplate.queryForObject(
                    "SELECT next_retry_at FROM dialogkandidat_varsel_status WHERE kafka_melding_uuid = ?",
                    LocalDateTime::class.java,
                    uuid,
                )!!

            // Andre retry skal ha lengre delay enn første (2^1=2min vs 2^0=1min)
            afterSecondRetry.isAfter(afterFirstRetry) shouldBe true
        }

        it("getPendingByType returns only rows matching the requested type, not other types") {
            dialogmotekandidatVarselStatusDao.create(
                UUID.randomUUID().toString(),
                UserConstants.ARBEIDSTAKER_FNR,
                DialogmotekandidatVarselType.VARSEL,
            )
            dialogmotekandidatVarselStatusDao.create(
                UUID.randomUUID().toString(),
                UserConstants.LEDER_FNR,
                DialogmotekandidatVarselType.FERDIGSTILL,
            )

            dialogmotekandidatVarselStatusDao.getPendingByType(DialogmotekandidatVarselType.VARSEL).apply {
                shouldHaveSize(1)
                with(first()) { type shouldBe DialogmotekandidatVarselType.VARSEL }
            }
            dialogmotekandidatVarselStatusDao.getPendingByType(DialogmotekandidatVarselType.FERDIGSTILL).apply {
                shouldHaveSize(1)
                with(first()) { type shouldBe DialogmotekandidatVarselType.FERDIGSTILL }
            }
        }

        it("hasPendingFerdigstillForFnr returns true only for pending ferdigstill rows on fnr") {
            val varselUuid = UUID.randomUUID().toString()
            val ferdigstillUuid = UUID.randomUUID().toString()
            dialogmotekandidatVarselStatusDao.create(varselUuid, UserConstants.ARBEIDSTAKER_FNR, DialogmotekandidatVarselType.VARSEL)
            dialogmotekandidatVarselStatusDao.create(
                ferdigstillUuid,
                UserConstants.ARBEIDSTAKER_FNR,
                DialogmotekandidatVarselType.FERDIGSTILL,
            )

            dialogmotekandidatVarselStatusDao.hasPendingFerdigstillForFnr(UserConstants.ARBEIDSTAKER_FNR) shouldBe true

            val ferdigstillRow = dialogmotekandidatVarselStatusDao.getPendingByType(DialogmotekandidatVarselType.FERDIGSTILL).single()
            dialogmotekandidatVarselStatusDao.updateStatusToSent(ferdigstillRow.id)

            dialogmotekandidatVarselStatusDao.hasPendingFerdigstillForFnr(UserConstants.ARBEIDSTAKER_FNR) shouldBe false
            dialogmotekandidatVarselStatusDao.hasPendingFerdigstillForFnr(UserConstants.LEDER_FNR) shouldBe false
        }

        it("countPendingOlderThan counts rows created before cutoff and excludes newer ones") {
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

        it("deleteSentOlderThan deletes SENT rows older than cutoff and returns deleted count") {
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

        it("deletePendingOlderThan deletes PENDING rows older than cutoff and returns deleted count") {
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
