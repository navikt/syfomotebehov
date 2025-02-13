package no.nav.syfo.dialogmote

import com.ninjasquad.springmockk.MockkBean
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.verify
import no.nav.syfo.IntegrationTest
import no.nav.syfo.LocalApplication
import no.nav.syfo.dialogmote.avro.KDialogmoteStatusEndring
import no.nav.syfo.dialogmote.database.DialogmoteDAO
import no.nav.syfo.dialogmote.database.DialogmoteStatusEndringType
import no.nav.syfo.testhelper.UserConstants
import no.nav.syfo.util.convertLocalDateTimeToInstant
import no.nav.syfo.varsel.VarselServiceV2
import org.assertj.core.api.Assertions.assertThat
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.annotation.DirtiesContext
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

//@ExtendWith(SpringExtension::class)
@TestConfiguration
@SpringBootTest(classes = [LocalApplication::class])
@DirtiesContext
class DialogmoteStatusServiceTest : IntegrationTest() {

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    private lateinit var dialogmoteDAO: DialogmoteDAO

    @Autowired
    private lateinit var dialogmoteStatusService: DialogmoteStatusService

    @MockkBean(relaxed = true)
    private lateinit var varselServiceV2: VarselServiceV2

    val externMoteUUID = "bae778f2-a085-11e8-98d0-529269fb1459"
    val dialogmotetidspunkt = convertLocalDateTimeToInstant(LocalDateTime.now().plusWeeks(4))


