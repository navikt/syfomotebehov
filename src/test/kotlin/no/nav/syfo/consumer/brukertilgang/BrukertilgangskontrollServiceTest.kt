package no.nav.syfo.consumer.brukertilgang

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import no.nav.syfo.LocalApplication
import no.nav.syfo.consumer.pdl.PdlConsumer
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
@SpringBootTest(classes = [LocalApplication::class])
@DirtiesContext
class BrukertilgangskontrollServiceTest {

    @MockkBean
    private lateinit var brukertilgangConsumer: BrukertilgangConsumer

    @MockkBean
    private lateinit var pdlConsumer: PdlConsumer

    @Autowired
    private lateinit var tilgangskontrollService: BrukertilgangService

    @Test
    fun harTilgangTilOppslaattBrukerGirFalseNaarOppslaattBrukerErKode6() {
        val innloggetFnr = "3333333"
        val oppslattFnr = "11038973567"

        every { brukertilgangConsumer.hasAccessToAnsatt(oppslattFnr) } returns true
        every { pdlConsumer.isKode6(oppslattFnr) } returns true

        val tilgang = tilgangskontrollService.harTilgangTilOppslaattBruker(innloggetFnr, oppslattFnr)
        Assertions.assertEquals(false, tilgang)
    }

    @Test
    fun harTilgangTilOppslaattBrukerGirTrueNaarManSporOmSegSelv() {
        val oppslattFnr = "10038973552"

        every { brukertilgangConsumer.hasAccessToAnsatt(oppslattFnr) } returns false
        every { pdlConsumer.isKode6(oppslattFnr) } returns false

        val tilgang = tilgangskontrollService.harTilgangTilOppslaattBruker(oppslattFnr, oppslattFnr)
        Assertions.assertEquals(true, tilgang)
    }

    @Test
    fun harTilgangTilOppslaattBrukerGirTrueNaarManSporOmEnAnsatt() {
        val innloggetFnr = "44444444"
        val oppslattFnr = "10038973561"

        every { brukertilgangConsumer.hasAccessToAnsatt(oppslattFnr) } returns true
        every { pdlConsumer.isKode6(oppslattFnr) } returns false

        val tilgang = tilgangskontrollService.harTilgangTilOppslaattBruker(innloggetFnr, oppslattFnr)
        Assertions.assertEquals(true, tilgang)
    }

    @Test
    fun harTilgangTilOppslaattBrukerGirFalseNaarManSporOmEnSomIkkeErSegSelvOgIkkeAnsatt() {
        val innloggetFnr = "5555555"
        val oppslattFnr = "555555566666"

        every { brukertilgangConsumer.hasAccessToAnsatt(oppslattFnr) } returns false

        val tilgang = tilgangskontrollService.harTilgangTilOppslaattBruker(innloggetFnr, oppslattFnr)
        Assertions.assertEquals(false, tilgang)
    }

    @Test
    fun sporOmNoenAndreEnnSegSelvGirFalseNaarManSporOmSegSelv() {
        val innloggetFnr = "666666"

        every { brukertilgangConsumer.hasAccessToAnsatt(any()) } returns false

        val tilgang =
            tilgangskontrollService.sporOmNoenAndreEnnSegSelvEllerEgneAnsatte(innloggetFnr, innloggetFnr)
        Assertions.assertEquals(false, tilgang)
    }

    @Test
    fun sporOmNoenAndreEnnSegSelvGirFalseNaarManSporOmEnAnsatt() {
        val innloggetFnr = "99999999"
        val oppslattFnr = "8888888"

        every { brukertilgangConsumer.hasAccessToAnsatt(oppslattFnr) } returns true

        val tilgang =
            tilgangskontrollService.sporOmNoenAndreEnnSegSelvEllerEgneAnsatte(innloggetFnr, oppslattFnr)
        Assertions.assertEquals(false, tilgang)
    }

    @Test
    fun sporOmNoenAndreEnnSegSelvGirTrueNaarManSporOmEnSomIkkeErSegSelvOgIkkeAnsatt() {
        val innloggetFnr = "7777777"
        val oppslattFnr = "77778888"

        every { brukertilgangConsumer.hasAccessToAnsatt(oppslattFnr) } returns false

        val tilgang =
            tilgangskontrollService.sporOmNoenAndreEnnSegSelvEllerEgneAnsatte(innloggetFnr, oppslattFnr)
        Assertions.assertEquals(true, tilgang)
    }
}
