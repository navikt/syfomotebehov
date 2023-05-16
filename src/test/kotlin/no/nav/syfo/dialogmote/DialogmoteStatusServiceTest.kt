package no.nav.syfo.dialogmote

import com.ninjasquad.springmockk.MockkBean
import io.mockk.verify
import no.nav.syfo.LocalApplication
import no.nav.syfo.dialogmote.avro.KDialogmoteStatusEndring
import no.nav.syfo.dialogmote.database.DialogmoteDAO
import no.nav.syfo.dialogmote.database.DialogmoteStatusEndringType
import no.nav.syfo.testhelper.UserConstants
import no.nav.syfo.util.convertLocalDateTimeToInstant
import no.nav.syfo.varsel.VarselServiceV2
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import javax.inject.Inject

@ExtendWith(SpringExtension::class)
@SpringBootTest(classes = [LocalApplication::class])
@DirtiesContext
internal class DialogmoteStatusServiceTest {

    @Inject
    private lateinit var dialogmoteDAO: DialogmoteDAO

    @Inject
    private lateinit var jdbcTemplate: JdbcTemplate

    @Inject
    private lateinit var dialogmoteStatusService: DialogmoteStatusService

    @MockkBean(relaxed = true)
    private lateinit var varselServiceV2: VarselServiceV2

    val externMoteUUID = "bae778f2-a085-11e8-98d0-529269fb1459"
    val dialogmotetidspunkt = convertLocalDateTimeToInstant(LocalDateTime.now().plusWeeks(4))

    private fun generateInnkalling(
        moteUUID: String?,
        dialogmoteStatusEndringType: DialogmoteStatusEndringType,
        motetidspunkt: Instant?,
    ): KDialogmoteStatusEndring {
        return KDialogmoteStatusEndring(
            moteUUID ?: UUID.randomUUID().toString(),
            motetidspunkt ?: dialogmotetidspunkt,
            dialogmoteStatusEndringType.name,
            convertLocalDateTimeToInstant(LocalDateTime.now()), // statusendring tidspnkt
            UserConstants.ARBEIDSTAKER_FNR,
            UserConstants.VIRKSOMHETSNUMMER,
            "",
            "",
            convertLocalDateTimeToInstant(LocalDateTime.now().minusWeeks(4)),
            false,
            false,
            false,
        )
    }

    @BeforeEach
    fun cleanup() {
        val sqlDeleteAll = "DELETE FROM DIALOGMOTER"
        jdbcTemplate.update(sqlDeleteAll)
    }

    @Test
    fun skalLagreNyttDialogmoteInnkalling() {
        val moteFraDBBefore = dialogmoteDAO.get(
            UserConstants.ARBEIDSTAKER_FNR, UserConstants.VIRKSOMHETSNUMMER, externMoteUUID
        )

        assertThat(moteFraDBBefore.size).isEqualTo(0)

        dialogmoteStatusService.receiveKDialogmoteStatusendring(
            generateInnkalling(
                externMoteUUID, DialogmoteStatusEndringType.INNKALT,
                null
            )
        )

        val moteFraDBAfter = dialogmoteDAO.get(
            UserConstants.ARBEIDSTAKER_FNR, UserConstants.VIRKSOMHETSNUMMER, externMoteUUID
        )

        assertThat(moteFraDBAfter.size).isEqualTo(1)
        assertThat(moteFraDBAfter[0].personIdent).isEqualTo(UserConstants.ARBEIDSTAKER_FNR)

        verify(exactly = 1) { varselServiceV2.ferdigstillSvarMotebehovVarselForArbeidstaker(any()) }
        verify(exactly = 1) { varselServiceV2.ferdigstillSvarMotebehovVarselForNarmesteLeder(any(), any()) }
    }

    @Test
    fun skalIkkeLagreNyttDialogmoteHvisIkkeInnkallingEllerTidsendring() {
        val moteFraDBBefore = dialogmoteDAO.get(
            UserConstants.ARBEIDSTAKER_FNR, UserConstants.VIRKSOMHETSNUMMER, externMoteUUID
        )

        assertThat(moteFraDBBefore.size).isEqualTo(0)

        dialogmoteStatusService.receiveKDialogmoteStatusendring(
            generateInnkalling(
                externMoteUUID, DialogmoteStatusEndringType.AVLYST,
                null
            )
        )

        val moteFraDBAfter = dialogmoteDAO.get(
            UserConstants.ARBEIDSTAKER_FNR, UserConstants.VIRKSOMHETSNUMMER, externMoteUUID
        )

        assertThat(moteFraDBAfter.isEmpty()).isTrue
    }

    @Test
    fun skalSletteDialogmoteHvisNyHendelseErAvlystEllerFerdigstilt() {
        dialogmoteStatusService.receiveKDialogmoteStatusendring(
            generateInnkalling(
                externMoteUUID, DialogmoteStatusEndringType.INNKALT,
                null
            )
        )
        val moteFraDBBefore = dialogmoteDAO.get(
            UserConstants.ARBEIDSTAKER_FNR, UserConstants.VIRKSOMHETSNUMMER, externMoteUUID
        )

        assertThat(moteFraDBBefore.size).isEqualTo(1)
        assertThat(moteFraDBBefore[0].personIdent).isEqualTo(UserConstants.ARBEIDSTAKER_FNR)

        dialogmoteStatusService.receiveKDialogmoteStatusendring(
            generateInnkalling(
                externMoteUUID, DialogmoteStatusEndringType.AVLYST,
                null
            )
        )
        val moteFraDBAfter = dialogmoteDAO.get(
            UserConstants.ARBEIDSTAKER_FNR, UserConstants.VIRKSOMHETSNUMMER, externMoteUUID
        )

        assertThat(moteFraDBAfter.isEmpty()).isTrue
    }

