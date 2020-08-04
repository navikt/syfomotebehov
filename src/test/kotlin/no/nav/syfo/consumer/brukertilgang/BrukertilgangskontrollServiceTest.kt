package no.nav.syfo.consumer.brukertilgang

import no.nav.security.oidc.context.*
import no.nav.security.oidc.test.support.JwtTokenGenerator
import no.nav.syfo.api.auth.OIDCIssuer.EKSTERN
import no.nav.syfo.consumer.aktorregister.domain.Fodselsnummer
import no.nav.syfo.consumer.pdl.PdlConsumer
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.*
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class BrukertilgangskontrollServiceTest {
    @Mock
    private lateinit var oidcRequestContextHolder: OIDCRequestContextHolder

    @Mock
    private lateinit var brukertilgangConsumer: BrukertilgangConsumer

    @Mock
    private lateinit var pdlConsumer: PdlConsumer

    @InjectMocks
    private lateinit var tilgangskontrollService: BrukertilgangService

    @BeforeEach
    fun setup() {
        mockOIDC(INNLOGGET_FNR)
        Mockito.`when`(pdlConsumer.isKode6(Fodselsnummer(SPOR_OM_FNR))).thenReturn(false)
    }

    @AfterEach
    fun tearDown() {
        oidcRequestContextHolder.oidcValidationContext = null
    }

    @Test
    fun harTilgangTilOppslaattBrukerGirFalseNaarOppslaattBrukerErKode6() {
        Mockito.`when`(brukertilgangConsumer.hasAccessToAnsatt(SPOR_OM_FNR)).thenReturn(true)
        Mockito.`when`(pdlConsumer.isKode6(Fodselsnummer(SPOR_OM_FNR))).thenReturn(true)
        val tilgang = tilgangskontrollService.harTilgangTilOppslaattBruker(INNLOGGET_FNR, SPOR_OM_FNR)
        Assertions.assertEquals(false, tilgang)
    }

    @Test
    fun harTilgangTilOppslaattBrukerGirTrueNaarManSporOmSegSelv() {
        val tilgang = tilgangskontrollService.harTilgangTilOppslaattBruker(INNLOGGET_FNR, INNLOGGET_FNR)
        Assertions.assertEquals(true, tilgang)
    }

    @Test
    fun harTilgangTilOppslaattBrukerGirTrueNaarManSporOmEnAnsatt() {
        Mockito.`when`(brukertilgangConsumer.hasAccessToAnsatt(SPOR_OM_FNR)).thenReturn(true)
        val tilgang = tilgangskontrollService.harTilgangTilOppslaattBruker(INNLOGGET_FNR, SPOR_OM_FNR)
        Assertions.assertEquals(true, tilgang)
    }

    @Test
    fun harTilgangTilOppslaattBrukerGirFalseNaarManSporOmEnSomIkkeErSegSelvOgIkkeAnsatt() {
        Mockito.`when`(brukertilgangConsumer.hasAccessToAnsatt(SPOR_OM_FNR)).thenReturn(false)
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
        Mockito.`when`(brukertilgangConsumer.hasAccessToAnsatt(SPOR_OM_FNR)).thenReturn(true)
        val tilgang = tilgangskontrollService.sporOmNoenAndreEnnSegSelvEllerEgneAnsatte(INNLOGGET_FNR, SPOR_OM_FNR)
        Assertions.assertEquals(false, tilgang)
    }

    @Test
    fun sporOmNoenAndreEnnSegSelvGirTrueNaarManSporOmEnSomIkkeErSegSelvOgIkkeAnsatt() {
        Mockito.`when`(brukertilgangConsumer.hasAccessToAnsatt(SPOR_OM_FNR)).thenReturn(false)
        val tilgang = tilgangskontrollService.sporOmNoenAndreEnnSegSelvEllerEgneAnsatte(INNLOGGET_FNR, SPOR_OM_FNR)
        Assertions.assertEquals(true, tilgang)
    }

    private fun mockOIDC(subject: String) {
        val jwt = JwtTokenGenerator.createSignedJWT(subject)
        val issuer: String = EKSTERN
        val tokenContext = TokenContext(issuer, jwt.serialize())
        val oidcClaims = OIDCClaims(jwt)
        val oidcValidationContext = OIDCValidationContext()
        oidcValidationContext.addValidatedToken(issuer, tokenContext, oidcClaims)
        oidcRequestContextHolder.oidcValidationContext = oidcValidationContext
    }

    companion object {
        private const val INNLOGGET_FNR = "15065933818"
        private const val SPOR_OM_FNR = "12345678902"
    }
}
