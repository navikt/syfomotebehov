package no.nav.syfo.varsel.budstikka

import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.extensions.ApplyExtension
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.just
import io.mockk.runs
import io.mockk.verify
import no.nav.budstikka.contract.EncodedDispatch
import no.nav.syfo.IntegrationTest
import no.nav.syfo.LocalApplication
import no.nav.syfo.consumer.narmesteleder.NarmesteLederService
import no.nav.syfo.dialogmote.DialogmoteStatusService
import no.nav.syfo.dialogmote.avro.KDialogmoteStatusEndring
import no.nav.syfo.dialogmote.database.DialogmoteStatusEndringType
import no.nav.syfo.testhelper.UserConstants
import no.nav.syfo.util.convertLocalDateTimeToInstant
import no.nav.syfo.varsel.VarselServiceV2
import no.nav.syfo.varsel.esyfovarsel.EsyfovarselService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.queryForObject
import org.springframework.test.annotation.DirtiesContext
import java.time.LocalDateTime

@TestConfiguration
@SpringBootTest(
    classes = [LocalApplication::class],
    properties = ["mock-oauth2-server.port=9925", "toggle.motebehov.mikrofrontend.via.budstikka=true"],
)
@ApplyExtension(SpringExtension::class)
@DirtiesContext
class BudstikkaMikrofrontendServiceTest : IntegrationTest() {
    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    private lateinit var budstikkaMikrofrontendService: BudstikkaMikrofrontendService

    @Autowired
    private lateinit var dialogmoteStatusService: DialogmoteStatusService

    @Autowired
    private lateinit var varselServiceV2: VarselServiceV2

    @MockkBean
    private lateinit var budstikkaProducer: BudstikkaProducer

    @MockkBean(relaxed = true)
    private lateinit var esyfovarselService: EsyfovarselService

    @MockkBean
    private lateinit var narmesteLederService: NarmesteLederService

    private val fnr = UserConstants.ARBEIDSTAKER_FNR
    private val moteUuid = "bae778f2-a085-11e8-98d0-529269fb1459"
    private val sendt = mutableListOf<EncodedDispatch>()

