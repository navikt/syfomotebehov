package no.nav.syfo.motebehov.api.internad.v2

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
import java.text.ParseException
import javax.inject.Inject

@ExtendWith(SpringExtension::class)
@SpringBootTest(classes = [LocalApplication::class])
@DirtiesContext
class MotebehovVeilederADTilgangV2Test {

    @Value("\${azure.openid.config.token.endpoint}")
    private lateinit var azureTokenEndpoint: String

    @Value("\${istilgangskontroll.url}")
    private lateinit var tilgangskontrollUrl: String

    @Inject
    private lateinit var motebehovVeilederController: MotebehovVeilederADControllerV2

    @Inject
    private lateinit var restTemplate: RestTemplate

    @Inject
    private lateinit var tokenValidationUtil: TokenValidationUtil

    private lateinit var mockRestServiceServer: MockRestServiceServer

    @Inject
    @Qualifier("restTemplateWithProxy")
    private lateinit var restTemplateWithProxy: RestTemplate
    private lateinit var mockRestServiceWithProxyServer: MockRestServiceServer

    @BeforeEach
    fun setUp() {
        mockRestServiceServer = MockRestServiceServer.bindTo(restTemplate).build()
        mockRestServiceWithProxyServer = MockRestServiceServer.bindTo(restTemplateWithProxy).build()
    }

    @AfterEach
    fun tearDown() {
        mockRestServiceServer.verify()
        mockRestServiceWithProxyServer.verify()
    }

    @Test
    @Throws(ParseException::class)
    fun `veileder nektes tilgang`() {
        mockSvarFraIstilgangskontroll(ARBEIDSTAKER_FNR, HttpStatus.FORBIDDEN)
        assertThrows<ForbiddenException> { kallHentMotebehovListe() }
    }

    @Test
    @Throws(ParseException::class)
    fun `klientfeil mot istilgangskontroll`() {
        mockSvarFraIstilgangskontroll(ARBEIDSTAKER_FNR, HttpStatus.BAD_REQUEST)
        assertThrows<HttpClientErrorException> { kallHentMotebehovListe() }
    }

    @Test
    @Throws(ParseException::class)
    fun `teknisk feil i istilgangskontroll`() {
        mockSvarFraIstilgangskontroll(ARBEIDSTAKER_FNR, HttpStatus.INTERNAL_SERVER_ERROR)
        assertThrows<HttpServerErrorException> { kallHentMotebehovListe() }
    }

    private fun mockSvarFraIstilgangskontroll(
        fnr: String,
        status: HttpStatus,
    ) {
        mockSvarFraIstilgangskontrollTilgangTilBruker(
            azureTokenEndpoint = azureTokenEndpoint,
            tilgangskontrollUrl = tilgangskontrollUrl,
            mockRestServiceServer = mockRestServiceServer,
            mockRestServiceWithProxyServer = mockRestServiceWithProxyServer,
            status = status,
            fnr = fnr,
        )
    }

    private fun kallHentMotebehovListe() {
        tokenValidationUtil.logInAsNavCounselor(VEILEDER_ID)
        motebehovVeilederController.hentMotebehovListe(ARBEIDSTAKER_FNR)
    }
}
