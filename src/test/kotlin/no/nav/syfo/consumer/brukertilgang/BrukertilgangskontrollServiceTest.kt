package no.nav.syfo.consumer.brukertilgang

import no.nav.security.token.support.core.context.TokenValidationContext
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.security.token.support.core.jwt.JwtToken
import no.nav.security.token.support.test.JwtTokenGenerator
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
    private lateinit var contextHolder: TokenValidationContextHolder

    @Mock
    private lateinit var brukertilgangConsumer: BrukertilgangConsumer

    @Mock
    private lateinit var pdlConsumer: PdlConsumer

    @InjectMocks
    private lateinit var tilgangskontrollService: BrukertilgangService

    @BeforeEach
    fun setup() {
        mockOIDC(INNLOGGET_FNR)
    }

    @AfterEach
    fun tearDown() {
        contextHolder.tokenValidationContext = null
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
        val jwt = JwtToken(JwtTokenGenerator.createSignedJWT(subject).serialize())
        val issuer = EKSTERN
        val issuerTokenMap = HashMap<String, JwtToken>()
        issuerTokenMap[issuer] = jwt
        val context = TokenValidationContext(issuerTokenMap)
        contextHolder.tokenValidationContext = context
    }

    companion object {
        private const val INNLOGGET_FNR = "15065933818"
        private const val SPOR_OM_FNR = "12345678902"
    }
}
