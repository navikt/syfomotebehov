package no.nav.syfo.consumer.brukertilgang

import io.mockk.every
import io.mockk.mockk
import no.nav.security.oidc.context.OIDCRequestContextHolder
import no.nav.syfo.consumer.aktorregister.domain.Fodselsnummer
import no.nav.syfo.consumer.pdl.PdlConsumer
import no.nav.syfo.testhelper.OidcTestHelper.getValidationContext
import org.junit.jupiter.api.*

class BrukertilgangskontrollServiceTest {
    private val brukertilgangConsumer = mockk<BrukertilgangConsumer>()
    private val oidcRequestContextHolder = mockk<OIDCRequestContextHolder>()
    private var pdlConsumer = mockk<PdlConsumer>()

    private val tilgangskontrollService = BrukertilgangService(
        brukertilgangConsumer = brukertilgangConsumer,
        contextHolder = oidcRequestContextHolder,
        pdlConsumer = pdlConsumer
    )

    @BeforeEach
    fun setup() {
        every { oidcRequestContextHolder.oidcValidationContext }.returns(
            getValidationContext(INNLOGGET_FNR)
        )
    }

    @Test
    fun harTilgangTilOppslaattBrukerGirFalseNaarOppslaattBrukerErKode6() {
        every { brukertilgangConsumer.hasAccessToAnsatt(SPOR_OM_FNR) } returns true
        every { pdlConsumer.isKode6(Fodselsnummer(SPOR_OM_FNR)) } returns true
        val tilgang = tilgangskontrollService.harTilgangTilOppslaattBruker(INNLOGGET_FNR, SPOR_OM_FNR)
        Assertions.assertEquals(false, tilgang)
    }

    @Test
    fun harTilgangTilOppslaattBrukerGirTrueNaarManSporOmSegSelv() {
        every { pdlConsumer.isKode6(Fodselsnummer(INNLOGGET_FNR)) } returns false
        val tilgang = tilgangskontrollService.harTilgangTilOppslaattBruker(INNLOGGET_FNR, INNLOGGET_FNR)
        Assertions.assertEquals(true, tilgang)
    }

    @Test
    fun harTilgangTilOppslaattBrukerGirTrueNaarManSporOmEnAnsatt() {
        every { brukertilgangConsumer.hasAccessToAnsatt(SPOR_OM_FNR) } returns true
        every { pdlConsumer.isKode6(Fodselsnummer(SPOR_OM_FNR)) } returns false
        val tilgang = tilgangskontrollService.harTilgangTilOppslaattBruker(INNLOGGET_FNR, SPOR_OM_FNR)
        Assertions.assertEquals(true, tilgang)
    }

    @Test
    fun harTilgangTilOppslaattBrukerGirFalseNaarManSporOmEnSomIkkeErSegSelvOgIkkeAnsatt() {
        every { brukertilgangConsumer.hasAccessToAnsatt(SPOR_OM_FNR) } returns false
        val tilgang = tilgangskontrollService.harTilgangTilOppslaattBruker(INNLOGGET_FNR, SPOR_OM_FNR)
        Assertions.assertEquals(false, tilgang)
    }

    @Test
    fun sporOmNoenAndreEnnSegSelvGirFalseNaarManSporOmSegSelv() {
        val tilgang = tilgangskontrollService.sporOmNoenAndreEnnSegSelvEllerEgneAnsatte(INNLOGGET_FNR, INNLOGGET_FNR)
        Assertions.assertEquals(false, tilgang)
    }

    @Test
    fun sporOmNoenAndreEnnSegSelvGirFalseNaarManSporOmEnAnsatt() {
        every { brukertilgangConsumer.hasAccessToAnsatt(SPOR_OM_FNR) } returns true
        val tilgang = tilgangskontrollService.sporOmNoenAndreEnnSegSelvEllerEgneAnsatte(INNLOGGET_FNR, SPOR_OM_FNR)
        Assertions.assertEquals(false, tilgang)
    }

    @Test
    fun sporOmNoenAndreEnnSegSelvGirTrueNaarManSporOmEnSomIkkeErSegSelvOgIkkeAnsatt() {
        every { brukertilgangConsumer.hasAccessToAnsatt(SPOR_OM_FNR) } returns false
        val tilgang = tilgangskontrollService.sporOmNoenAndreEnnSegSelvEllerEgneAnsatte(INNLOGGET_FNR, SPOR_OM_FNR)
        Assertions.assertEquals(true, tilgang)
    }

    companion object {
        private const val INNLOGGET_FNR = "15065933818"
        private const val SPOR_OM_FNR = "12345678902"
    }
}
