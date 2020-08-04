package no.nav.syfo.motebehov.historikk

import no.nav.syfo.consumer.aktorregister.AktorregisterConsumer
import no.nav.syfo.consumer.aktorregister.domain.AktorId
import no.nav.syfo.consumer.aktorregister.domain.Fodselsnummer
import no.nav.syfo.consumer.pdl.*
import no.nav.syfo.motebehov.MotebehovService
import no.nav.syfo.testhelper.UserConstants
import no.nav.syfo.testhelper.UserConstants.VEILEDER_ID
import no.nav.syfo.testhelper.generator.MotebehovGenerator
import no.nav.syfo.testhelper.generator.generatePdlHentPerson
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.*
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.LocalDateTime

@ExtendWith(SpringExtension::class)
class HistorikkServiceTest {

    @Mock
    private lateinit var aktorregisterConsumer: AktorregisterConsumer

    @Mock
    private lateinit var motebehovService: MotebehovService

    @Mock
    private lateinit var pdlConsumer: PdlConsumer

    @InjectMocks
    private lateinit var historikkService: HistorikkService

    private val motebehovGenerator = MotebehovGenerator()

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

    @BeforeEach
    fun setup() {
        Mockito.`when`(aktorregisterConsumer.getFnrForAktorId(AktorId(NL1_AKTORID))).thenReturn(NL1_FNR)
        Mockito.`when`(aktorregisterConsumer.getFnrForAktorId(AktorId(NL3_AKTORID))).thenReturn(NL3_FNR)
        Mockito.`when`(pdlConsumer.person(Fodselsnummer(NL1_FNR))).thenReturn(pdlPersonResponseNL1)
        Mockito.`when`(pdlConsumer.person(Fodselsnummer(NL3_FNR))).thenReturn(pdlPersonResponseNL3)
    }

    @Test
    fun hentHistorikkServiceSkalReturnereHistorikkAvForskjelligeTyper() {
        val motebehov1 = motebehovGenerator.generateMotebehov().copy(
            opprettetAv = NL3_AKTORID,
            opprettetDato = LocalDateTime.now(),
            behandletVeilederIdent = VEILEDER_ID,
            behandletTidspunkt = LocalDateTime.now()
        )
        val motebehov2 = motebehovGenerator.generateMotebehov().copy(
            opprettetAv = NL1_AKTORID,
            opprettetDato = LocalDateTime.now().minusMinutes(2L),
            behandletVeilederIdent = VEILEDER_ID,
            behandletTidspunkt = LocalDateTime.now()
        )
        Mockito.`when`(motebehovService.hentMotebehovListe(Fodselsnummer(SM_FNR))).thenReturn(listOf(
            motebehov1,
            motebehov2
        ))
        val historikkForSykmeldt = historikkService.hentHistorikkListe(SM_FNR)
        Assertions.assertThat(historikkForSykmeldt.size).isEqualTo(4)
        val historikkOpprettetMotebehovTekst1 = historikkForSykmeldt[0].tekst
        val historikkOpprettetMotebehovTekst2 = historikkForSykmeldt[1].tekst
        val historikkLesteMotebehovTekst1 = historikkForSykmeldt[2].tekst
        val historikkLesteMotebehovTekst2 = historikkForSykmeldt[3].tekst
        Assertions.assertThat(historikkOpprettetMotebehovTekst1).isEqualTo(pdlPersonResponseNL1.fullName() + HistorikkService.HAR_SVART_PAA_MOTEBEHOV)
        Assertions.assertThat(historikkOpprettetMotebehovTekst2).isEqualTo(pdlPersonResponseNL3.fullName() + HistorikkService.HAR_SVART_PAA_MOTEBEHOV)
        Assertions.assertThat(historikkLesteMotebehovTekst1).isEqualTo(HistorikkService.MOTEBEHOVET_BLE_LEST_AV + VEILEDER_ID)
        Assertions.assertThat(historikkLesteMotebehovTekst2).isEqualTo(HistorikkService.MOTEBEHOVET_BLE_LEST_AV + VEILEDER_ID)
    }

    companion object {
        private const val SM_FNR = "10000000001"
        private const val NL1_FNR = "20000000001"
        private const val NL3_FNR = "20000000003"
        private const val NL1_AKTORID = "2000000000001"
        private const val NL3_AKTORID = "2000000000003"
    }
}
