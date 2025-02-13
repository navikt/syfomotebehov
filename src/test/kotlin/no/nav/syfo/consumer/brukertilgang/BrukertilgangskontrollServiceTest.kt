package no.nav.syfo.consumer.brukertilgang

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import org.junit.jupiter.api.Assertions

class BrukertilgangskontrollServiceTest : DescribeSpec( {


    val brukertilgangConsumer: BrukertilgangConsumer = mockk<BrukertilgangConsumer>()
    val token = mockk<TokenValidationContextHolder>()
    val tilgangskontrollService = BrukertilgangService(
        token,
        brukertilgangConsumer)


    describe("BrukertilgangskontrollService") {
        it("has access to oppslått bruker when asking about self") {
            val oppslattFnr = "10038973552"

            every { brukertilgangConsumer.hasAccessToAnsatt(oppslattFnr) } returns false

            val tilgang = tilgangskontrollService.harTilgangTilOppslaattBruker(oppslattFnr, oppslattFnr)
            tilgang shouldBe true
        }

        it("has access to oppslått bruker when asking about ansatt") {
            val innloggetFnr = "44444444"
            val oppslattFnr = "10038973561"

            every { brukertilgangConsumer.hasAccessToAnsatt(oppslattFnr) } returns true

            val tilgang = tilgangskontrollService.harTilgangTilOppslaattBruker(innloggetFnr, oppslattFnr)
            Assertions.assertEquals(true, tilgang)
        }

        it("does not have access to oppslått bruker when asking about someone ikke ansatt") {
            val innloggetFnr = "5555555"
            val oppslattFnr = "555555566666"

            every { brukertilgangConsumer.hasAccessToAnsatt(oppslattFnr) } returns false

            val tilgang = tilgangskontrollService.harTilgangTilOppslaattBruker(innloggetFnr, oppslattFnr)
            tilgang shouldBe false
        }

        it("does not have access to another the itself or its ansatte") {
            val innloggetFnr = "666666"

            every { brukertilgangConsumer.hasAccessToAnsatt(any()) } returns false

            val tilgang = tilgangskontrollService.sporOmNoenAndreEnnSegSelvEllerEgneAnsatte(innloggetFnr, innloggetFnr)
            tilgang shouldBe false
        }

        it("sporOmNoenAndreEnnSegSelv Give False NårManSporOmEnAnsatt") {
            val innloggetFnr = "99999999"
            val oppslattFnr = "8888888"

            every { brukertilgangConsumer.hasAccessToAnsatt(oppslattFnr) } returns true

            val tilgang = tilgangskontrollService.sporOmNoenAndreEnnSegSelvEllerEgneAnsatte(innloggetFnr, oppslattFnr)
            tilgang shouldBe false
        }

        it("sporOmNoenAndreEnnSegSelvGirTrueNaarManSporOmEnSomIkkeErSegSelvOgIkkeAnsatt") {
            val innloggetFnr = "7777777"
            val oppslattFnr = "77778888"

            every { brukertilgangConsumer.hasAccessToAnsatt(oppslattFnr) } returns false

            val tilgang = tilgangskontrollService.sporOmNoenAndreEnnSegSelvEllerEgneAnsatte(innloggetFnr, oppslattFnr)
            Assertions.assertEquals(true, tilgang)
        }
    }
})