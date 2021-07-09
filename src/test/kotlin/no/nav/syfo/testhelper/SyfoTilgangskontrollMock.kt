package no.nav.syfo.testhelper

import no.nav.syfo.consumer.veiledertilgang.VeilederTilgangConsumer
import org.springframework.http.*
import org.springframework.test.web.client.ExpectedCount
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers
import org.springframework.test.web.client.response.MockRestResponseCreators
import org.springframework.web.util.UriComponentsBuilder

fun mockSvarFraSyfoTilgangskontrollV2TilgangTilBruker(
    azureTokenEndpoint: String,
    tilgangskontrollUrl: String,
    mockRestServiceWithProxyServer: MockRestServiceServer,
    mockRestServiceServer: MockRestServiceServer,
    fnr: String,
    status: HttpStatus
) {
    mockAndExpectAzureADV2(mockRestServiceWithProxyServer, azureTokenEndpoint, generateAzureAdV2TokenResponse())

    val oboToken = generateAzureAdV2TokenResponse().access_token

    val uriString = UriComponentsBuilder.fromHttpUrl(tilgangskontrollUrl)
        .path(VeilederTilgangConsumer.TILGANG_TIL_BRUKER_VIA_AZURE_V2_PATH)
        .path("/")
        .path(fnr)
        .toUriString()
    mockRestServiceServer.expect(ExpectedCount.manyTimes(), MockRestRequestMatchers.requestTo(uriString))
        .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
        .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, "Bearer $oboToken"))
        .andRespond(MockRestResponseCreators.withStatus(status))
}
