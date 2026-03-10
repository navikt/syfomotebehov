package no.nav.syfo.dialogmotekandidat

import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.extensions.ApplyExtension
import io.kotest.extensions.spring.SpringExtension
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.syfo.IntegrationTest
import no.nav.syfo.LocalApplication
import no.nav.syfo.consumer.narmesteleder.NarmesteLederRelasjonDTO
import no.nav.syfo.consumer.narmesteleder.NarmesteLederRelasjonStatus
import no.nav.syfo.consumer.narmesteleder.NarmesteLederService
import no.nav.syfo.dialogmote.database.DialogmoteDAO
import no.nav.syfo.dialogmotekandidat.database.VarselOutboxDao
import no.nav.syfo.dialogmotekandidat.database.VarselOutboxRecipientDao
import no.nav.syfo.dialogmotekandidat.database.VarselOutboxRecipientStatus
import no.nav.syfo.dialogmotekandidat.database.VarselOutboxStatus
import no.nav.syfo.dialogmotekandidat.kafka.KafkaDialogmotekandidatEndring
import no.nav.syfo.dialogmotekandidat.scheduler.VarselOutboxScheduler
import no.nav.syfo.leaderelection.LeaderElectionClient
import no.nav.syfo.motebehov.MotebehovService
import no.nav.syfo.motebehov.motebehovstatus.MotebehovStatusHelper
import no.nav.syfo.oppfolgingstilfelle.OppfolgingstilfelleService
import no.nav.syfo.testhelper.UserConstants
import no.nav.syfo.varsel.esyfovarsel.EsyfovarselProducer
import no.nav.syfo.varsel.esyfovarsel.domain.ArbeidstakerHendelse
import no.nav.syfo.varsel.esyfovarsel.domain.EsyfovarselHendelse
import no.nav.syfo.varsel.esyfovarsel.domain.HendelseType
import no.nav.syfo.varsel.esyfovarsel.domain.NarmesteLederHendelse
import org.assertj.core.api.Assertions.assertThat
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.annotation.DirtiesContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.util.UUID

@TestConfiguration
@SpringBootTest(classes = [LocalApplication::class])
@ApplyExtension(SpringExtension::class)
@DirtiesContext
internal class VarselOutboxSchedulerTest : IntegrationTest() {

    @Autowired
    private lateinit var scheduler: VarselOutboxScheduler

    @Autowired
    private lateinit var varselOutboxDao: VarselOutboxDao

    @Autowired
    private lateinit var varselOutboxRecipientDao: VarselOutboxRecipientDao

    @Autowired
    private lateinit var dialogmotekandidatService: DialogmotekandidatService

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @MockkBean
    private lateinit var leaderElectionClient: LeaderElectionClient

    @MockkBean
    private lateinit var narmesteLederService: NarmesteLederService

    @MockkBean
    private lateinit var oppfolgingstilfelleService: OppfolgingstilfelleService

    @MockkBean
    private lateinit var dialogmoteDAO: DialogmoteDAO

    @MockkBean
    private lateinit var motebehovService: MotebehovService

    @MockkBean
    private lateinit var motebehovStatusHelper: MotebehovStatusHelper

    @MockkBean
    private lateinit var esyfovarselProducer: EsyfovarselProducer

