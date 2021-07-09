package no.nav.syfo.motebehov.api.internad.v1

import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.syfo.LocalApplication
import no.nav.syfo.api.auth.OIDCIssuer.AZURE
import no.nav.syfo.consumer.veiledertilgang.VeilederTilgangConsumer
import no.nav.syfo.testhelper.OidcTestHelper.loggInnVeilederAzure
import no.nav.syfo.testhelper.OidcTestHelper.loggUtAlle
import no.nav.syfo.testhelper.UserConstants.ARBEIDSTAKER_FNR
import no.nav.syfo.testhelper.UserConstants.VEILEDER_ID
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.*
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.client.ExpectedCount
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers
import org.springframework.test.web.client.response.MockRestResponseCreators
import org.springframework.web.client.*
import org.springframework.web.util.UriComponentsBuilder
import java.text.ParseException
import javax.inject.Inject
import javax.ws.rs.ForbiddenException

@ExtendWith(SpringExtension::class)
@SpringBootTest(classes = [LocalApplication::class])
@DirtiesContext
class MotebehovVeilederADTilgangTest {
    @Value("\${tilgangskontrollapi.url}")
    private lateinit var tilgangskontrollUrl: String

    @Inject
    private lateinit var motebehovVeilederController: MotebehovVeilederADController

    @Inject
    private lateinit var contextHolder: TokenValidationContextHolder

    @Inject
    private lateinit var restTemplate: RestTemplate

    private lateinit var mockRestServiceServer: MockRestServiceServer

    @BeforeEach
    fun setUp() {
        mockRestServiceServer = MockRestServiceServer.bindTo(restTemplate).build()
    }

    @AfterEach
    fun tearDown() {
        // Verify all expectations met
        mockRestServiceServer.verify()
        loggUtAlle(contextHolder)
    }

    // Innvilget tilgang testes gjennom @MotebehovVeilederADControllerTest.arbeidsgiverLagrerOgVeilederHenterMotebehov
    @Test
    @Throws(ParseException::class)
    fun veilederNektesTilgang() {
        loggInnVeilederAzure(contextHolder, VEILEDER_ID)
        mockSvarFraSyfoTilgangskontroll(ARBEIDSTAKER_FNR, HttpStatus.FORBIDDEN)
        assertThrows<ForbiddenException> { motebehovVeilederController.hentMotebehovListe(ARBEIDSTAKER_FNR) }
    }

    @Test
    @Throws(ParseException::class)
    fun klientFeilMotTilgangskontroll() {
        loggInnVeilederAzure(contextHolder, VEILEDER_ID)
        mockSvarFraSyfoTilgangskontroll(ARBEIDSTAKER_FNR, HttpStatus.BAD_REQUEST)
        assertThrows<HttpClientErrorException> { motebehovVeilederController.hentMotebehovListe(ARBEIDSTAKER_FNR) }
    }

    @Test
    @Throws(ParseException::class)
    fun tekniskFeilITilgangskontroll() {
        loggInnVeilederAzure(contextHolder, VEILEDER_ID)
        mockSvarFraSyfoTilgangskontroll(ARBEIDSTAKER_FNR, HttpStatus.INTERNAL_SERVER_ERROR)
        assertThrows<HttpServerErrorException> { motebehovVeilederController.hentMotebehovListe(ARBEIDSTAKER_FNR) }
    }

    private fun mockSvarFraSyfoTilgangskontroll(fnr: String, status: HttpStatus) {
        val uriString = UriComponentsBuilder.fromHttpUrl(tilgangskontrollUrl)
            .path(VeilederTilgangConsumer.TILGANG_TIL_BRUKER_VIA_AZURE_PATH)
            .queryParam(VeilederTilgangConsumer.FNR, fnr)
            .toUriString()
        val idToken = contextHolder.tokenValidationContext.getJwtToken(AZURE).tokenAsString
        mockRestServiceServer.expect(ExpectedCount.manyTimes(), MockRestRequestMatchers.requestTo(uriString))
            .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
            .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, "Bearer $idToken"))
            .andRespond(MockRestResponseCreators.withStatus(status))
    }
}
