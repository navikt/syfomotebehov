package no.nav.syfo.motebehov.api.internad.v3

import jakarta.ws.rs.ForbiddenException
import no.nav.syfo.LocalApplication
import no.nav.syfo.testhelper.UserConstants.ARBEIDSTAKER_FNR
import no.nav.syfo.testhelper.UserConstants.VEILEDER_ID
import no.nav.syfo.testhelper.mockSvarFraIstilgangskontrollTilgangTilBruker
import no.nav.syfo.util.TokenValidationUtil
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.web.client.*
import javax.inject.Inject

@ExtendWith(SpringExtension::class)
@SpringBootTest(classes = [LocalApplication::class])
@DirtiesContext
class MotebehovVeilederADTilgangV3Test {

    @Value("\${azure.openid.config.token.endpoint}")
    private lateinit var azureTokenEndpoint: String

    @Value("\${istilgangskontroll.url}")
    private lateinit var tilgangskontrollUrl: String

    @Inject
    private lateinit var motebehovVeilederController: MotebehovVeilederADControllerV3

    @Inject
    @Qualifier("AzureAD")
    private lateinit var restTemplateAzureAD: RestTemplate

    @Inject
    private lateinit var restTemplate: RestTemplate

    @Inject
    private lateinit var tokenValidationUtil: TokenValidationUtil

    private lateinit var mockRestServiceServerAzureAD: MockRestServiceServer
    private lateinit var mockRestServiceServer: MockRestServiceServer

    @BeforeEach
    fun setUp() {
        mockRestServiceServerAzureAD = MockRestServiceServer.bindTo(restTemplateAzureAD).build()
        mockRestServiceServer = MockRestServiceServer.bindTo(restTemplate).build()
    }

    @AfterEach
    fun tearDown() {
        mockRestServiceServer.verify()
        mockRestServiceServerAzureAD.verify()
    }

    @Test
    fun `veileder nektes tilgang`() {
        mockSvarFraIstilgangskontroll(ARBEIDSTAKER_FNR, HttpStatus.FORBIDDEN)
        assertThrows<ForbiddenException> { loggInnOgKallHentMotebehovListe() }
    }

    @Test
    fun `klientfeil mot istilgangskontroll`() {
        mockSvarFraIstilgangskontroll(ARBEIDSTAKER_FNR, HttpStatus.BAD_REQUEST)
        assertThrows<HttpClientErrorException> { loggInnOgKallHentMotebehovListe() }
    }

    @Test
    fun `teknisk feil i istilgangskontroll`() {
        mockSvarFraIstilgangskontroll(ARBEIDSTAKER_FNR, HttpStatus.INTERNAL_SERVER_ERROR)
        assertThrows<HttpServerErrorException> { loggInnOgKallHentMotebehovListe() }
    }

    private fun mockSvarFraIstilgangskontroll(
        fnr: String,
        status: HttpStatus,
    ) {
        mockSvarFraIstilgangskontrollTilgangTilBruker(
            azureTokenEndpoint = azureTokenEndpoint,
            tilgangskontrollUrl = tilgangskontrollUrl,
            mockRestServiceServer = mockRestServiceServer,
            mockRestServiceServerAzureAD = mockRestServiceServerAzureAD,
            status = status,
            fnr = fnr,
        )
    }

    private fun loggInnOgKallHentMotebehovListe() {
        tokenValidationUtil.logInAsNavCounselor(VEILEDER_ID)
        motebehovVeilederController.hentMotebehovListe(ARBEIDSTAKER_FNR)
    }
}
