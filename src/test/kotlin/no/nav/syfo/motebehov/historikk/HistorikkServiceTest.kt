package no.nav.syfo.motebehov.historikk

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.syfo.consumer.pdl.PdlConsumer
import no.nav.syfo.consumer.pdl.PdlPersonNavn
import no.nav.syfo.consumer.pdl.fullName
import no.nav.syfo.motebehov.MotebehovService
import no.nav.syfo.testhelper.UserConstants
import no.nav.syfo.testhelper.UserConstants.VEILEDER_ID
import no.nav.syfo.testhelper.generator.MotebehovGenerator
import no.nav.syfo.testhelper.generator.generatePdlHentPerson
import java.time.LocalDateTime

class HistorikkServiceTest : DescribeSpec({

    val pdlConsumer: PdlConsumer = mockk<PdlConsumer>()
    val motebehovService: MotebehovService = mockk<MotebehovService>()
    val historikkService = HistorikkService(
        motebehovService,
        pdlConsumer
    )

    val motebehovGenerator = MotebehovGenerator()

    val pdlPersonResponseNL1 = generatePdlHentPerson(
        PdlPersonNavn(
            UserConstants.PERSON_NAME_FIRST,
            UserConstants.PERSON_NAME_MIDDLE,
            UserConstants.PERSON_NAME_LAST
        ),
        null
    )
    val pdlPersonResponseNL3 = generatePdlHentPerson(
        PdlPersonNavn(
            UserConstants.PERSON_NAME_FIRST,
            UserConstants.PERSON_NAME_MIDDLE,
            UserConstants.PERSON_NAME_LAST
        ),
        null
    )

    beforeTest {
        every { pdlConsumer.aktorid(NL1_FNR) } returns NL1_AKTORID
        every { pdlConsumer.fnr(NL1_AKTORID) } returns NL1_FNR
        every { pdlConsumer.fnr(NL3_AKTORID) } returns NL3_FNR
        every { pdlConsumer.person(NL1_FNR) } returns pdlPersonResponseNL1
        every { pdlConsumer.person(NL3_FNR) } returns pdlPersonResponseNL3
    }

    describe("HistorikkService") {
        it("should return history of different types") {
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

            every { motebehovService.hentMotebehovListe(SM_FNR) } returns listOf(
                motebehov1,
                motebehov2
            )

            val historikkForSykmeldt = historikkService.hentHistorikkListe(SM_FNR)
            historikkForSykmeldt.size shouldBe 4
            val historikkOpprettetMotebehovTekst1 = historikkForSykmeldt[0].tekst
            val historikkOpprettetMotebehovTekst2 = historikkForSykmeldt[1].tekst
            val historikkLesteMotebehovTekst1 = historikkForSykmeldt[2].tekst
            val historikkLesteMotebehovTekst2 = historikkForSykmeldt[3].tekst
            historikkOpprettetMotebehovTekst1 shouldBe
                pdlPersonResponseNL1.fullName() + HistorikkService.HAR_SVART_PAA_MOTEBEHOV
            historikkOpprettetMotebehovTekst2 shouldBe
                pdlPersonResponseNL3.fullName() + HistorikkService.HAR_SVART_PAA_MOTEBEHOV
            historikkLesteMotebehovTekst1 shouldBe HistorikkService.MOTEBEHOVET_BLE_LEST_AV + VEILEDER_ID
            historikkLesteMotebehovTekst2 shouldBe HistorikkService.MOTEBEHOVET_BLE_LEST_AV + VEILEDER_ID
        }
    }
}) {
    companion object {
        private const val SM_FNR = "10000000001"
        private const val NL1_FNR = "20000000001"
        private const val NL3_FNR = "20000000003"
        private const val NL1_AKTORID = "2000000000001"
        private const val NL3_AKTORID = "2000000000003"
    }
}
