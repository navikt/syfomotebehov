package no.nav.syfo.motebehov.api

import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.extensions.ApplyExtension
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.syfo.IntegrationTest
import no.nav.syfo.LocalApplication
import no.nav.syfo.consumer.pdl.PdlConsumer
import no.nav.syfo.consumer.veiledertilgang.IVeilederTilgangConsumer
import no.nav.syfo.motebehov.database.MotebehovDAO
import no.nav.syfo.oppfolgingstilfelle.database.OppfolgingstilfelleDAO
import no.nav.syfo.testhelper.UserConstants.ARBEIDSTAKER_AKTORID
import no.nav.syfo.testhelper.UserConstants.ARBEIDSTAKER_FNR
import no.nav.syfo.testhelper.UserConstants.VEILEDER_ID
import no.nav.syfo.util.NAV_PERSONIDENT_HEADER
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

@SpringBootTest(
    classes = [LocalApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
)
@ApplyExtension(SpringExtension::class)
class BearerAuthHttpIntegrationTest : IntegrationTest() {
    @Value("\${local.server.port}")
    private var localServerPort: Int = 0

    @Value("\${server.servlet.context-path}")
    private lateinit var contextPath: String

    @Autowired
    private lateinit var mockOAuth2Server: MockOAuth2Server

    @Autowired
    private lateinit var motebehovDAO: MotebehovDAO

    @Autowired
    private lateinit var oppfolgingstilfelleDAO: OppfolgingstilfelleDAO

    @MockkBean
    private lateinit var pdlConsumer: PdlConsumer

    @MockkBean
    private lateinit var veilederTilgangConsumer: IVeilederTilgangConsumer

    private val httpClient = HttpClient.newHttpClient()

    init {
        beforeTest {
            cleanDb()
            every { pdlConsumer.aktorid(any()) } returns ARBEIDSTAKER_AKTORID
            every { veilederTilgangConsumer.sjekkVeiledersTilgangTilPersonMedOBO(ARBEIDSTAKER_FNR) } returns true
        }

        afterTest {
            cleanDb()
        }

        describe("Bearer auth through HTTP layer") {
            it("accepts bearer token for tokenx endpoint") {
                val response =
                    get(
                        path = "/api/v4/arbeidstaker/motebehov",
                        headers =
                            mapOf(
                                HttpHeaders.AUTHORIZATION to "Bearer ${tokenXBearerToken()}",
                            ),
                    )

                response.statusCode() shouldBe 200
                response.body() shouldContain "\"visMotebehov\":false"
            }

            it("accepts bearer token for veileder endpoint") {
                val response =
                    get(
                        path = "/api/internad/v4/veileder/historikk",
                        headers =
                            mapOf(
                                HttpHeaders.AUTHORIZATION to "Bearer ${internAzureAdBearerToken()}",
                                NAV_PERSONIDENT_HEADER to ARBEIDSTAKER_FNR,
                            ),
                    )

                response.statusCode() shouldBe 200
                response.body() shouldBe "[]"
            }

            it("rejects cookie-only auth for veileder endpoint") {
                val response =
                    get(
                        path = "/api/internad/v4/veileder/historikk",
                        headers =
                            mapOf(
                                HttpHeaders.COOKIE to "localhost-idtoken=${internAzureAdBearerToken()}",
                                NAV_PERSONIDENT_HEADER to ARBEIDSTAKER_FNR,
                            ),
                    )

                response.statusCode() shouldBe 401
            }
        }
    }

    private fun get(
        path: String,
        headers: Map<String, String>,
    ): HttpResponse<String> {
        val requestBuilder =
            HttpRequest
                .newBuilder()
                .uri(URI.create("http://localhost:$localServerPort$contextPath$path"))
                .GET()

        headers.forEach { (name, value) ->
            requestBuilder.header(name, value)
        }

        return httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString())
    }

    private fun tokenXBearerToken(): String =
        mockOAuth2Server
            .issueToken(
                issuerId = "tokenx",
                subject = "dialogmote-frontend",
                audience = "clientID",
                claims =
                    mapOf(
                        "acr" to "idporten-loa-high",
                        "pid" to ARBEIDSTAKER_FNR,
                        "client_id" to "dialogmote-frontend",
                    ),
            ).serialize()

    private fun internAzureAdBearerToken(): String =
        mockOAuth2Server
            .issueToken(
                issuerId = "internazureadv2",
                subject = "modiasyfoperson",
                audience = "clientID",
                claims =
                    mapOf(
                        "NAVident" to VEILEDER_ID,
                    ),
            ).serialize()

    private fun cleanDb() {
        motebehovDAO.nullstillMotebehov(ARBEIDSTAKER_AKTORID)
        oppfolgingstilfelleDAO.nullstillOppfolgingstilfeller(ARBEIDSTAKER_FNR)
    }
}
