package no.nav.syfo.dialogmotekandidat

import io.kotest.core.extensions.ApplyExtension
import io.kotest.extensions.spring.SpringExtension
import no.nav.syfo.IntegrationTest
import no.nav.syfo.LocalApplication
import no.nav.syfo.dialogmotekandidat.database.VarselOutboxDao
import no.nav.syfo.dialogmotekandidat.database.VarselOutboxRecipientDao
import no.nav.syfo.dialogmotekandidat.database.VarselOutboxRecipientStatus
import no.nav.syfo.dialogmotekandidat.kafka.KafkaDialogmotekandidatEndring
import no.nav.syfo.testhelper.UserConstants
import no.nav.syfo.varsel.esyfovarsel.domain.ArbeidstakerHendelse
import no.nav.syfo.varsel.esyfovarsel.domain.HendelseType
import no.nav.syfo.varsel.esyfovarsel.domain.NarmesteLederHendelse
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
internal class VarselOutboxRecipientDaoTest : IntegrationTest() {

    @Autowired
    private lateinit var varselOutboxDao: VarselOutboxDao

    @Autowired
    private lateinit var varselOutboxRecipientDao: VarselOutboxRecipientDao

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    init {
        beforeTest {
            jdbcTemplate.update("DELETE FROM VARSEL_OUTBOX_RECIPIENT")
            jdbcTemplate.update("DELETE FROM VARSEL_OUTBOX")
        }

        describe("VarselOutboxRecipientDao") {
            describe("createRecipient") {
                it("oppretter en mottaker-entry med status PENDING") {
                    val outboxUuid = varselOutboxDao.createPending(lagEndring(kandidat = true))
                    varselOutboxRecipientDao.createRecipient(
                        outboxUuid = outboxUuid,
                        mottakerFnr = UserConstants.ARBEIDSTAKER_FNR,
                        hendelse = lagArbeidstakerHendelse(ferdigstill = false),
                    )

                    val pending = varselOutboxRecipientDao.getPending()
                    assertThat(pending).hasSize(1)
                    assertThat(pending.first().mottakerFnr).isEqualTo(UserConstants.ARBEIDSTAKER_FNR)
                    assertThat(pending.first().status).isEqualTo(VarselOutboxRecipientStatus.PENDING)
                    assertThat(pending.first().outboxUuid).isEqualTo(outboxUuid)
                }

                it("idempotent — same outboxUuid + mottakerFnr skrives ikke dobbelt") {
                    val outboxUuid = varselOutboxDao.createPending(lagEndring(kandidat = true))
                    val hendelse = lagArbeidstakerHendelse(ferdigstill = false)

                    varselOutboxRecipientDao.createRecipient(outboxUuid, UserConstants.ARBEIDSTAKER_FNR, hendelse)
                    varselOutboxRecipientDao.createRecipient(outboxUuid, UserConstants.ARBEIDSTAKER_FNR, hendelse)

                    assertThat(varselOutboxRecipientDao.getPending()).hasSize(1)
                }

                it("nullable outboxUuid lar seg sette inn flere ganger for samme mottaker") {
                    val hendelse = lagArbeidstakerHendelse(ferdigstill = false)

                    varselOutboxRecipientDao.createRecipient(null, UserConstants.ARBEIDSTAKER_FNR, hendelse)
                    varselOutboxRecipientDao.createRecipient(null, UserConstants.ARBEIDSTAKER_FNR, hendelse)

                    assertThat(varselOutboxRecipientDao.getPending()).hasSize(2)
                }

                it("oppretter mottakere for arbeidstaker og nærmeste leder") {
                    val outboxUuid = varselOutboxDao.createPending(lagEndring(kandidat = true))

                    varselOutboxRecipientDao.createRecipient(
                        outboxUuid = outboxUuid,
                        mottakerFnr = UserConstants.ARBEIDSTAKER_FNR,
                        hendelse = lagArbeidstakerHendelse(ferdigstill = false),
                    )
                    varselOutboxRecipientDao.createRecipient(
                        outboxUuid = outboxUuid,
                        mottakerFnr = UserConstants.LEDER_FNR,
                        hendelse = lagNarmesteLederHendelse(ferdigstill = false),
                    )

                    val pending = varselOutboxRecipientDao.getPending()
                    assertThat(pending).hasSize(2)
                    assertThat(pending.map { it.mottakerFnr }).containsExactlyInAnyOrder(
                        UserConstants.ARBEIDSTAKER_FNR,
                        UserConstants.LEDER_FNR,
                    )
                }

                it("deserialiserer ArbeidstakerHendelse korrekt") {
                    val outboxUuid = varselOutboxDao.createPending(lagEndring(kandidat = true))
                    val hendelse = lagArbeidstakerHendelse(ferdigstill = true)

                    varselOutboxRecipientDao.createRecipient(outboxUuid, UserConstants.ARBEIDSTAKER_FNR, hendelse)

                    val entry = varselOutboxRecipientDao.getPending().first()
                    assertThat(entry.hendelse).isInstanceOf(ArbeidstakerHendelse::class.java)
                    val atHendelse = entry.hendelse as ArbeidstakerHendelse
                    assertThat(atHendelse.type).isEqualTo(HendelseType.SM_DIALOGMOTE_SVAR_MOTEBEHOV)
                    assertThat(atHendelse.ferdigstill).isTrue()
                }

                it("deserialiserer NarmesteLederHendelse korrekt") {
                    val outboxUuid = varselOutboxDao.createPending(lagEndring(kandidat = false))
                    val hendelse = lagNarmesteLederHendelse(ferdigstill = true)

                    varselOutboxRecipientDao.createRecipient(outboxUuid, UserConstants.LEDER_FNR, hendelse)

                    val entry = varselOutboxRecipientDao.getPending().first()
                    assertThat(entry.hendelse).isInstanceOf(NarmesteLederHendelse::class.java)
                    val nlHendelse = entry.hendelse as NarmesteLederHendelse
                    assertThat(nlHendelse.type).isEqualTo(HendelseType.NL_DIALOGMOTE_SVAR_MOTEBEHOV)
                    assertThat(nlHendelse.ferdigstill).isTrue()
                    assertThat(nlHendelse.narmesteLederFnr).isEqualTo(UserConstants.LEDER_FNR)
                }
            }

            describe("updateStatus") {
                it("oppdaterer status til SENT") {
                    val outboxUuid = varselOutboxDao.createPending(lagEndring(kandidat = true))
                    val recipientUuid = varselOutboxRecipientDao.createRecipient(
                        outboxUuid = outboxUuid,
                        mottakerFnr = UserConstants.ARBEIDSTAKER_FNR,
                        hendelse = lagArbeidstakerHendelse(ferdigstill = false),
                    )

                    varselOutboxRecipientDao.updateStatus(recipientUuid, VarselOutboxRecipientStatus.SENT)

                    assertThat(varselOutboxRecipientDao.getPending()).isEmpty()
                    val status = jdbcTemplate.queryForObject(
                        "SELECT status FROM VARSEL_OUTBOX_RECIPIENT WHERE uuid = ?",
                        String::class.java,
                        recipientUuid,
                    )
                    assertThat(status).isEqualTo("SENT")
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

    private fun lagArbeidstakerHendelse(ferdigstill: Boolean) = ArbeidstakerHendelse(
        type = HendelseType.SM_DIALOGMOTE_SVAR_MOTEBEHOV,
        ferdigstill = ferdigstill,
        data = null,
        arbeidstakerFnr = UserConstants.ARBEIDSTAKER_FNR,
        orgnummer = null,
    )

    private fun lagNarmesteLederHendelse(ferdigstill: Boolean) = NarmesteLederHendelse(
        type = HendelseType.NL_DIALOGMOTE_SVAR_MOTEBEHOV,
        ferdigstill = ferdigstill,
        data = null,
        narmesteLederFnr = UserConstants.LEDER_FNR,
        arbeidstakerFnr = UserConstants.ARBEIDSTAKER_FNR,
        orgnummer = UserConstants.VIRKSOMHETSNUMMER,
    )
}
