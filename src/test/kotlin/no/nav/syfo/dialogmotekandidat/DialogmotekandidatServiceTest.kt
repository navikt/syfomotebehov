package no.nav.syfo.dialogmotekandidat

import io.kotest.core.extensions.ApplyExtension
import io.kotest.extensions.spring.SpringExtension
import no.nav.syfo.IntegrationTest
import no.nav.syfo.LocalApplication
import no.nav.syfo.dialogmotekandidat.database.DialogmotekandidatDAO
import no.nav.syfo.dialogmotekandidat.database.DialogmotekandidatEndringArsak
import no.nav.syfo.dialogmotekandidat.database.VarselOutboxDao
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
import java.util.*

@TestConfiguration
@SpringBootTest(classes = [LocalApplication::class])
@ApplyExtension(SpringExtension::class)
@DirtiesContext
internal class DialogmotekandidatServiceTest : IntegrationTest() {

    @Autowired
    private lateinit var dialogmotekandidatDAO: DialogmotekandidatDAO

    @Autowired
    private lateinit var varselOutboxDao: VarselOutboxDao

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    private lateinit var dialogmotekandidatService: DialogmotekandidatService

    init {
        beforeTest {
            jdbcTemplate.update("DELETE FROM VARSEL_OUTBOX_RECIPIENT")
            jdbcTemplate.update("DELETE FROM VARSEL_OUTBOX")
            jdbcTemplate.update("DELETE FROM DIALOGMOTEKANDIDAT")
        }

        it("skalLagreNyKandidatEndring") {
            val existingKandidat = dialogmotekandidatDAO.get(UserConstants.ARBEIDSTAKER_FNR)
            assertThat(existingKandidat).isNull()

            dialogmotekandidatService.receiveDialogmotekandidatEndring(
                generateDialogmotekandidatEndring(kandidat = true, arsak = DialogmotekandidatEndringArsak.STOPPUNKT.name)
            )

            val kandidatAfterKafkaMessage = dialogmotekandidatDAO.get(UserConstants.ARBEIDSTAKER_FNR)
            assertThat(kandidatAfterKafkaMessage).isNotNull
            assertThat(kandidatAfterKafkaMessage?.databaseUpdatedAt).isNotNull
            assertThat(kandidatAfterKafkaMessage?.personIdentNumber).isEqualTo(UserConstants.ARBEIDSTAKER_FNR)
            assertThat(kandidatAfterKafkaMessage?.kandidat).isTrue
            assertThat(kandidatAfterKafkaMessage?.arsak).isEqualTo(DialogmotekandidatEndringArsak.STOPPUNKT)
        }

        it("skalOppdatereKandidatVedNyEndring") {
            dialogmotekandidatService.receiveDialogmotekandidatEndring(
                generateDialogmotekandidatEndring(kandidat = true, arsak = DialogmotekandidatEndringArsak.STOPPUNKT.name)
            )

            val existingKandidat = dialogmotekandidatDAO.get(UserConstants.ARBEIDSTAKER_FNR)
            assertThat(existingKandidat?.kandidat).isTrue
            assertThat(existingKandidat?.arsak).isEqualTo(DialogmotekandidatEndringArsak.STOPPUNKT)

            dialogmotekandidatService.receiveDialogmotekandidatEndring(
                generateDialogmotekandidatEndring(kandidat = false, arsak = DialogmotekandidatEndringArsak.UNNTAK.name)
            )

            val updatedKandidat = dialogmotekandidatDAO.get(UserConstants.ARBEIDSTAKER_FNR)
            assertThat(updatedKandidat?.kandidat).isFalse
            assertThat(updatedKandidat?.arsak).isEqualTo(DialogmotekandidatEndringArsak.UNNTAK)
        }

        it("rekjoringIGalRekkefolgeSkalOgsaaFungere") {
            val forsteKandidatMelding = generateDialogmotekandidatEndring(
                kandidat = true,
                arsak = DialogmotekandidatEndringArsak.STOPPUNKT.name,
                createdAt = OffsetDateTime.now(ZoneId.of("Europe/Oslo")).minusDays(10),
            )
            val andreKandidatMelding = generateDialogmotekandidatEndring(
                kandidat = false,
                arsak = DialogmotekandidatEndringArsak.UNNTAK.name,
                createdAt = OffsetDateTime.now(ZoneId.of("Europe/Oslo")).minusDays(6),
            )
            val tredjeKandidatMelding = generateDialogmotekandidatEndring(
                kandidat = false,
                arsak = DialogmotekandidatEndringArsak.DIALOGMOTE_FERDIGSTILT.name,
                createdAt = OffsetDateTime.now(ZoneId.of("Europe/Oslo")).minusDays(1),
            )

            // Mottar meldingene i omvendt rekkefølge
            dialogmotekandidatService.receiveDialogmotekandidatEndring(tredjeKandidatMelding)
            dialogmotekandidatService.receiveDialogmotekandidatEndring(andreKandidatMelding)
            dialogmotekandidatService.receiveDialogmotekandidatEndring(forsteKandidatMelding)

            val kandidatStatus = dialogmotekandidatDAO.get(UserConstants.ARBEIDSTAKER_FNR)
            assertThat(kandidatStatus?.kandidat).isFalse
            assertThat(kandidatStatus?.arsak).isEqualTo(DialogmotekandidatEndringArsak.DIALOGMOTE_FERDIGSTILT)
        }

        it("skalOppretteOutboxEntryDersomNyKandidat") {
            dialogmotekandidatService.receiveDialogmotekandidatEndring(
                generateDialogmotekandidatEndring(
                    kandidat = true,
                    arsak = DialogmotekandidatEndringArsak.STOPPUNKT.name,
                    createdAt = OffsetDateTime.now(ZoneId.of("Europe/Oslo")).minusDays(10),
                )
            )

            val pending = varselOutboxDao.getPending()
            assertThat(pending).hasSize(1)
            assertThat(pending.first().kilde).isEqualTo(VarselOutboxDao.KILDE_DIALOGMOTEKANDIDAT_LISTENER)
            assertThat(pending.first().status.name).isEqualTo("PENDING")
        }

        it("skalOppretteSkippedOutboxEntryDersomAlleredeKandidat") {
            dialogmotekandidatService.receiveDialogmotekandidatEndring(
                generateDialogmotekandidatEndring(kandidat = true, arsak = DialogmotekandidatEndringArsak.STOPPUNKT.name)
            )
            dialogmotekandidatService.receiveDialogmotekandidatEndring(
                generateDialogmotekandidatEndring(kandidat = true, arsak = DialogmotekandidatEndringArsak.STOPPUNKT.name)
            )

            val skippedCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM VARSEL_OUTBOX WHERE status = 'SKIPPED'",
                Int::class.java,
            )
            assertThat(skippedCount).isEqualTo(1)
            assertThat(varselOutboxDao.getPending()).hasSize(1)
        }

