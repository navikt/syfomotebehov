package no.nav.syfo.motebehov.api.internad.v3

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.extensions.spring.SpringExtension
import jakarta.ws.rs.ForbiddenException
import no.nav.syfo.IntegrationTest
import no.nav.syfo.LocalApplication
import no.nav.syfo.testhelper.UserConstants.ARBEIDSTAKER_FNR
import no.nav.syfo.testhelper.UserConstants.VEILEDER_ID
import no.nav.syfo.testhelper.mockSvarFraIstilgangskontrollTilgangTilBruker
import no.nav.syfo.util.TokenValidationUtil
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.http.HttpStatus
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.RestTemplate

@TestConfiguration
@SpringBootTest(classes = [LocalApplication::class])
class MotebehovVeilederADTilgangV3Test : IntegrationTest() {

    @Value("\${azure.openid.config.token.endpoint}")
    private lateinit var azureTokenEndpoint: String

    @Value("\${istilgangskontroll.url}")
    private lateinit var tilgangskontrollUrl: String

    @Autowired
    private lateinit var motebehovVeilederController: MotebehovVeilederADControllerV3

    @Autowired
    @Qualifier("AzureAD")
    private lateinit var restTemplateAzureAD: RestTemplate

    @Autowired
    private lateinit var restTemplate: RestTemplate

    @Autowired
    private lateinit var tokenValidationUtil: TokenValidationUtil

    private lateinit var mockRestServiceServerAzureAD: MockRestServiceServer
    private lateinit var mockRestServiceServer: MockRestServiceServer

    init {
        extensions(SpringExtension)
        beforeTest {
            mockRestServiceServerAzureAD = MockRestServiceServer.bindTo(restTemplateAzureAD).build()
            mockRestServiceServer = MockRestServiceServer.bindTo(restTemplate).build()
        }

        afterTest {
            mockRestServiceServer.verify()
            mockRestServiceServerAzureAD.verify()
            tokenValidationUtil.resetAll()
        }

        describe("MotebehovVeilederADTilgangV3") {
            it("veileder nektes tilgang") {
                mockSvarFraIstilgangskontroll(ARBEIDSTAKER_FNR, HttpStatus.FORBIDDEN)
                shouldThrow<ForbiddenException> { loggInnOgKallHentMotebehovListe() }
            }

            it("klientfeil mot istilgangskontroll") {
                mockSvarFraIstilgangskontroll(ARBEIDSTAKER_FNR, HttpStatus.BAD_REQUEST)
                shouldThrow<HttpClientErrorException> { loggInnOgKallHentMotebehovListe() }
            }

            it("teknisk feil i istilgangskontroll") {
                mockSvarFraIstilgangskontroll(ARBEIDSTAKER_FNR, HttpStatus.INTERNAL_SERVER_ERROR)
                shouldThrow<HttpServerErrorException> { loggInnOgKallHentMotebehovListe() }
            }
        }
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
