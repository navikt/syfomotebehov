package no.nav.syfo.dialogmotekandidat

import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.extensions.ApplyExtension
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.shouldBe
import io.micrometer.core.instrument.MeterRegistry
import io.mockk.every
import io.mockk.verify
import no.nav.syfo.IntegrationTest
import no.nav.syfo.LocalApplication
import no.nav.syfo.dialogmotekandidat.database.DialogmotekandidatVarselStatusDao
import no.nav.syfo.dialogmotekandidat.database.DialogmotekandidatVarselType
import no.nav.syfo.leaderelection.LeaderElectionClient
import no.nav.syfo.testhelper.UserConstants
import no.nav.syfo.varsel.VarselServiceV2
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.PlatformTransactionManager
import java.util.UUID

@TestConfiguration
@ActiveProfiles("local")
@SpringBootTest(classes = [LocalApplication::class])
@ApplyExtension(SpringExtension::class)
internal class DialogmotekandidatVarselSchedulerTest : IntegrationTest() {
    @Autowired
    private lateinit var dialogmotekandidatVarselStatusDao: DialogmotekandidatVarselStatusDao

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    private lateinit var meterRegistry: MeterRegistry

    @Autowired
    private lateinit var transactionManager: PlatformTransactionManager

    @MockkBean(relaxed = true)
    private lateinit var varselServiceV2: VarselServiceV2

    @MockkBean
    private lateinit var leaderElectionClient: LeaderElectionClient

    private val scheduler by lazy {
        DialogmotekandidatVarselScheduler(
            leaderElectionClient = leaderElectionClient,
            varselStatusDao = dialogmotekandidatVarselStatusDao,
            varselServiceV2 = varselServiceV2,
            transactionManager = transactionManager,
            meterRegistry = meterRegistry,
        )
    }