    init {
        extensions(SpringExtension)
        beforeTest {
            val sqlDeleteAll = "DELETE FROM DIALOGMOTE"
            jdbcTemplate.update(sqlDeleteAll)
        }

        describe("Dialogmøte status service") {
            it("will save new dialogmøte innkalling") {

                val moteFraDBBefore = dialogmoteDAO.get(
                    UserConstants.ARBEIDSTAKER_FNR, UserConstants.VIRKSOMHETSNUMMER, externMoteUUID
                )

                moteFraDBBefore.size shouldBe 0

                dialogmoteStatusService.receiveKDialogmoteStatusendring(
                    generateInnkalling(
                        externMoteUUID, DialogmoteStatusEndringType.INNKALT,
                        null
                    )
                )

                val moteFraDBAfter = dialogmoteDAO.get(
                    UserConstants.ARBEIDSTAKER_FNR, UserConstants.VIRKSOMHETSNUMMER, externMoteUUID
                )

                moteFraDBAfter.size shouldBe 1
                moteFraDBAfter[0].personIdent shouldBe UserConstants.ARBEIDSTAKER_FNR

                verify(exactly = 1) { varselServiceV2.ferdigstillSvarMotebehovVarselForArbeidstaker(any()) }
                verify(exactly = 1) { varselServiceV2.ferdigstillSvarMotebehovVarselForNarmesteLeder(any(), any()) }
            }

            it("will not save new dialogmøte HvisIkkeInnkallingEllerTidsendring") {
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

            it("will remove dialogmøte if new hendelse is avlyst or ferdigstilt") {
                dialogmoteStatusService.receiveKDialogmoteStatusendring(
                    generateInnkalling(
                        externMoteUUID, DialogmoteStatusEndringType.INNKALT,
                        null
                    )
                )
                val moteFraDBBefore = dialogmoteDAO.get(
                    UserConstants.ARBEIDSTAKER_FNR, UserConstants.VIRKSOMHETSNUMMER, externMoteUUID
                )

                moteFraDBBefore.size shouldBe 1
                moteFraDBBefore[0].personIdent shouldBe UserConstants.ARBEIDSTAKER_FNR

                dialogmoteStatusService.receiveKDialogmoteStatusendring(
                    generateInnkalling(
                        externMoteUUID, DialogmoteStatusEndringType.AVLYST,
                        null
                    )
                )
                val moteFraDBAfter = dialogmoteDAO.get(
                    UserConstants.ARBEIDSTAKER_FNR, UserConstants.VIRKSOMHETSNUMMER, externMoteUUID
                )

                moteFraDBAfter.isEmpty() shouldBe true
            }


            it("will update dialogmøte status type hvis status type er innkalling eller nytt tid sted") {
                dialogmoteStatusService.receiveKDialogmoteStatusendring(
                    generateInnkalling(
                        externMoteUUID, DialogmoteStatusEndringType.INNKALT,
                        null
                    )
                )
                val moteFraDBBefore = dialogmoteDAO.get(
                    UserConstants.ARBEIDSTAKER_FNR, UserConstants.VIRKSOMHETSNUMMER, externMoteUUID
                )

                moteFraDBBefore.size shouldBe 1
                moteFraDBBefore[0].personIdent shouldBe UserConstants.ARBEIDSTAKER_FNR
                moteFraDBBefore[0].statusEndringType shouldBe DialogmoteStatusEndringType.INNKALT

                dialogmoteStatusService.receiveKDialogmoteStatusendring(
                    generateInnkalling(
                        externMoteUUID, DialogmoteStatusEndringType.NYTT_TID_STED,
                        null
                    )
                )
                val moteFraDBAfter = dialogmoteDAO.get(
                    UserConstants.ARBEIDSTAKER_FNR, UserConstants.VIRKSOMHETSNUMMER, externMoteUUID
                )

                moteFraDBAfter.size shouldBe 1
                moteFraDBAfter[0].personIdent shouldBe UserConstants.ARBEIDSTAKER_FNR
                moteFraDBAfter[0].statusEndringType shouldBe DialogmoteStatusEndringType.NYTT_TID_STED
            }

            it("will not update dialogmøte status type hvis endrings tidspunkt er eldre enn i database") {
                dialogmoteStatusService.receiveKDialogmoteStatusendring(
                    generateInnkalling(
                        externMoteUUID, DialogmoteStatusEndringType.INNKALT,
                        null
                    )
                )
                val moteFraDBBefore = dialogmoteDAO.get(
                    UserConstants.ARBEIDSTAKER_FNR, UserConstants.VIRKSOMHETSNUMMER, externMoteUUID
                )

                moteFraDBBefore.size shouldBe 1
                moteFraDBBefore[0].personIdent shouldBe UserConstants.ARBEIDSTAKER_FNR
                moteFraDBBefore[0].statusEndringType shouldBe DialogmoteStatusEndringType.INNKALT

                val oldRecord = generateInnkalling(
                    externMoteUUID, DialogmoteStatusEndringType.NYTT_TID_STED,
                    null
                )

                oldRecord.setStatusEndringTidspunkt(convertLocalDateTimeToInstant(LocalDateTime.now().minusWeeks(99)))

                dialogmoteStatusService.receiveKDialogmoteStatusendring(oldRecord)
                val moteFraDBAfter = dialogmoteDAO.get(
                    UserConstants.ARBEIDSTAKER_FNR, UserConstants.VIRKSOMHETSNUMMER, externMoteUUID
                )

                moteFraDBAfter.size shouldBe 1
                moteFraDBAfter[0].personIdent shouldBe UserConstants.ARBEIDSTAKER_FNR
                moteFraDBAfter[0].statusEndringType shouldNotBe DialogmoteStatusEndringType.NYTT_TID_STED
            }

            it("will return true if dialogmøte is planned after date") {
                dialogmoteStatusService.receiveKDialogmoteStatusendring(
                    generateInnkalling(
                        externMoteUUID, DialogmoteStatusEndringType.INNKALT,
                        null
                    )
                )

                val isMote = dialogmoteStatusService.isDialogmotePlanlagtEtterDato(
                    UserConstants.ARBEIDSTAKER_FNR,
                    UserConstants.VIRKSOMHETSNUMMER,
                    LocalDate.now()
                )

                isMote shouldBe true
            }

            it("will return true if dialogmøte is same date") {
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

                isMote shouldBe true
            }

            it("will return false if no dialogmøte after date") {
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

                isMote shouldBe false
            }
        }
    }

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

}
