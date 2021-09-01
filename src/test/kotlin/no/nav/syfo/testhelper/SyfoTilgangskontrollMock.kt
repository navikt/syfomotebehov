package no.nav.syfo.testhelper

import no.nav.syfo.consumer.veiledertilgang.VeilederTilgangConsumer
import no.nav.syfo.util.NAV_PERSONIDENT_HEADER
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
        .path(VeilederTilgangConsumer.TILGANGSKONTROLL_PERSON_PATH)
        .toUriString()
    mockRestServiceServer.expect(ExpectedCount.manyTimes(), MockRestRequestMatchers.requestTo(uriString))
        .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
        .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, "Bearer $oboToken"))
        .andExpect(MockRestRequestMatchers.header(NAV_PERSONIDENT_HEADER, fnr))
        .andRespond(MockRestResponseCreators.withStatus(status))
}
