package no.nav.syfo.consumer.brukertilgang

import no.nav.security.oidc.context.OIDCClaims
import no.nav.security.oidc.context.OIDCRequestContextHolder
import no.nav.security.oidc.context.OIDCValidationContext
import no.nav.security.oidc.context.TokenContext
import no.nav.security.oidc.test.support.JwtTokenGenerator
import no.nav.syfo.consumer.aktorregister.domain.Fodselsnummer
import no.nav.syfo.api.auth.OIDCIssuer.EKSTERN
import no.nav.syfo.consumer.pdl.PdlConsumer
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class BrukertilgangskontrollServiceTest {
    @Mock
    private lateinit var oidcRequestContextHolder: OIDCRequestContextHolder

    @Mock
    private lateinit var brukertilgangConsumer: BrukertilgangConsumer

    @Mock
    private lateinit var pdlConsumer: PdlConsumer

    @InjectMocks
    private lateinit var tilgangskontrollService: BrukertilgangService

    @Before
    fun setup() {
        mockOIDC(INNLOGGET_FNR)
        Mockito.`when`(pdlConsumer.isKode6(Fodselsnummer(SPOR_OM_FNR))).thenReturn(false)
    }

    @After
    fun tearDown() {
        oidcRequestContextHolder.oidcValidationContext = null
    }

    @Test
    fun harTilgangTilOppslaattBrukerGirFalseNaarOppslaattBrukerErKode6() {
        Mockito.`when`(brukertilgangConsumer.hasAccessToAnsatt(SPOR_OM_FNR)).thenReturn(true)
        Mockito.`when`(pdlConsumer.isKode6(Fodselsnummer(SPOR_OM_FNR))).thenReturn(true)
        val tilgang = tilgangskontrollService.harTilgangTilOppslaattBruker(INNLOGGET_FNR, SPOR_OM_FNR)
        Assert.assertEquals(false, tilgang)
    }

    @Test
    fun harTilgangTilOppslaattBrukerGirTrueNaarManSporOmSegSelv() {
        val tilgang = tilgangskontrollService.harTilgangTilOppslaattBruker(INNLOGGET_FNR, INNLOGGET_FNR)
        Assert.assertEquals(true, tilgang)
    }

    @Test
    fun harTilgangTilOppslaattBrukerGirTrueNaarManSporOmEnAnsatt() {
        Mockito.`when`(brukertilgangConsumer.hasAccessToAnsatt(SPOR_OM_FNR)).thenReturn(true)
        val tilgang = tilgangskontrollService.harTilgangTilOppslaattBruker(INNLOGGET_FNR, SPOR_OM_FNR)
        Assert.assertEquals(true, tilgang)
    }

    @Test
    fun harTilgangTilOppslaattBrukerGirFalseNaarManSporOmEnSomIkkeErSegSelvOgIkkeAnsatt() {
        Mockito.`when`(brukertilgangConsumer.hasAccessToAnsatt(SPOR_OM_FNR)).thenReturn(false)
        val tilgang = tilgangskontrollService.harTilgangTilOppslaattBruker(INNLOGGET_FNR, SPOR_OM_FNR)
        Assert.assertEquals(false, tilgang)
    }

    @Test
    fun sporOmNoenAndreEnnSegSelvGirFalseNaarManSporOmSegSelv() {
        val tilgang = tilgangskontrollService.sporOmNoenAndreEnnSegSelvEllerEgneAnsatte(INNLOGGET_FNR, INNLOGGET_FNR)
        Assert.assertEquals(false, tilgang)
    }

    @Test
    fun sporOmNoenAndreEnnSegSelvGirFalseNaarManSporOmEnAnsatt() {
        Mockito.`when`(brukertilgangConsumer.hasAccessToAnsatt(SPOR_OM_FNR)).thenReturn(true)
        val tilgang = tilgangskontrollService.sporOmNoenAndreEnnSegSelvEllerEgneAnsatte(INNLOGGET_FNR, SPOR_OM_FNR)
        Assert.assertEquals(false, tilgang)
    }

    @Test
    fun sporOmNoenAndreEnnSegSelvGirTrueNaarManSporOmEnSomIkkeErSegSelvOgIkkeAnsatt() {
        Mockito.`when`(brukertilgangConsumer.hasAccessToAnsatt(SPOR_OM_FNR)).thenReturn(false)
        val tilgang = tilgangskontrollService.sporOmNoenAndreEnnSegSelvEllerEgneAnsatte(INNLOGGET_FNR, SPOR_OM_FNR)
        Assert.assertEquals(true, tilgang)
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