    init {
        beforeTest {
            jdbcTemplate.update("DELETE FROM dialogkandidat_varsel_status")
            every { leaderElectionClient.isLeader() } returns true
        }

        describe("DialogmotekandidatVarselScheduler") {
            it("sender PENDING varsler og setter rad til SENT") {
                val uuid = createPendingRow(DialogmotekandidatVarselType.VARSEL)

                scheduler.sendPendingVarsler()

                verify(exactly = 1) {
                    varselServiceV2.sendSvarBehovVarsel(UserConstants.ARBEIDSTAKER_FNR, uuid)
                }
                statusFor(uuid) shouldBe "SENT"
            }

            it("sender PENDING ferdigstillinger og setter rad til SENT") {
                val uuid = createPendingRow(DialogmotekandidatVarselType.FERDIGSTILL)

                scheduler.ferdigstillPendingVarsler()

                verify(exactly = 1) {
                    varselServiceV2.ferdigstillSvarMotebehovVarsel(UserConstants.ARBEIDSTAKER_FNR)
                }
                statusFor(uuid) shouldBe "SENT"
            }

            it("oker retry_count og lar rad forbli PENDING ved feil") {
                val uuid = createPendingRow(DialogmotekandidatVarselType.VARSEL)
                every {
                    varselServiceV2.sendSvarBehovVarsel(any(), any())
                } throws RuntimeException("esyfovarsel utilgjengelig")

                scheduler.sendPendingVarsler()

                statusFor(uuid) shouldBe "PENDING"
                retryCountFor(uuid) shouldBe 1
            }

            it("stopper videre behandling etter tre pafolgende feil") {
                val uuids = List(4) { createPendingRow(DialogmotekandidatVarselType.VARSEL) }
                every {
                    varselServiceV2.sendSvarBehovVarsel(any(), any())
                } throws RuntimeException("esyfovarsel utilgjengelig")

                scheduler.sendPendingVarsler()

                verify(exactly = 3) { varselServiceV2.sendSvarBehovVarsel(any(), any()) }
                uuids.take(3).forEach { uuid ->
                    statusFor(uuid) shouldBe "PENDING"
                    retryCountFor(uuid) shouldBe 1
                }
                statusFor(uuids.last()) shouldBe "PENDING"
                retryCountFor(uuids.last()) shouldBe 0
            }

            it("resetter circuit breaker etter suksess") {
                val uuids = List(6) { createPendingRow(DialogmotekandidatVarselType.VARSEL) }
                every {
                    varselServiceV2.sendSvarBehovVarsel(any(), uuids[0])
                } throws RuntimeException("feil 1")
                every {
                    varselServiceV2.sendSvarBehovVarsel(any(), uuids[1])
                } throws RuntimeException("feil 2")
                every {
                    varselServiceV2.sendSvarBehovVarsel(any(), uuids[2])
                } returns Unit
                every {
                    varselServiceV2.sendSvarBehovVarsel(any(), uuids[3])
                } throws RuntimeException("feil 3")
                every {
                    varselServiceV2.sendSvarBehovVarsel(any(), uuids[4])
                } throws RuntimeException("feil 4")
                every {
                    varselServiceV2.sendSvarBehovVarsel(any(), uuids[5])
                } throws RuntimeException("feil 5")

                scheduler.sendPendingVarsler()

                verify(exactly = 6) { varselServiceV2.sendSvarBehovVarsel(any(), any()) }
                retryCountFor(uuids[0]) shouldBe 1
                retryCountFor(uuids[1]) shouldBe 1
                statusFor(uuids[2]) shouldBe "SENT"
                retryCountFor(uuids[3]) shouldBe 1
                retryCountFor(uuids[4]) shouldBe 1
                retryCountFor(uuids[5]) shouldBe 1
            }

            it("respekterer backoff og plukker ikke opp rad som nylig er retried") {
                val uuid = createPendingRow(DialogmotekandidatVarselType.VARSEL)
                val row = dialogmotekandidatVarselStatusDao.getPendingByType(DialogmotekandidatVarselType.VARSEL).single()
                dialogmotekandidatVarselStatusDao.incrementRetryCount(row.id)

                scheduler.sendPendingVarsler()

                verify(exactly = 0) { varselServiceV2.sendSvarBehovVarsel(any(), any()) }
                statusFor(uuid) shouldBe "PENDING"
            }

            it("sender ikke varsel nar pending ferdigstill finnes for samme fnr") {
                val varselUuid = createPendingRow(DialogmotekandidatVarselType.VARSEL)
                val ferdigstillUuid = createPendingRow(DialogmotekandidatVarselType.FERDIGSTILL)

                scheduler.sendPendingVarsler()

                verify(exactly = 0) { varselServiceV2.sendSvarBehovVarsel(any(), any()) }
                statusFor(varselUuid) shouldBe "SENT"
                statusFor(ferdigstillUuid) shouldBe "PENDING"
            }

            it("ignorerer rader som har overskredet maks antall retries") {
                val uuid = createPendingRow(DialogmotekandidatVarselType.VARSEL)
                jdbcTemplate.update(
                    "UPDATE dialogkandidat_varsel_status SET retry_count = ?, next_retry_at = NOW() - INTERVAL '1 hour' WHERE kafka_melding_uuid = ?",
                    DialogmotekandidatVarselStatusDao.MAX_RETRY_COUNT,
                    uuid,
                )

                scheduler.sendPendingVarsler()

                verify(exactly = 0) { varselServiceV2.sendSvarBehovVarsel(any(), any()) }
                statusFor(uuid) shouldBe "PENDING"
            }

            it("gjor ingenting nar podden ikke er leader") {
                createPendingRow(DialogmotekandidatVarselType.VARSEL)
                every { leaderElectionClient.isLeader() } returns false

                scheduler.run()

                verify(exactly = 0) { varselServiceV2.sendSvarBehovVarsel(any(), any()) }
                verify(exactly = 0) { varselServiceV2.ferdigstillSvarMotebehovVarsel(any()) }
            }

            it("cleanup sletter gamle SENT-rader") {
                val uuid = createPendingRow(DialogmotekandidatVarselType.VARSEL)
                val row = dialogmotekandidatVarselStatusDao.getPendingByType(DialogmotekandidatVarselType.VARSEL).single()
                dialogmotekandidatVarselStatusDao.updateStatusToSent(row.id)
                jdbcTemplate.update(
                    "UPDATE dialogkandidat_varsel_status SET updated_at = NOW() - INTERVAL '2 months' WHERE kafka_melding_uuid = ?",
                    uuid,
                )

                scheduler.runCleanUp()

                countRowsFor(uuid) shouldBe 0
            }

            it("cleanup sletter gamle PENDING-rader") {
                val uuid = createPendingRow(DialogmotekandidatVarselType.VARSEL)
                jdbcTemplate.update(
                    "UPDATE dialogkandidat_varsel_status SET created_at = NOW() - INTERVAL '3 weeks' WHERE kafka_melding_uuid = ?",
                    uuid,
                )

                scheduler.runCleanUp()

                countRowsFor(uuid) shouldBe 0
            }
        }
    }

    private fun createPendingRow(type: DialogmotekandidatVarselType): String {
        val uuid = UUID.randomUUID().toString()
        dialogmotekandidatVarselStatusDao.create(uuid, UserConstants.ARBEIDSTAKER_FNR, type)
        return uuid
    }

    private fun statusFor(kafkaMeldingUuid: String): String =
        jdbcTemplate.queryForObject(
            "SELECT status FROM dialogkandidat_varsel_status WHERE kafka_melding_uuid = ?",
            String::class.java,
            kafkaMeldingUuid,
        )!!

    private fun retryCountFor(kafkaMeldingUuid: String): Int =
        jdbcTemplate.queryForObject(
            "SELECT retry_count FROM dialogkandidat_varsel_status WHERE kafka_melding_uuid = ?",
            Int::class.java,
            kafkaMeldingUuid,
        )!!

    private fun countRowsFor(kafkaMeldingUuid: String): Int =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM dialogkandidat_varsel_status WHERE kafka_melding_uuid = ?",
            Int::class.java,
            kafkaMeldingUuid,
        )!!
}
