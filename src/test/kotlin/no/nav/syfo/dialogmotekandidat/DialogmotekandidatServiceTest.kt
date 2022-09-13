package no.nav.syfo.dialogmotekandidat

import com.ninjasquad.springmockk.MockkBean
import io.mockk.verify
import no.nav.syfo.LocalApplication
import no.nav.syfo.consumer.aktorregister.domain.Fodselsnummer
import no.nav.syfo.dialogmotekandidat.database.DialogmotekandidatDAO
import no.nav.syfo.dialogmotekandidat.database.DialogmotekandidatEndringArsak
import no.nav.syfo.dialogmotekandidat.kafka.KafkaDialogmotekandidatEndring
import no.nav.syfo.testhelper.UserConstants
import no.nav.syfo.varsel.VarselServiceV2
import org.assertj.core.api.Assertions.assertThat
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

    @MockkBean(relaxed = true)
    private lateinit var varselServiceV2: VarselServiceV2

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

        assertThat(existingKandidat).isNull()

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

        assertThat(kandidatAfterKafkaMessage).isNotNull
        assertThat(kandidatAfterKafkaMessage?.databaseUpdatedAt).isNotNull
        assertThat(kandidatAfterKafkaMessage?.personIdentNumber).isEqualTo(UserConstants.ARBEIDSTAKER_FNR)
        assertThat(kandidatAfterKafkaMessage?.kandidat).isTrue
        assertThat(kandidatAfterKafkaMessage?.arsak).isEqualTo(DialogmotekandidatEndringArsak.STOPPUNKT)
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

        assertThat(existingKandidat).isNotNull
        assertThat(existingKandidat?.kandidat).isTrue
        assertThat(existingKandidat?.arsak).isEqualTo(DialogmotekandidatEndringArsak.STOPPUNKT)

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

        assertThat(updatedKandidat).isNotNull
        assertThat(updatedKandidat?.kandidat).isFalse
        assertThat(updatedKandidat?.arsak).isEqualTo(DialogmotekandidatEndringArsak.UNNTAK)
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
        assertThat(kandidatStatus).isNotNull
        assertThat(kandidatStatus?.kandidat).isFalse
        assertThat(kandidatStatus?.arsak).isEqualTo(DialogmotekandidatEndringArsak.DIALOGMOTE_FERDIGSTILT)
    }

    @Test
    fun skalSendeVarselDersomNyKandidat() {
        val forsteGangKandidat = generateDialogmotekandidatEndring(
            kandidat = true,
            arsak = DialogmotekandidatEndringArsak.STOPPUNKT.name,
            OffsetDateTime.now(ZoneId.of("Europe/Oslo")).minusDays(10)
        )

        dialogmotekandidatService.receiveDialogmotekandidatEndring(forsteGangKandidat)

        verify(exactly = 1) { varselServiceV2.sendSvarBehovVarsel(any()) }
    }

    @Test
    fun skalIkkeSendeVarselDersomIkkeKandidat() {
        val forsteGangKandidat = generateDialogmotekandidatEndring(
            kandidat = false,
            arsak = DialogmotekandidatEndringArsak.UNNTAK.name,
            OffsetDateTime.now(ZoneId.of("Europe/Oslo")).minusDays(10)
        )

        dialogmotekandidatService.receiveDialogmotekandidatEndring(forsteGangKandidat)

        verify(exactly = 0) { varselServiceV2.sendSvarBehovVarsel(any()) }
    }
}