        it("skalOpprettePendingOutboxEntryForFerdigstillingDersomIkkeKandidat") {
            dialogmotekandidatService.receiveDialogmotekandidatEndring(
                generateDialogmotekandidatEndring(
                    kandidat = false,
                    arsak = DialogmotekandidatEndringArsak.UNNTAK.name,
                )
            )

            val pending = varselOutboxDao.getPending()
            assertThat(pending).hasSize(1)
            assertThat(pending.first().kilde).isEqualTo(VarselOutboxDao.KILDE_DIALOGMOTEKANDIDAT_LISTENER)
        }

        it("skalOppretteToPendingOutboxEntriesDersomKandidatSaaFerdigstilt") {
            dialogmotekandidatService.receiveDialogmotekandidatEndring(
                generateDialogmotekandidatEndring(
                    kandidat = true,
                    arsak = DialogmotekandidatEndringArsak.STOPPUNKT.name,
                    createdAt = OffsetDateTime.now(ZoneId.of("Europe/Oslo")).minusDays(10),
                )
            )
            dialogmotekandidatService.receiveDialogmotekandidatEndring(
                generateDialogmotekandidatEndring(
                    kandidat = false,
                    arsak = DialogmotekandidatEndringArsak.UNNTAK.name,
                )
            )

            val pending = varselOutboxDao.getPending()
            assertThat(pending).hasSize(2)
        }
    }

    private fun generateDialogmotekandidatEndring(
        kandidat: Boolean,
        arsak: String,
        createdAt: OffsetDateTime? = null,
    ): KafkaDialogmotekandidatEndring {
        return KafkaDialogmotekandidatEndring(
            UUID.randomUUID().toString(),
            createdAt ?: OffsetDateTime.now(ZoneId.of("Europe/Oslo")).minusMinutes(1),
            UserConstants.ARBEIDSTAKER_FNR,
            kandidat,
            arsak,
        )
    }
}
