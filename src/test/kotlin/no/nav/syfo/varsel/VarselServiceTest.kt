package no.nav.syfo.varsel

import io.kotest.core.spec.style.DescribeSpec
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.syfo.LocalApplication
import no.nav.syfo.consumer.narmesteleder.NarmesteLederRelasjonDTO
import no.nav.syfo.consumer.narmesteleder.NarmesteLederRelasjonStatus
import no.nav.syfo.consumer.narmesteleder.NarmesteLederService
import no.nav.syfo.dialogmote.database.Dialogmote
import no.nav.syfo.dialogmote.database.DialogmoteDAO
import no.nav.syfo.dialogmote.database.DialogmoteStatusEndringType
import no.nav.syfo.metric.Metric
import no.nav.syfo.motebehov.MotebehovService
import no.nav.syfo.motebehov.motebehovstatus.MotebehovStatusHelper
import no.nav.syfo.oppfolgingstilfelle.OppfolgingstilfelleService
import no.nav.syfo.oppfolgingstilfelle.database.PersonOppfolgingstilfelle
import no.nav.syfo.testhelper.UserConstants
import no.nav.syfo.varsel.esyfovarsel.EsyfovarselService
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.LocalDate
import java.time.LocalDateTime.now
import java.util.UUID.randomUUID

