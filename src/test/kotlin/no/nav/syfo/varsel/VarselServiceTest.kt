package no.nav.syfo.varsel

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import no.nav.syfo.LocalApplication
import no.nav.syfo.consumer.narmesteleder.NarmesteLederRelasjonDTO
import no.nav.syfo.consumer.narmesteleder.NarmesteLederRelasjonStatus
import no.nav.syfo.consumer.narmesteleder.NarmesteLederService
import no.nav.syfo.dialogmote.DialogmoteStatusService
import no.nav.syfo.motebehov.MotebehovService
import no.nav.syfo.oppfolgingstilfelle.OppfolgingstilfelleService
import no.nav.syfo.oppfolgingstilfelle.database.PersonOppfolgingstilfelle
import no.nav.syfo.testhelper.UserConstants
import no.nav.syfo.varsel.esyfovarsel.EsyfovarselService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.LocalDate
import java.time.LocalDateTime

@ExtendWith(SpringExtension::class)
@SpringBootTest(classes = [LocalApplication::class])
@DirtiesContext
class VarselServiceTest {

    @MockkBean
    private lateinit var motebehovService: MotebehovService

    @MockkBean
    private lateinit var oppfolgingstilfelleService: OppfolgingstilfelleService

    @MockkBean(relaxed = true)
    private lateinit var esyfovarselService: EsyfovarselService

    @MockkBean
    private lateinit var dialogmoteStatusService: DialogmoteStatusService

    @MockkBean
    private lateinit var narmesteLederService: NarmesteLederService

    @Autowired
    private lateinit var varselService: VarselServiceV2

    private val userFnr = UserConstants.ARBEIDSTAKER_FNR

    private val narmesteLederFnr1 = "11111111111"
    private val narmesteLederFnr2 = "33333333333"
    private val virksomhetsnummer1 = "777888555"
    private val virksomhetsnummer2 = "222222222"

    @BeforeEach
    fun setup() {
        every { oppfolgingstilfelleService.getActiveOppfolgingstilfelleForArbeidstaker(any()) } returns createOppfolgingstilfelle()
        every {
            oppfolgingstilfelleService.getActiveOppfolgingstilfelleForArbeidsgiver(
                any(),
                any()
            )
        } returns createOppfolgingstilfelle()
        every { dialogmoteStatusService.isDialogmotePlanlagtEtterDato(any(), any(), any()) } returns false
        every {
            motebehovService.hentMotebehovListeForArbeidstakerOpprettetAvLeder(
                any(),
                any(),
                any()
            )
        } returns emptyList()
        every { motebehovService.hentMotebehovListeForOgOpprettetAvArbeidstaker(any()) } returns emptyList()
        every { narmesteLederService.getAllNarmesteLederRelations(userFnr) } returns createNarmesteLederRelations()
    }

    @Test
    fun skalSendeVarselTilDenSykmeldteOgAlleNarmesteLedereMedAktivtOppfolgingstilfelle() {
        varselService.sendSvarBehovVarsel(userFnr, "")

        verify(exactly = 1) { esyfovarselService.sendSvarMotebehovVarselTilArbeidstaker(userFnr) }
        verify(exactly = 2) { esyfovarselService.sendSvarMotebehovVarselTilNarmesteLeder(any(), userFnr, any()) }
    }

    @Test
    fun senderIkkeVarselOmIkkeAktivtOppfolgingstilfelle() {
        every { oppfolgingstilfelleService.getActiveOppfolgingstilfelleForArbeidsgiver(userFnr, virksomhetsnummer2) } returns null

        varselService.sendSvarBehovVarsel(userFnr, "")

        verify(exactly = 1) { esyfovarselService.sendSvarMotebehovVarselTilArbeidstaker(userFnr) }
        verify(exactly = 1) { esyfovarselService.sendSvarMotebehovVarselTilNarmesteLeder(any(), userFnr, any()) }
    }

    @Test
    fun senderIkkeVarselDersomDialogmotePlanlagt() {
        every { dialogmoteStatusService.isDialogmotePlanlagtEtterDato(userFnr, any(), any()) } returns true

        varselService.sendSvarBehovVarsel(userFnr, "")

        verify(exactly = 0) { esyfovarselService.sendSvarMotebehovVarselTilArbeidstaker(userFnr) }
        verify(exactly = 0) { esyfovarselService.sendSvarMotebehovVarselTilNarmesteLeder(any(), userFnr, any()) }
    }

    private fun createOppfolgingstilfelle(): PersonOppfolgingstilfelle {
        return PersonOppfolgingstilfelle(
            userFnr,
            LocalDate.now().minusWeeks(4),
            LocalDate.now().minusMonths(2)
        )
    }

    private fun createNarmesteLederRelations(): List<NarmesteLederRelasjonDTO> {
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
                timestamp = LocalDateTime.now(),
                status = NarmesteLederRelasjonStatus.INNMELDT_AKTIV
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
                timestamp = LocalDateTime.now(),
                status = NarmesteLederRelasjonStatus.INNMELDT_AKTIV
            )
        )
    }
}
