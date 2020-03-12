package no.nav.syfo.historikk

import no.nav.syfo.aktorregister.AktorregisterConsumer
import no.nav.syfo.aktorregister.domain.AktorId
import no.nav.syfo.aktorregister.domain.Fodselsnummer
import no.nav.syfo.domain.rest.Motebehov
import no.nav.syfo.pdl.*
import no.nav.syfo.service.MotebehovService
import no.nav.syfo.testhelper.UserConstants
import no.nav.syfo.testhelper.UserConstants.VEILEDER_ID
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
                        .opprettetDato(LocalDateTime.now())
                        .behandletVeilederIdent(VEILEDER_ID)
                        .behandletTidspunkt(LocalDateTime.now()),
                Motebehov()
                        .opprettetAv(NL1_AKTORID)
                        .opprettetDato(LocalDateTime.now().minusMinutes(2L))
        ))
        val historikkForSykmeldt = historikkService.hentHistorikkListe(SM_FNR)
        Assertions.assertThat(historikkForSykmeldt.size).isEqualTo(3)
        val historikkOpprettetMotebehovTekst1 = historikkForSykmeldt[0].tekst
        val historikkOpprettetMotebehovTekst2 = historikkForSykmeldt[1].tekst
        val historikkLesteMotebehovTekst = historikkForSykmeldt[2].tekst
        Assertions.assertThat(historikkOpprettetMotebehovTekst1).isEqualTo(pdlPersonResponseNL1.fullName() + HistorikkService.HAR_SVART_PAA_MOTEBEHOV)
        Assertions.assertThat(historikkOpprettetMotebehovTekst2).isEqualTo(pdlPersonResponseNL3.fullName() + HistorikkService.HAR_SVART_PAA_MOTEBEHOV)
        Assertions.assertThat(historikkLesteMotebehovTekst).isEqualTo(HistorikkService.MOTEBEHOVET_BLE_LEST_AV + VEILEDER_ID)
    }

    companion object {
        private const val SM_FNR = "10000000001"
        private const val NL1_FNR = "20000000001"
        private const val NL3_FNR = "20000000003"
        private const val NL1_AKTORID = "2000000000001"
        private const val NL3_AKTORID = "2000000000003"
    }
}