@ExtendWith(SpringExtension::class)
@SpringBootTest(classes = [LocalApplication::class])
@DirtiesContext
class VarselServiceTest : DescribeSpec({

    val motebehovService: MotebehovService = mockk<MotebehovService>()

    val oppfolgingstilfelleService: OppfolgingstilfelleService = mockk<OppfolgingstilfelleService>()

    val esyfovarselService: EsyfovarselService = mockk<EsyfovarselService>(relaxed = true)

    val dialogmoteDAO: DialogmoteDAO = mockk<DialogmoteDAO>()

    val narmesteLederService: NarmesteLederService = mockk<NarmesteLederService>()
    val metric = mockk<Metric>()
    val motebehovStatusHelper = mockk<MotebehovStatusHelper>()
    val varselService = VarselServiceV2(
        metric,
        motebehovService,
        motebehovStatusHelper,
        oppfolgingstilfelleService,
        esyfovarselService,
        narmesteLederService,
        dialogmoteDAO,
    )

    val userFnr = UserConstants.ARBEIDSTAKER_FNR

    val narmesteLederFnr1 = "11111111111"
    val narmesteLederFnr2 = "33333333333"
    val virksomhetsnummer1 = "777888555"
    val virksomhetsnummer2 = "222222222"

    beforeTest {
        clearMocks(
            motebehovService,
            oppfolgingstilfelleService,
            esyfovarselService,
            dialogmoteDAO,
            narmesteLederService,
            metric,
            motebehovStatusHelper
        )
        every { oppfolgingstilfelleService.getActiveOppfolgingstilfelleForArbeidstaker(any()) } returns createOppfolgingstilfelle()
        every {
            oppfolgingstilfelleService.getActiveOppfolgingstilfelleForArbeidsgiver(
                any(),
                any(),
            )
        } returns createOppfolgingstilfelle()
        every { dialogmoteDAO.getAktiveDialogmoterEtterDato(any(), any()) } returns emptyList()
        every {
            motebehovService.hentMotebehovListeForArbeidstakerOpprettetAvLeder(
                any(),
                any(),
                any(),
            )
        } returns emptyList()
        every { motebehovService.hentMotebehovListeForOgOpprettetAvArbeidstaker(any()) } returns emptyList()
        every { narmesteLederService.getAllNarmesteLederRelations(userFnr) } returns createNarmesteLederRelations()
        every { motebehovStatusHelper.isSvarBehovVarselAvailable(any(), any()) } returns true
        every { metric.tellHendelse(any()) } returns Unit
    }

    describe("Varsel service") {
        it("skalSendeVarselTilDenSykmeldteOgAlleNarmesteLedereMedAktivtOppfolgingstilfelle") {
            varselService.sendSvarBehovVarsel(userFnr, "")

            verify(exactly = 1) { esyfovarselService.sendSvarMotebehovVarselTilArbeidstaker(userFnr) }
            verify(exactly = 2) { esyfovarselService.sendSvarMotebehovVarselTilNarmesteLeder(any(), userFnr, any()) }
        }

        it("senderIkkeVarselOmIkkeAktivtOppfolgingstilfelle") {
            every {
                oppfolgingstilfelleService.getActiveOppfolgingstilfelleForArbeidsgiver(
                    userFnr,
                    virksomhetsnummer2
                )
            } returns null
            every { esyfovarselService.sendSvarMotebehovVarselTilNarmesteLeder(any(), any(), any()) } returns Unit
            varselService.sendSvarBehovVarsel(userFnr, "")

            verify(exactly = 1) { esyfovarselService.sendSvarMotebehovVarselTilArbeidstaker(userFnr) }
            verify(exactly = 1) {
                esyfovarselService.sendSvarMotebehovVarselTilNarmesteLeder(
                    narmesteLederFnr1,
                    userFnr,
                    virksomhetsnummer1
                )
            }
        }

        it("senderIkkeVarselOmIkkeSykmeldtLenger") {
            every {
                oppfolgingstilfelleService.getActiveOppfolgingstilfelleForArbeidsgiver(
                    userFnr,
                    virksomhetsnummer2
                )
            } returns PersonOppfolgingstilfelle(
                userFnr,
                LocalDate.now().minusMonths(4),
                LocalDate.now().minusDays(1),
            )

            varselService.sendSvarBehovVarsel(userFnr, "")

            verify(exactly = 1) { esyfovarselService.sendSvarMotebehovVarselTilArbeidstaker(userFnr) }
            verify(exactly = 1) {
                esyfovarselService.sendSvarMotebehovVarselTilNarmesteLeder(
                    narmesteLederFnr2,
                    userFnr,
                    any()
                )
            }
        }

        it("senderIkkeVarselDersomDialogmotePlanlagt") {
            every { dialogmoteDAO.getAktiveDialogmoterEtterDato(any(), any()) } returns listOf(
                Dialogmote(
                    randomUUID(),
                    randomUUID(),
                    now(),
                    now(),
                    DialogmoteStatusEndringType.INNKALT,
                    userFnr,
                    virksomhetsnummer1,
                ),
            )
            every { esyfovarselService.sendSvarMotebehovVarselTilArbeidstaker(any()) } returns Unit
            every { esyfovarselService.sendSvarMotebehovVarselTilNarmesteLeder(any(), any(), any()) } returns Unit

            varselService.sendSvarBehovVarsel(userFnr, "")

            verify(exactly = 0) { esyfovarselService.sendSvarMotebehovVarselTilArbeidstaker(userFnr) }
            verify(exactly = 0) { esyfovarselService.sendSvarMotebehovVarselTilNarmesteLeder(any(), userFnr, any()) }
        }
    }
}) {
    companion object {
        val userFnr = UserConstants.ARBEIDSTAKER_FNR
        val narmesteLederFnr1 = "11111111111"
        val narmesteLederFnr2 = "33333333333"
        val virksomhetsnummer1 = "777888555"
        val virksomhetsnummer2 = "222222222"

        fun createOppfolgingstilfelle(): PersonOppfolgingstilfelle {
            return PersonOppfolgingstilfelle(
                userFnr,
                LocalDate.now().minusMonths(4),
                LocalDate.now().plusWeeks(2),
            )
        }

        fun createNarmesteLederRelations(): List<NarmesteLederRelasjonDTO> {
            return listOf(
                NarmesteLederRelasjonDTO(
                    uuid = "123",
                    arbeidstakerPersonIdentNumber = userFnr,
                    virksomhetsnavn = "Yolomaster AS",
                    virksomhetsnummer = virksomhetsnummer1,
                    narmesteLederPersonIdentNumber = narmesteLederFnr1,
                    narmesteLederTelefonnummer = "123",
                    narmesteLederEpost = "123@123.no",
                    narmesteLederNavn = "Grebb",
                    aktivFom = LocalDate.now().minusYears(10),
                    aktivTom = null,
                    arbeidsgiverForskutterer = false,
                    timestamp = now(),
                    status = NarmesteLederRelasjonStatus.INNMELDT_AKTIV,
                ),
                NarmesteLederRelasjonDTO(
                    uuid = "234",
                    arbeidstakerPersonIdentNumber = userFnr,
                    virksomhetsnavn = "Kakemester AS",
                    virksomhetsnummer = virksomhetsnummer2,
                    narmesteLederPersonIdentNumber = narmesteLederFnr2,
                    narmesteLederTelefonnummer = "333",
                    narmesteLederEpost = "234@234.no",
                    narmesteLederNavn = "Labben",
                    aktivFom = LocalDate.now().minusYears(16),
                    aktivTom = null,
                    arbeidsgiverForskutterer = false,
                    timestamp = now(),
                    status = NarmesteLederRelasjonStatus.INNMELDT_AKTIV,
                ),
            )
        }
    }
}
