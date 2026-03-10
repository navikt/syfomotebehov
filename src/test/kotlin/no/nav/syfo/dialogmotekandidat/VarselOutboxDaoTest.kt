package no.nav.syfo.dialogmotekandidat

import io.kotest.core.extensions.ApplyExtension
import io.kotest.extensions.spring.SpringExtension
import no.nav.syfo.IntegrationTest
import no.nav.syfo.LocalApplication
import no.nav.syfo.dialogmotekandidat.database.VarselOutboxDao
import no.nav.syfo.dialogmotekandidat.database.VarselOutboxStatus
import no.nav.syfo.dialogmotekandidat.kafka.KafkaDialogmotekandidatEndring
import no.nav.syfo.testhelper.UserConstants
import org.assertj.core.api.Assertions.assertThat
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.annotation.DirtiesContext
import java.time.OffsetDateTime
import java.time.ZoneId
import java.util.UUID

@TestConfiguration
@SpringBootTest(classes = [LocalApplication::class])
@ApplyExtension(SpringExtension::class)
@DirtiesContext
internal class VarselOutboxDaoTest : IntegrationTest() {

    @Autowired
    private lateinit var varselOutboxDao: VarselOutboxDao

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    init {
        beforeTest {
            jdbcTemplate.update("DELETE FROM VARSEL_OUTBOX_RECIPIENT")
            jdbcTemplate.update("DELETE FROM VARSEL_OUTBOX")
        }

        describe("VarselOutboxDao") {
            describe("createPending") {
                it("oppretter en entry med status PENDING") {
                    val endring = lagEndring(kandidat = true)
                    val uuid = varselOutboxDao.createPending(endring)

                    val pending = varselOutboxDao.getPending()
                    assertThat(pending).hasSize(1)
                    assertThat(pending.first().uuid).isEqualTo(uuid)
                    assertThat(pending.first().status).isEqualTo(VarselOutboxStatus.PENDING)
                    assertThat(pending.first().kilde).isEqualTo(VarselOutboxDao.KILDE_DIALOGMOTEKANDIDAT_LISTENER)
                }

                it("ON CONFLICT DO NOTHING — samme UUID skrives ikke dobbelt") {
                    val endring = lagEndring(kandidat = true)
                    varselOutboxDao.createPending(endring)
                    varselOutboxDao.createPending(endring)

                    assertThat(varselOutboxDao.getPending()).hasSize(1)
                }
            }

            describe("createSkipped") {
                it("oppretter en entry med status SKIPPED") {
                    val endring = lagEndring(kandidat = true)
                    varselOutboxDao.createSkipped(endring)

                    assertThat(varselOutboxDao.getPending()).isEmpty()
                    val count = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM VARSEL_OUTBOX WHERE status = 'SKIPPED'",
                        Int::class.java,
                    )
                    assertThat(count).isEqualTo(1)
                }
            }

            describe("getPending") {
                it("returnerer kun PENDING entries, ikke SKIPPED eller PROCESSED") {
                    varselOutboxDao.createPending(lagEndring(kandidat = true))
                    varselOutboxDao.createSkipped(lagEndring(kandidat = true))

                    assertThat(varselOutboxDao.getPending()).hasSize(1)
                }
            }

            describe("updateStatus") {
                it("oppdaterer status til PROCESSED") {
                    val uuid = varselOutboxDao.createPending(lagEndring(kandidat = true))
                    varselOutboxDao.updateStatus(uuid, VarselOutboxStatus.PROCESSED)

                    assertThat(varselOutboxDao.getPending()).isEmpty()
                    val status = jdbcTemplate.queryForObject(
                        "SELECT status FROM VARSEL_OUTBOX WHERE uuid = ?",
                        String::class.java,
                        uuid,
                    )
                    assertThat(status).isEqualTo("PROCESSED")
                }

                it("oppdaterer status til SKIPPED") {
                    val uuid = varselOutboxDao.createPending(lagEndring(kandidat = false))
                    varselOutboxDao.updateStatus(uuid, VarselOutboxStatus.SKIPPED)

                    assertThat(varselOutboxDao.getPending()).isEmpty()
                }
            }
        }
    }

    private fun lagEndring(kandidat: Boolean): KafkaDialogmotekandidatEndring =
        KafkaDialogmotekandidatEndring(
            uuid = UUID.randomUUID().toString(),
            createdAt = OffsetDateTime.now(ZoneId.of("Europe/Oslo")),
            personIdentNumber = UserConstants.ARBEIDSTAKER_FNR,
            kandidat = kandidat,
            arsak = "STOPPUNKT",
        )
}