    @Test
    fun skalOppdatereDialogmoteStatusTypeHvisStatusTypeErInnkallingEllerNyttTidSted() {
        dialogmoteStatusService.receiveKDialogmoteStatusendring(
            generateInnkalling(
                externMoteUUID, DialogmoteStatusEndringType.INNKALT,
                null
            )
        )
        val moteFraDBBefore = dialogmoteDAO.get(
            UserConstants.ARBEIDSTAKER_FNR, UserConstants.VIRKSOMHETSNUMMER, externMoteUUID
        )

        assertThat(moteFraDBBefore.size).isEqualTo(1)
        assertThat(moteFraDBBefore[0].personIdent).isEqualTo(UserConstants.ARBEIDSTAKER_FNR)
        assertThat(moteFraDBBefore[0].statusEndringType).isEqualTo(DialogmoteStatusEndringType.INNKALT)

        dialogmoteStatusService.receiveKDialogmoteStatusendring(
            generateInnkalling(
                externMoteUUID, DialogmoteStatusEndringType.NYTT_TID_STED,
                null
            )
        )
        val moteFraDBAfter = dialogmoteDAO.get(
            UserConstants.ARBEIDSTAKER_FNR, UserConstants.VIRKSOMHETSNUMMER, externMoteUUID
        )

        assertThat(moteFraDBAfter.size).isEqualTo(1)
        assertThat(moteFraDBAfter[0].personIdent).isEqualTo(UserConstants.ARBEIDSTAKER_FNR)
        assertThat(moteFraDBAfter[0].statusEndringType).isEqualTo(DialogmoteStatusEndringType.NYTT_TID_STED)
    }

    @Test
    fun skalIkkeOppdatereDialogmoteHvisEndringsTidspunktErEldreEnnIDatabase() {
        dialogmoteStatusService.receiveKDialogmoteStatusendring(
            generateInnkalling(
                externMoteUUID,
                DialogmoteStatusEndringType.INNKALT,
                null
            )
        )

        val moteFraDBBefore = dialogmoteDAO.get(
            UserConstants.ARBEIDSTAKER_FNR, UserConstants.VIRKSOMHETSNUMMER, externMoteUUID
        )

        assertThat(moteFraDBBefore.size).isEqualTo(1)
        assertThat(moteFraDBBefore[0].personIdent).isEqualTo(UserConstants.ARBEIDSTAKER_FNR)
        assertThat(moteFraDBBefore[0].statusEndringType).isEqualTo(DialogmoteStatusEndringType.INNKALT)

        val oldRecord = generateInnkalling(
            externMoteUUID,
            DialogmoteStatusEndringType.NYTT_TID_STED,
            null
        )

        oldRecord.setStatusEndringTidspunkt(convertLocalDateTimeToInstant(LocalDateTime.now().minusWeeks(99)))

        dialogmoteStatusService.receiveKDialogmoteStatusendring(oldRecord)
        val moteFraDBAfter = dialogmoteDAO.get(
            UserConstants.ARBEIDSTAKER_FNR, UserConstants.VIRKSOMHETSNUMMER, externMoteUUID
        )

        assertThat(moteFraDBAfter.size).isEqualTo(1)
        assertThat(moteFraDBAfter[0].personIdent).isEqualTo(UserConstants.ARBEIDSTAKER_FNR)
        assertThat(moteFraDBAfter[0].statusEndringType)
            .isNotEqualTo(DialogmoteStatusEndringType.NYTT_TID_STED)
    }

    @Test
    fun skalReturnereTrueHvisDetErMoteEtterDato() {
        dialogmoteStatusService.receiveKDialogmoteStatusendring(
            generateInnkalling(
                externMoteUUID,
                DialogmoteStatusEndringType.INNKALT,
                null
            )
        )

        val isMote = dialogmoteStatusService.isDialogmotePlanlagtEtterDato(
            UserConstants.ARBEIDSTAKER_FNR,
            UserConstants.VIRKSOMHETSNUMMER,
            LocalDate.now()
        )

        assertThat(isMote).isTrue
    }

    @Test
    fun skalReturnereTrueHvisDetErMoteSammeDato() {
        val todayLocalDateTime = LocalDateTime.now()
        val todayInstant = convertLocalDateTimeToInstant(todayLocalDateTime)
        dialogmoteStatusService.receiveKDialogmoteStatusendring(
            generateInnkalling(
                externMoteUUID,
                DialogmoteStatusEndringType.INNKALT,
                todayInstant
            )
        )

        val isMote = dialogmoteStatusService.isDialogmotePlanlagtEtterDato(
            UserConstants.ARBEIDSTAKER_FNR,
            UserConstants.VIRKSOMHETSNUMMER,
            todayLocalDateTime.toLocalDate()
        )

        assertThat(isMote).isTrue
    }

    @Test
    fun skalReturnereFalseHvisDetIkkeErMoteEtterDato() {
        dialogmoteStatusService.receiveKDialogmoteStatusendring(
            generateInnkalling(
                externMoteUUID,
                DialogmoteStatusEndringType.INNKALT,
                convertLocalDateTimeToInstant(LocalDateTime.now().minusWeeks(4))
            )
        )

        val isMote = dialogmoteStatusService.isDialogmotePlanlagtEtterDato(
            UserConstants.ARBEIDSTAKER_FNR,
            UserConstants.VIRKSOMHETSNUMMER,
            LocalDate.now()
        )

        assertThat(isMote).isFalse
    }
}
