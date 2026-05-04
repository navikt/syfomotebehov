package no.nav.syfo.dialogmotekandidat

import io.kotest.core.extensions.ApplyExtension
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.syfo.IntegrationTest
import no.nav.syfo.LocalApplication
import no.nav.syfo.dialogmotekandidat.database.DialogmotekandidatDAO
import no.nav.syfo.dialogmotekandidat.database.DialogmotekandidatEndringArsak
import no.nav.syfo.dialogmotekandidat.database.DialogmotekandidatVarselStatusDao
import no.nav.syfo.dialogmotekandidat.database.DialogmotekandidatVarselType
import no.nav.syfo.dialogmotekandidat.kafka.KafkaDialogmotekandidatEndring
import no.nav.syfo.testhelper.UserConstants
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.annotation.DirtiesContext
import java.time.OffsetDateTime
import java.time.ZoneId
import java.util.*

@TestConfiguration
@SpringBootTest(classes = [LocalApplication::class])
@ApplyExtension(SpringExtension::class)
@DirtiesContext
internal class DialogmotekandidatServiceTest : IntegrationTest() {

    @Autowired
    private lateinit var dialogmotekandidatVarselStatusDao: DialogmotekandidatVarselStatusDao

    @Autowired
    private lateinit var dialogmotekandidatDAO: DialogmotekandidatDAO

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    private lateinit var dialogmotekandidatService: DialogmotekandidatService


