package no.nav.syfo.historikk

import no.nav.syfo.aktorregister.AktorregisterConsumer
import no.nav.syfo.aktorregister.domain.AktorId
import no.nav.syfo.aktorregister.domain.Fodselsnummer
import no.nav.syfo.domain.rest.*
import no.nav.syfo.pdl.*
import no.nav.syfo.service.MotebehovService
import no.nav.syfo.service.VeilederOppgaverService
import no.nav.syfo.testhelper.UserConstants
import no.nav.syfo.testhelper.generatePdlHentPerson
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.*
import org.mockito.junit.MockitoJUnitRunner
import java.time.LocalDateTime

@RunWith(MockitoJUnitRunner::class)
class HistorikkServiceTest {

    @Mock
    private lateinit var aktorregisterConsumer: AktorregisterConsumer
    @Mock
    private lateinit var motebehovService: MotebehovService
    @Mock
    private lateinit var pdlConsumer: PdlConsumer
    @Mock
    private lateinit var veilederOppgaverService: VeilederOppgaverService
    @InjectMocks
    private lateinit var historikkService: HistorikkService

    private val pdlPersonResponseNL1 = generatePdlHentPerson(
            PdlPersonNavn(
                    UserConstants.PERSON_NAME_FIRST,
                    UserConstants.PERSON_NAME_MIDDLE,
                    UserConstants.PERSON_NAME_LAST
            ),
            null
    )
    private val pdlPersonResponseNL3 = generatePdlHentPerson(
            PdlPersonNavn(
                    UserConstants.PERSON_NAME_FIRST,
                    UserConstants.PERSON_NAME_MIDDLE,
                    UserConstants.PERSON_NAME_LAST
            ),
            null
    )

    @Before
    fun setup() {
        Mockito.`when`(aktorregisterConsumer.getFnrForAktorId(AktorId(NL1_AKTORID))).thenReturn(NL1_FNR)
        Mockito.`when`(aktorregisterConsumer.getFnrForAktorId(AktorId(NL3_AKTORID))).thenReturn(NL3_FNR)
        Mockito.`when`(pdlConsumer.person(Fodselsnummer(NL1_FNR))).thenReturn(pdlPersonResponseNL1)
        Mockito.`when`(pdlConsumer.person(Fodselsnummer(NL3_FNR))).thenReturn(pdlPersonResponseNL3)
    }

    @Test
    fun hentHistorikkServiceSkalReturnereHistorikkAvForskjelligeTyper() {
        Mockito.`when`(motebehovService.hentMotebehovListe(ArgumentMatchers.any(Fodselsnummer::class.java))).thenReturn(listOf(
                Motebehov()
                        .opprettetAv(NL3_AKTORID)
                        .opprettetDato(LocalDateTime.now()),
                Motebehov()
                        .opprettetAv(NL1_AKTORID)
                        .opprettetDato(LocalDateTime.now().minusMinutes(2L))
        ))
        Mockito.`when`(veilederOppgaverService.getVeilederoppgave(SM_FNR)).thenReturn(listOf(
                VeilederOppgave()
                        .type("SE_OPPFOLGINGSPLAN")
                        .status("IKKE_STARTET")
                        .sistEndretAv(Z_IDENT1)
                        .sistEndret("2018-01-01"),
                VeilederOppgave()
                        .type("MOTEBEHOV_MOTTATT")
                        .status("FERDIG")
                        .sistEndretAv(Z_IDENT2)
                        .sistEndret("2018-01-02"),
                VeilederOppgave()
                        .type("MOTEBEHOV_MOTTATT")
                        .status("IKKE_STARTET")
                        .sistEndretAv(Z_IDENT3)
                        .sistEndret("2018-01-03")
        ))
        val historikkForSykmeldt = historikkService.hentHistorikkListe(SM_FNR)
        Assertions.assertThat(historikkForSykmeldt.size).isEqualTo(3)
        val historikkOpprettetMotebehovTekst1 = historikkForSykmeldt[0].tekst
        val historikkOpprettetMotebehovTekst2 = historikkForSykmeldt[1].tekst
        val historikkLesteMotebehovTekst = historikkForSykmeldt[2].tekst
        Assertions.assertThat(historikkOpprettetMotebehovTekst1).isEqualTo(pdlPersonResponseNL1.fullName() + HistorikkService.HAR_SVART_PAA_MOTEBEHOV)
        Assertions.assertThat(historikkOpprettetMotebehovTekst2).isEqualTo(pdlPersonResponseNL3.fullName() + HistorikkService.HAR_SVART_PAA_MOTEBEHOV)
        Assertions.assertThat(historikkLesteMotebehovTekst).isEqualTo(HistorikkService.MOTEBEHOVET_BLE_LEST_AV + Z_IDENT2)
    }

    companion object {
        private const val SM_FNR = "10000000001"
        private const val NL1_FNR = "20000000001"
        private const val NL2_FNR = "20000000002"
        private const val NL3_FNR = "20000000003"
        private const val SM_AKTORID = "1000000000001"
        private const val NL1_AKTORID = "2000000000001"
        private const val NL2_AKTORID = "2000000000002"
        private const val NL3_AKTORID = "2000000000003"
        private const val NL1_NAVN = "NL1 navn"
        private const val NL3_NAVN = "NL3 navn"
        private const val ORGNR_1 = "123456789"
        private const val ORGNR_2 = "234567890"
        private const val ORGNR_3 = "345678901"
        private const val ORGNAVN_1 = "Bedrift 1"
        private const val ORGNAVN_2 = "Bedrift 2"
        private const val SVAR_MOTEBEHOV = "SVAR_MOTEBEHOV"
        private const val AKTIVITETKRAV_VARSEL = "AKTIVITETKRAV_VARSEL"
        private const val NAERMESTE_LEDER_SVAR_MOTEBEHOV = "NAERMESTE_LEDER_SVAR_MOTEBEHOV"
        private const val NAERMESTE_LEDER_LES_SYKMELDING = "NAERMESTE_LEDER_LES_SYKMELDING"
        private const val Z_IDENT1 = "ZIdent1"
        private const val Z_IDENT2 = "ZIdent2"
        private const val Z_IDENT3 = "ZIdent3"
    }
}
