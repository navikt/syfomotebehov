package no.nav.syfo.testhelper

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.syfo.consumer.azuread.v2.AzureAdV2TokenResponse
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.web.client.ExpectedCount
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers
import org.springframework.test.web.client.response.MockRestResponseCreators

fun mockAndExpectAzureADV2(
    mockRestServiceServer: MockRestServiceServer,
    url: String,
    response: AzureAdV2TokenResponse
) {
    val json = ObjectMapper().writeValueAsString(response)

    mockRestServiceServer.expect(ExpectedCount.once(), MockRestRequestMatchers.requestTo(url))
        .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
        .andRespond(MockRestResponseCreators.withSuccess(json, MediaType.APPLICATION_JSON))
}

fun generateAzureAdV2TokenResponse() = AzureAdV2TokenResponse(
    access_token = "oboToken",
    expires_in = 3600
)