    init {
        beforeTest {
            jdbcTemplate.update("DELETE FROM dialogmote")
            jdbcTemplate.update("DELETE FROM ${BudstikkaMikrofrontendDAO.TABLE_NAME}")
            sendt.clear()
            every { budstikkaProducer.send(capture(sendt)) } just runs
            every { narmesteLederService.getAllNarmesteLederRelations(any()) } returns emptyList()
        }

        describe("syfo-dialog via Budstikka") {
            it("innkalling mens møtebehov er aktivt gir ingen disable") {
                budstikkaMikrofrontendService.vis(fnr)
                budstikkaMikrofrontendService.sendVentendeKommandoer()
                sendt.single().value shouldContain "\"MicrofrontendEnable\""
                sendt.clear()

                dialogmoteStatusService.receiveKDialogmoteStatusendring(statusendring(DialogmoteStatusEndringType.INNKALT))
                budstikkaMikrofrontendService.sendVentendeKommandoer()

                verify(exactly = 1) { esyfovarselService.ferdigstillSvarMotebehovForArbeidstaker(fnr) }
                sendt.shouldBeEmpty()
                antallKommandoer(MikrofrontendKommando.DISABLE) shouldBe 0
            }

            it("ferdigstilling mens møtet er aktivt gir ingen disable") {
                dialogmoteStatusService.receiveKDialogmoteStatusendring(statusendring(DialogmoteStatusEndringType.INNKALT))

                varselServiceV2.ferdigstillSvarMotebehovVarselForArbeidstaker(fnr)
                varselServiceV2.ferdigstillSvarMotebehovVarsel(fnr)
                budstikkaMikrofrontendService.sendVentendeKommandoer()

                sendt.shouldBeEmpty()
                antallKommandoer(MikrofrontendKommando.DISABLE) shouldBe 0
            }

            it("ferdigstilling uten aktivt møte gir disable") {
                varselServiceV2.ferdigstillSvarMotebehovVarselForArbeidstaker(fnr)
                budstikkaMikrofrontendService.sendVentendeKommandoer()

                with(sendt.single()) {
                    topic shouldBe "team-esyfo.budstikka.v1"
                    key shouldBe fnr
                    value shouldContain "\"MicrofrontendDisable\""
                    value shouldContain "\"syfo-dialog\""
                }
            }

            it("avlyst møte er ikke aktivt") {
                dialogmoteStatusService.receiveKDialogmoteStatusendring(statusendring(DialogmoteStatusEndringType.INNKALT))
                dialogmoteStatusService.receiveKDialogmoteStatusendring(
                    statusendring(DialogmoteStatusEndringType.AVLYST, LocalDateTime.now().plusSeconds(1)),
                )

                varselServiceV2.ferdigstillSvarMotebehovVarsel(fnr)
                budstikkaMikrofrontendService.sendVentendeKommandoer()

                sendt.single().value shouldContain "\"MicrofrontendDisable\""
            }

            it("endret beholder møtet, så ferdigstilling gir ingen disable") {
                dialogmoteStatusService.receiveKDialogmoteStatusendring(statusendring(DialogmoteStatusEndringType.INNKALT))
                dialogmoteStatusService.receiveKDialogmoteStatusendring(
                    statusendring(DialogmoteStatusEndringType.ENDRET, LocalDateTime.now().plusSeconds(1)),
                )

                varselServiceV2.ferdigstillSvarMotebehovVarselForArbeidstaker(fnr)
                budstikkaMikrofrontendService.sendVentendeKommandoer()

                sendt.shouldBeEmpty()
            }

            it("disable i kø sendes ikke når et møte er innkalt før sending") {
                varselServiceV2.ferdigstillSvarMotebehovVarselForArbeidstaker(fnr)
                dialogmoteStatusService.receiveKDialogmoteStatusendring(statusendring(DialogmoteStatusEndringType.INNKALT))

                budstikkaMikrofrontendService.sendVentendeKommandoer()

                sendt.shouldBeEmpty()
                status(MikrofrontendKommando.DISABLE) shouldBe MikrofrontendUtsendingStatus.SKIPPED.name
            }

            it("retry bruker samme EventId") {
                every { budstikkaProducer.send(capture(sendt)) } throws RuntimeException("Kafka nede") andThen Unit

                budstikkaMikrofrontendService.vis(fnr)
                budstikkaMikrofrontendService.sendVentendeKommandoer()
                klarForRetry()
                budstikkaMikrofrontendService.sendVentendeKommandoer()

                sendt shouldHaveSize 2
                sendt[1].eventId shouldBe sendt[0].eventId
                status(MikrofrontendKommando.ENABLE) shouldBe MikrofrontendUtsendingStatus.SENT.name
            }

            it("nyere kommando venter på eldre ventende kommando for samme person") {
                val annenFnr = "10987654321"
                every { budstikkaProducer.send(any()) } throws RuntimeException("Kafka nede")
                budstikkaMikrofrontendService.vis(fnr)
                budstikkaMikrofrontendService.sendVentendeKommandoer()

                every { budstikkaProducer.send(capture(sendt)) } just runs
                budstikkaMikrofrontendService.skjulHvisIngenAktivtDialogmote(fnr)
                budstikkaMikrofrontendService.vis(annenFnr)
                budstikkaMikrofrontendService.sendVentendeKommandoer()

                sendt.single().key shouldBe annenFnr
                status(MikrofrontendKommando.DISABLE) shouldBe MikrofrontendUtsendingStatus.PENDING.name
            }
        }
    }

    private fun antallKommandoer(kommando: MikrofrontendKommando): Int? =
        jdbcTemplate.queryForObject<Int>(
            "SELECT COUNT(*) FROM ${BudstikkaMikrofrontendDAO.TABLE_NAME} WHERE kommando = ?",
            kommando.name,
        )

    private fun status(kommando: MikrofrontendKommando): String? =
        jdbcTemplate.queryForObject<String>(
            "SELECT status FROM ${BudstikkaMikrofrontendDAO.TABLE_NAME} WHERE kommando = ? AND fnr = ?",
            kommando.name,
            fnr,
        )

    private fun klarForRetry() {
        jdbcTemplate.update("UPDATE ${BudstikkaMikrofrontendDAO.TABLE_NAME} SET next_retry_at = NOW()")
    }

    private fun statusendring(
        type: DialogmoteStatusEndringType,
        statusEndringTidspunkt: LocalDateTime = LocalDateTime.now(),
    ): KDialogmoteStatusEndring =
        KDialogmoteStatusEndring(
            moteUuid,
            convertLocalDateTimeToInstant(LocalDateTime.now().plusWeeks(4)),
            type.name,
            convertLocalDateTimeToInstant(statusEndringTidspunkt),
            fnr,
            UserConstants.VIRKSOMHETSNUMMER,
            "",
            "",
            convertLocalDateTimeToInstant(LocalDateTime.now().minusWeeks(4)),
            false,
            false,
            false,
        )
}
