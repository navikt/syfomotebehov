package no.nav.syfo.dialogmotekandidat

import no.nav.syfo.LocalApplication
import no.nav.syfo.consumer.aktorregister.domain.Fodselsnummer
import no.nav.syfo.dialogmotekandidat.database.DialogmotekandidatDAO
import no.nav.syfo.dialogmotekandidat.database.DialogmotekandidatEndringArsak
import no.nav.syfo.dialogmotekandidat.kafka.KafkaDialogmotekandidatEndring
import no.nav.syfo.testhelper.UserConstants
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.OffsetDateTime
import java.time.ZoneId
import java.util.*
import javax.inject.Inject

@ExtendWith(SpringExtension::class)
@SpringBootTest(classes = [LocalApplication::class])
@DirtiesContext
internal class DialogmotekandidatServiceTest {

    @Inject
    private lateinit var dialogmotekandidatDAO: DialogmotekandidatDAO

    @Inject
    private lateinit var jdbcTemplate: JdbcTemplate

    @Inject
    private lateinit var dialogmotekandidatService: DialogmotekandidatService

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

    @BeforeEach
    fun cleanup() {
        val sqlDeleteAll = "DELETE FROM DIALOGMOTEKANDIDAT"
        jdbcTemplate.update(sqlDeleteAll)
    }

    @Test
    fun skalLagreNyKandidatEndring() {
        val existingKandidat = dialogmotekandidatDAO.get(
            Fodselsnummer(UserConstants.ARBEIDSTAKER_FNR)
        )

        Assertions.assertThat(existingKandidat).isNull()

        dialogmotekandidatService.receiveDialogmotekandidatEndring(
            generateDialogmotekandidatEndring(
                kandidat = true,
                arsak = DialogmotekandidatEndringArsak.STOPPUNKT.name,
                null
            )
        )

        val kandidatAfterKafkaMessage = dialogmotekandidatDAO.get(
            Fodselsnummer(UserConstants.ARBEIDSTAKER_FNR)
        )

        Assertions.assertThat(kandidatAfterKafkaMessage).isNotNull
        Assertions.assertThat(kandidatAfterKafkaMessage?.databaseUpdatedAt).isNotNull
        Assertions.assertThat(kandidatAfterKafkaMessage?.personIdentNumber).isEqualTo(UserConstants.ARBEIDSTAKER_FNR)
        Assertions.assertThat(kandidatAfterKafkaMessage?.kandidat).isTrue
        Assertions.assertThat(kandidatAfterKafkaMessage?.arsak).isEqualTo(DialogmotekandidatEndringArsak.STOPPUNKT)
    }

    @Test
    fun skalOppdatereKandidatVedNyEndring() {
        dialogmotekandidatService.receiveDialogmotekandidatEndring(
            generateDialogmotekandidatEndring(
                kandidat = true,
                arsak = DialogmotekandidatEndringArsak.STOPPUNKT.name,
                null
            )
        )

        val existingKandidat = dialogmotekandidatDAO.get(
            Fodselsnummer(UserConstants.ARBEIDSTAKER_FNR)
        )

        Assertions.assertThat(existingKandidat).isNotNull
        Assertions.assertThat(existingKandidat?.kandidat).isTrue
        Assertions.assertThat(existingKandidat?.arsak).isEqualTo(DialogmotekandidatEndringArsak.STOPPUNKT)

        dialogmotekandidatService.receiveDialogmotekandidatEndring(
            generateDialogmotekandidatEndring(
                kandidat = false,
                arsak = DialogmotekandidatEndringArsak.UNNTAK.name,
                null
            )
        )

        val updatedKandidat = dialogmotekandidatDAO.get(
            Fodselsnummer(UserConstants.ARBEIDSTAKER_FNR)
        )

        Assertions.assertThat(updatedKandidat).isNotNull
        Assertions.assertThat(updatedKandidat?.kandidat).isFalse
        Assertions.assertThat(updatedKandidat?.arsak).isEqualTo(DialogmotekandidatEndringArsak.UNNTAK)
    }

    @Test
    fun rekjoringIGalRekkefolgeSkalOgsaaFungere() {
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
            Fodselsnummer(UserConstants.ARBEIDSTAKER_FNR)
        )

        // Nyeste melding er den som har blitt persistert
        Assertions.assertThat(kandidatStatus).isNotNull
        Assertions.assertThat(kandidatStatus?.kandidat).isFalse
        Assertions.assertThat(kandidatStatus?.arsak).isEqualTo(DialogmotekandidatEndringArsak.DIALOGMOTE_FERDIGSTILT)
    }
}