    init {
        beforeTest {
            jdbcTemplate.update("DELETE FROM DIALOGMOTEKANDIDAT")
            jdbcTemplate.update("DELETE FROM dialogkandidat_varsel_status")
        }

        it("skalLagreNyKandidatEndring") {
            val existingKandidat = dialogmotekandidatDAO.get(
                UserConstants.ARBEIDSTAKER_FNR
            )

            existingKandidat.shouldBeNull()

            dialogmotekandidatService.receiveDialogmotekandidatEndring(
                generateDialogmotekandidatEndring(
                    kandidat = true,
                    arsak = DialogmotekandidatEndringArsak.STOPPUNKT.name,
                    null
                )
            )

            val kandidatAfterKafkaMessage = dialogmotekandidatDAO.get(
                UserConstants.ARBEIDSTAKER_FNR
            )

            kandidatAfterKafkaMessage.shouldNotBeNull()
            with(kandidatAfterKafkaMessage) {
                databaseUpdatedAt.shouldNotBeNull()
                personIdentNumber shouldBe UserConstants.ARBEIDSTAKER_FNR
                kandidat shouldBe true
                arsak shouldBe DialogmotekandidatEndringArsak.STOPPUNKT
            }
        }

        it("skalOppdatereKandidatVedNyEndring") {
            dialogmotekandidatService.receiveDialogmotekandidatEndring(
                generateDialogmotekandidatEndring(
                    kandidat = true,
                    arsak = DialogmotekandidatEndringArsak.STOPPUNKT.name,
                    null
                )
            )

            val existingKandidat = dialogmotekandidatDAO.get(
                UserConstants.ARBEIDSTAKER_FNR
            )

            existingKandidat.shouldNotBeNull()
            existingKandidat.kandidat shouldBe true
            existingKandidat.arsak shouldBe DialogmotekandidatEndringArsak.STOPPUNKT

            dialogmotekandidatService.receiveDialogmotekandidatEndring(
                generateDialogmotekandidatEndring(
                    kandidat = false,
                    arsak = DialogmotekandidatEndringArsak.UNNTAK.name,
                    null
                )
            )

            val updatedKandidat = dialogmotekandidatDAO.get(
                UserConstants.ARBEIDSTAKER_FNR
            )

            updatedKandidat.shouldNotBeNull()
            updatedKandidat.kandidat shouldBe false
            updatedKandidat.arsak shouldBe DialogmotekandidatEndringArsak.UNNTAK
        }

        it("rekjoringIGalRekkefolgeSkalOgsaaFungere") {
            val forsteKandidatMelding = generateDialogmotekandidatEndring(
                kandidat = true,
                arsak = DialogmotekandidatEndringArsak.STOPPUNKT.name,
                OffsetDateTime.now(ZoneId.of("Europe/Oslo")).minusDays(10)
            )
            val andreKandidatMelding = generateDialogmotekandidatEndring(
                kandidat = false,
                arsak = DialogmotekandidatEndringArsak.UNNTAK.name,
                OffsetDateTime.now(ZoneId.of("Europe/Oslo")).minusDays(6)
            )
            val tredjeKandidatMelding = generateDialogmotekandidatEndring(
                kandidat = false,
                arsak = DialogmotekandidatEndringArsak.DIALOGMOTE_FERDIGSTILT.name,
                OffsetDateTime.now(ZoneId.of("Europe/Oslo")).minusDays(1)
            )

            // Mottar meldingene i omvendt rekkefølge
            dialogmotekandidatService.receiveDialogmotekandidatEndring(tredjeKandidatMelding)
            dialogmotekandidatService.receiveDialogmotekandidatEndring(andreKandidatMelding)
            dialogmotekandidatService.receiveDialogmotekandidatEndring(forsteKandidatMelding)

            val kandidatStatus = dialogmotekandidatDAO.get(
                UserConstants.ARBEIDSTAKER_FNR
            )

            // Nyeste melding er den som har blitt persistert
            kandidatStatus.shouldNotBeNull()
            kandidatStatus.kandidat shouldBe false
            kandidatStatus.arsak shouldBe DialogmotekandidatEndringArsak.DIALOGMOTE_FERDIGSTILT
        }

        it("skalOppretteVarselPendingRadDersomNyKandidat") {
            val melding = generateDialogmotekandidatEndring(
                kandidat = true,
                arsak = DialogmotekandidatEndringArsak.STOPPUNKT.name,
                OffsetDateTime.now(ZoneId.of("Europe/Oslo")).minusDays(10)
            )
            dialogmotekandidatService.receiveDialogmotekandidatEndring(melding)

            dialogmotekandidatVarselStatusDao.getPendingByType(DialogmotekandidatVarselType.VARSEL).apply {
                shouldHaveSize(1)
                with(first()) {
                    this.fnr shouldBe UserConstants.ARBEIDSTAKER_FNR
                    type shouldBe DialogmotekandidatVarselType.VARSEL
                }
            }
        }

        it("skalOppretteFerdigstillPendingRadDersomIkkeKandidat") {
            val melding = generateDialogmotekandidatEndring(
                kandidat = false,
                arsak = DialogmotekandidatEndringArsak.UNNTAK.name,
                OffsetDateTime.now(ZoneId.of("Europe/Oslo")).minusDays(10)
            )
            dialogmotekandidatService.receiveDialogmotekandidatEndring(melding)

            dialogmotekandidatVarselStatusDao.getPendingByType(DialogmotekandidatVarselType.FERDIGSTILL).apply {
                shouldHaveSize(1)
                with(first()) { type shouldBe DialogmotekandidatVarselType.FERDIGSTILL }
            }
        }

        it("skalIgnorereKandidatSomAlleredeErKandidat") {
            // First message: candidate becomes true
            dialogmotekandidatService.receiveDialogmotekandidatEndring(
                generateDialogmotekandidatEndring(
                    kandidat = true,
                    arsak = DialogmotekandidatEndringArsak.STOPPUNKT.name,
                    OffsetDateTime.now(ZoneId.of("Europe/Oslo")).minusDays(10)
                )
            )
            // Clear the outbox row
            jdbcTemplate.update("DELETE FROM dialogkandidat_varsel_status")

            // Second message: still candidate (different uuid but same fnr, newer time)
            dialogmotekandidatService.receiveDialogmotekandidatEndring(
                generateDialogmotekandidatEndring(
                    kandidat = true,
                    arsak = DialogmotekandidatEndringArsak.STOPPUNKT.name,
                    OffsetDateTime.now(ZoneId.of("Europe/Oslo")).minusMinutes(1)
                )
            )

            val pendingVarsel = dialogmotekandidatVarselStatusDao.getPendingByType(DialogmotekandidatVarselType.VARSEL)
            pendingVarsel.shouldBeEmpty()
        }

        it("skalIgnorereEldreKafkaMelding") {
            // Insert newer message first
            dialogmotekandidatService.receiveDialogmotekandidatEndring(
                generateDialogmotekandidatEndring(
                    kandidat = true,
                    arsak = DialogmotekandidatEndringArsak.STOPPUNKT.name,
                    OffsetDateTime.now(ZoneId.of("Europe/Oslo")).minusMinutes(1)
                )
            )
            jdbcTemplate.update("DELETE FROM dialogkandidat_varsel_status")

            // Try to insert older message
            dialogmotekandidatService.receiveDialogmotekandidatEndring(
                generateDialogmotekandidatEndring(
                    kandidat = true,
                    arsak = DialogmotekandidatEndringArsak.STOPPUNKT.name,
                    OffsetDateTime.now(ZoneId.of("Europe/Oslo")).minusDays(10)
                )
            )

            val pendingVarsel = dialogmotekandidatVarselStatusDao.getPendingByType(DialogmotekandidatVarselType.VARSEL)
            pendingVarsel.shouldBeEmpty()
        }
    }

    private fun generateDialogmotekandidatEndring(
        kandidat: Boolean,
        arsak: String,
        createdAt: OffsetDateTime?
    ): KafkaDialogmotekandidatEndring {
        return KafkaDialogmotekandidatEndring(
            UUID.randomUUID().toString(),
            createdAt ?: OffsetDateTime.now(ZoneId.of("Europe/Oslo")).minusMinutes(1),
            UserConstants.ARBEIDSTAKER_FNR,
            kandidat,
            arsak
        )
    }
}
