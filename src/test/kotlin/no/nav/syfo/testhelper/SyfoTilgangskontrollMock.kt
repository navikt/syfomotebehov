package no.nav.syfo.testhelper

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.syfo.consumer.veiledertilgang.Tilgang
import no.nav.syfo.consumer.veiledertilgang.VeilederTilgangConsumer
import no.nav.syfo.util.NAV_PERSONIDENT_HEADER
import org.springframework.http.*
import org.springframework.test.web.client.ExpectedCount
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.ResponseCreator
import org.springframework.test.web.client.match.MockRestRequestMatchers
import org.springframework.test.web.client.response.MockRestResponseCreators
import org.springframework.web.util.UriComponentsBuilder

fun mockSvarFraIstilgangskontrollTilgangTilBruker(
    azureTokenEndpoint: String,
    tilgangskontrollUrl: String,
    mockRestServiceServerAzureAD: MockRestServiceServer,
    mockRestServiceServer: MockRestServiceServer,
    fnr: String,
    status: HttpStatus,
) {
    mockAndExpectAzureADV2(mockRestServiceServerAzureAD, azureTokenEndpoint, generateAzureAdV2TokenResponse())

    val oboToken = generateAzureAdV2TokenResponse().access_token

    val uriString = UriComponentsBuilder.fromHttpUrl(tilgangskontrollUrl)
        .path(VeilederTilgangConsumer.TILGANGSKONTROLL_PERSON_PATH)
        .toUriString()
    mockRestServiceServer.expect(ExpectedCount.manyTimes(), MockRestRequestMatchers.requestTo(uriString))
        .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
        .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, "Bearer $oboToken"))
        .andExpect(MockRestRequestMatchers.header(NAV_PERSONIDENT_HEADER, fnr))
        .andRespond(response(status))
}

private fun response(status: HttpStatus): ResponseCreator {
    return if (status == HttpStatus.OK) {
        MockRestResponseCreators.withSuccess(tilgangAsJsonString(), MediaType.APPLICATION_JSON)
    } else {
        MockRestResponseCreators.withStatus(
            status,
        )
    }
}

private fun tilgangAsJsonString(): String {
    val objectMapper = ObjectMapper()
    val tilgang = Tilgang(erGodkjent = true)
    return try {
        objectMapper.writeValueAsString(tilgang)
    } catch (e: JsonProcessingException) {
        throw RuntimeException(e)
    }
}
