package no.nav.syfo.api

import no.nav.security.oidc.context.OIDCRequestContextHolder
import no.nav.syfo.LocalApplication
import no.nav.syfo.oidc.OIDCIssuer.AZURE
import no.nav.syfo.testhelper.OidcTestHelper.loggInnVeilederAzure
import no.nav.syfo.testhelper.OidcTestHelper.loggUtAlle
import no.nav.syfo.testhelper.UserConstants.ARBEIDSTAKER_FNR
import no.nav.syfo.testhelper.UserConstants.VEILEDER_ID
import no.nav.syfo.veiledertilgang.VeilederTilgangConsumer
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.*
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.test.web.client.ExpectedCount
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers
import org.springframework.test.web.client.response.MockRestResponseCreators
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.text.ParseException
import javax.inject.Inject
import javax.ws.rs.ForbiddenException

@RunWith(SpringRunner::class)
@SpringBootTest(classes = [LocalApplication::class])
@DirtiesContext
class MotebehovVeilederADTilgangTest {
    @Value("\${tilgangskontrollapi.url}")
    private lateinit var tilgangskontrollUrl: String

    @Inject
    private lateinit var motebehovVeilederController: MotebehovVeilederADController

    @Inject
    private lateinit var oidcRequestContextHolder: OIDCRequestContextHolder

    @Inject
    private lateinit var restTemplate: RestTemplate

    private lateinit var mockRestServiceServer: MockRestServiceServer

    @Before
    fun setUp() {
        mockRestServiceServer = MockRestServiceServer.bindTo(restTemplate).build()
    }

    @After
    fun tearDown() {
        // Verify all expectations met
        mockRestServiceServer.verify()
        loggUtAlle(oidcRequestContextHolder)
    }

    // Innvilget tilgang testes gjennom @MotebehovVeilederADControllerTest.arbeidsgiverLagrerOgVeilederHenterMotebehov
    @Test(expected = ForbiddenException::class)
    @Throws(ParseException::class)
    fun veilederNektesTilgang() {
        loggInnVeilederAzure(oidcRequestContextHolder, VEILEDER_ID)
        mockSvarFraSyfoTilgangskontroll(ARBEIDSTAKER_FNR, HttpStatus.FORBIDDEN)
        motebehovVeilederController.hentMotebehovListe(ARBEIDSTAKER_FNR)
    }

    @Test(expected = HttpClientErrorException::class)
    @Throws(ParseException::class)
    fun klientFeilMotTilgangskontroll() {
        loggInnVeilederAzure(oidcRequestContextHolder, VEILEDER_ID)
        mockSvarFraSyfoTilgangskontroll(ARBEIDSTAKER_FNR, HttpStatus.BAD_REQUEST)
        motebehovVeilederController.hentMotebehovListe(ARBEIDSTAKER_FNR)
    }

    @Test(expected = HttpServerErrorException::class)
    @Throws(ParseException::class)
    fun tekniskFeilITilgangskontroll() {
        loggInnVeilederAzure(oidcRequestContextHolder, VEILEDER_ID)
        mockSvarFraSyfoTilgangskontroll(ARBEIDSTAKER_FNR, HttpStatus.INTERNAL_SERVER_ERROR)
        motebehovVeilederController.hentMotebehovListe(ARBEIDSTAKER_FNR)
    }

    private fun mockSvarFraSyfoTilgangskontroll(fnr: String, status: HttpStatus) {
        val uriString = UriComponentsBuilder.fromHttpUrl(tilgangskontrollUrl)
                .path(VeilederTilgangConsumer.TILGANG_TIL_BRUKER_VIA_AZURE_PATH)
                .queryParam(VeilederTilgangConsumer.FNR, fnr)
                .toUriString()
        val idToken = oidcRequestContextHolder.oidcValidationContext.getToken(AZURE).idToken
        mockRestServiceServer.expect(ExpectedCount.manyTimes(), MockRestRequestMatchers.requestTo(uriString))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, "Bearer $idToken"))
                .andRespond(MockRestResponseCreators.withStatus(status))
    }
}