    init {
        beforeTest {
            jdbcTemplate.update("DELETE FROM VARSEL_OUTBOX_RECIPIENT")
            jdbcTemplate.update("DELETE FROM VARSEL_OUTBOX")
            jdbcTemplate.update("DELETE FROM DIALOGMOTEKANDIDAT")

            // isLeader=false prevents the background @Scheduled thread from consuming entries during test setup
            every { leaderElectionClient.isLeader() } returns false
            every { narmesteLederService.getAllNarmesteLederRelations(any()) } returns emptyList()
            every { oppfolgingstilfelleService.getActiveOppfolgingstilfelleForArbeidstaker(any()) } returns null
            every { oppfolgingstilfelleService.getActiveOppfolgingstilfelleForArbeidsgiver(any(), any()) } returns null
            every { dialogmoteDAO.getAktiveDialogmoterEtterDato(any(), any()) } returns emptyList()
            every { motebehovService.hentMotebehovListeForOgOpprettetAvArbeidstaker(any()) } returns emptyList()
            every { motebehovService.hentMotebehovListeForArbeidstakerOpprettetAvLeder(any(), any(), any()) } returns emptyList()
            every { motebehovStatusHelper.isSvarBehovVarselAvailable(any(), any()) } returns true
            every { esyfovarselProducer.sendVarselTilEsyfovarsel(any()) } returns Unit
        }

        describe("VarselOutboxScheduler") {
            describe("isLeader-guard") {
                it("gjør ingenting dersom ikke leader") {
                    every { leaderElectionClient.isLeader() } returns false
                    varselOutboxDao.createPending(lagEndring(kandidat = true))

                    scheduler.run()

                    assertThat(varselOutboxDao.getPending()).hasSize(1)
                }
            }

            describe("Fase 1 — ekspansjon av PENDING outbox") {
                it("oppretter recipient for arbeidstaker når varsel er tilgjengelig") {
                    kandidatIDb()
                    varselOutboxDao.createPending(lagEndring(kandidat = true))

                    runScheduler()

                    val hendelse = slot<EsyfovarselHendelse>()
                    verify(exactly = 1) { esyfovarselProducer.sendVarselTilEsyfovarsel(capture(hendelse)) }
                    val atHendelse = hendelse.captured as ArbeidstakerHendelse
                    assertThat(atHendelse.arbeidstakerFnr).isEqualTo(UserConstants.ARBEIDSTAKER_FNR)
                    assertThat(atHendelse.type).isEqualTo(HendelseType.SM_DIALOGMOTE_SVAR_MOTEBEHOV)
                    assertThat(atHendelse.ferdigstill).isFalse()
                }

                it("oppretter recipients for arbeidstaker og nærmeste leder") {
                    kandidatIDb()
                    every { narmesteLederService.getAllNarmesteLederRelations(any()) } returns listOf(
                        lagNarmesteLederRelasjon(),
                    )
                    varselOutboxDao.createPending(lagEndring(kandidat = true))

                    runScheduler()

                    verify(exactly = 2) { esyfovarselProducer.sendVarselTilEsyfovarsel(any()) }
                    val sentFnrs = jdbcTemplate.queryForList(
                        "SELECT mottaker_fnr FROM VARSEL_OUTBOX_RECIPIENT WHERE status = 'SENT'",
                        String::class.java,
                    )
                    assertThat(sentFnrs).containsExactlyInAnyOrder(
                        UserConstants.ARBEIDSTAKER_FNR,
                        UserConstants.LEDER_FNR,
                    )
                }

                it("oppretter ferdigstill-recipients for alle mottakere ved kandidat=false") {
                    every { narmesteLederService.getAllNarmesteLederRelations(any()) } returns listOf(lagNarmesteLederRelasjon())
                    varselOutboxDao.createPending(lagEndring(kandidat = false))

                    runScheduler()

                    verify(exactly = 2) { esyfovarselProducer.sendVarselTilEsyfovarsel(any()) }
                    val sentCount = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM VARSEL_OUTBOX_RECIPIENT WHERE status = 'SENT'",
                        Int::class.java,
                    )
                    assertThat(sentCount).isEqualTo(2)
                    val ferdigstillCount = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM VARSEL_OUTBOX_RECIPIENT WHERE payload->>'ferdigstill' = 'true'",
                        Int::class.java,
                    )
                    assertThat(ferdigstillCount).isEqualTo(2)
                }

                it("oppretter ingen recipients dersom dialogmøte allerede er planlagt") {
                    kandidatIDb()
                    every { dialogmoteDAO.getAktiveDialogmoterEtterDato(any(), any()) } returns listOf(mockk())

                    varselOutboxDao.createPending(lagEndring(kandidat = true))

                    runScheduler()

                    assertThat(varselOutboxRecipientDao.getPending()).isEmpty()
                }

                it("oppretter ingen recipient for arbeidstaker når varsel ikke er tilgjengelig") {
                    kandidatIDb()
                    every { motebehovStatusHelper.isSvarBehovVarselAvailable(any(), any()) } returns false
                    varselOutboxDao.createPending(lagEndring(kandidat = true))

                    runScheduler()

                    assertThat(varselOutboxRecipientDao.getPending()).isEmpty()
                }

                it("setter outbox-entry til PROCESSED etter ekspansjon") {
                    kandidatIDb()
                    val uuid = varselOutboxDao.createPending(lagEndring(kandidat = true))

                    runScheduler()

                    assertThat(varselOutboxDao.getPending()).isEmpty()
                    val status = jdbcTemplate.queryForObject(
                        "SELECT status FROM VARSEL_OUTBOX WHERE uuid = ?",
                        String::class.java,
                        uuid,
                    )
                    assertThat(status).isEqualTo("PROCESSED")
                }

                it("idempotent — retry av ekspansjon oppretter ikke doble mottakere") {
                    kandidatIDb()
                    val endring = lagEndring(kandidat = true)
                    varselOutboxDao.createPending(endring)

                    runScheduler()
                    // Sett tilbake til PENDING for å simulere at fase 1 kjøres på nytt
                    jdbcTemplate.update("UPDATE VARSEL_OUTBOX SET status = 'PENDING' WHERE uuid = ?", UUID.fromString(endring.uuid))

                    runScheduler()

                    // ON CONFLICT DO NOTHING: recipient opprettes ikke to ganger
                    val totalCount = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM VARSEL_OUTBOX_RECIPIENT",
                        Int::class.java,
                    )
                    assertThat(totalCount).isEqualTo(1)
                }
            }

            describe("Fase 1 — staleness-sjekker") {
                it("setter SKIPPED dersom outbox er eldre enn 7 dager") {
                    val gammelEndring = lagEndring(kandidat = true, createdAt = OffsetDateTime.now().minusDays(8))
                    varselOutboxDao.createPending(gammelEndring)
                    // Oppdater created_at direkte i DB for å simulere gammel entry
                    jdbcTemplate.update(
                        "UPDATE VARSEL_OUTBOX SET created_at = ? WHERE uuid = ?",
                        LocalDateTime.now().minusDays(8),
                        UUID.fromString(gammelEndring.uuid),
                    )

                    runScheduler()

                    assertThat(varselOutboxDao.getPending()).isEmpty()
                    assertThat(varselOutboxRecipientDao.getPending()).isEmpty()
                    val status = jdbcTemplate.queryForObject(
                        "SELECT status FROM VARSEL_OUTBOX WHERE uuid = ?",
                        String::class.java,
                        UUID.fromString(gammelEndring.uuid),
                    )
                    assertThat(status).isEqualTo("SKIPPED")
                }

                it("setter SKIPPED for kandidat=true-entry dersom person ikke lenger er kandidat") {
                    // Person er IKKE kandidat i DB (ingen DIALOGMOTEKANDIDAT-rad)
                    varselOutboxDao.createPending(lagEndring(kandidat = true))

                    runScheduler()

                    assertThat(varselOutboxRecipientDao.getPending()).isEmpty()
                    val count = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM VARSEL_OUTBOX WHERE status = 'SKIPPED'",
                        Int::class.java,
                    )
                    assertThat(count).isEqualTo(1)
                }

                it("setter SKIPPED for kandidat=false-entry dersom person er kandidat igjen") {
                    kandidatIDb()
                    varselOutboxDao.createPending(lagEndring(kandidat = false))

                    runScheduler()

                    assertThat(varselOutboxRecipientDao.getPending()).isEmpty()
                    val count = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM VARSEL_OUTBOX WHERE status = 'SKIPPED'",
                        Int::class.java,
                    )
                    assertThat(count).isEqualTo(1)
                }
            }

            describe("Fase 2 — utsending av PENDING recipients") {
                it("sender varsel og setter status til SENT") {
                    kandidatIDb()
                    varselOutboxDao.createPending(lagEndring(kandidat = true))
                    runScheduler()

                    val sentCount = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM VARSEL_OUTBOX_RECIPIENT WHERE status = 'SENT'",
                        Int::class.java,
                    )
                    assertThat(sentCount).isEqualTo(1)
                    assertThat(varselOutboxRecipientDao.getPending()).isEmpty()
                    verify(exactly = 1) { esyfovarselProducer.sendVarselTilEsyfovarsel(any()) }
                }

                it("beholder PENDING og retryer ved feil i Kafka-sending") {
                    kandidatIDb()
                    every { esyfovarselProducer.sendVarselTilEsyfovarsel(any()) } throws RuntimeException("Kafka feil")
                    varselOutboxDao.createPending(lagEndring(kandidat = true))

                    runScheduler()

                    assertThat(varselOutboxRecipientDao.getPending()).hasSize(1)
                    val sentCount = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM VARSEL_OUTBOX_RECIPIENT WHERE status = 'SENT'",
                        Int::class.java,
                    )
                    assertThat(sentCount).isEqualTo(0)
                }

                it("delvis retry — sender kun PENDING-mottakere, ikke allerede SENT") {
                    kandidatIDb()
                    every { narmesteLederService.getAllNarmesteLederRelations(any()) } returns listOf(lagNarmesteLederRelasjon())
                    varselOutboxDao.createPending(lagEndring(kandidat = true))

                    // Første run: ekspanderer og sender begge mottakere
                    runScheduler()

                    // Reset AT til PENDING (NL forblir SENT) — simuler at AT-sending feilet
                    jdbcTemplate.update(
                        "UPDATE VARSEL_OUTBOX_RECIPIENT SET status = 'PENDING' WHERE mottaker_fnr = ?",
                        UserConstants.ARBEIDSTAKER_FNR,
                    )

                    // Nullstill mock-kall-historikk for å verifisere kun neste run
                    clearMocks(esyfovarselProducer, answers = false, recordedCalls = true)
                    every { esyfovarselProducer.sendVarselTilEsyfovarsel(any()) } returns Unit

                    runScheduler()

                    // Bare AT-mottaker er PENDING → kun 1 ny sending
                    verify(exactly = 1) { esyfovarselProducer.sendVarselTilEsyfovarsel(any()) }
                    assertThat(varselOutboxRecipientDao.getPending()).isEmpty()
                }
            }
        }
    }

    private fun kandidatIDb() {
        dialogmotekandidatService.receiveDialogmotekandidatEndring(
            lagEndring(kandidat = true)
        )
        jdbcTemplate.update("DELETE FROM VARSEL_OUTBOX_RECIPIENT")
        jdbcTemplate.update("DELETE FROM VARSEL_OUTBOX")
    }

    private fun runScheduler() {
        every { leaderElectionClient.isLeader() } returns true
        scheduler.run()
    }

    private fun lagEndring(
        kandidat: Boolean,
        createdAt: OffsetDateTime = OffsetDateTime.now(ZoneId.of("Europe/Oslo")),
    ): KafkaDialogmotekandidatEndring =
        KafkaDialogmotekandidatEndring(
            uuid = UUID.randomUUID().toString(),
            createdAt = createdAt,
            personIdentNumber = UserConstants.ARBEIDSTAKER_FNR,
            kandidat = kandidat,
            arsak = if (kandidat) "STOPPUNKT" else "UNNTAK",
        )

    private fun lagNarmesteLederRelasjon() = NarmesteLederRelasjonDTO(
        uuid = UUID.randomUUID().toString(),
        arbeidstakerPersonIdentNumber = UserConstants.ARBEIDSTAKER_FNR,
        virksomhetsnavn = "Test AS",
        virksomhetsnummer = UserConstants.VIRKSOMHETSNUMMER,
        narmesteLederPersonIdentNumber = UserConstants.LEDER_FNR,
        narmesteLederTelefonnummer = "99999999",
        narmesteLederEpost = "leder@test.no",
        narmesteLederNavn = "Test Leder",
        aktivFom = LocalDate.now().minusYears(1),
        aktivTom = null,
        arbeidsgiverForskutterer = null,
        timestamp = LocalDateTime.now(),
        status = NarmesteLederRelasjonStatus.INNMELDT_AKTIV,
    )
}
