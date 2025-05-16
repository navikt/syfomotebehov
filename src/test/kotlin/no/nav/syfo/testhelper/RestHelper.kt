package no.nav.syfo.testhelper

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.syfo.consumer.behandlendeenhet.BehandlendeEnhet
import no.nav.syfo.consumer.behandlendeenhet.BehandlendeEnhetConsumer.Companion.BEHANDLENDEENHET_PATH
import no.nav.syfo.consumer.behandlendeenhet.EnhetDTO
import no.nav.syfo.util.NAV_PERSONIDENT_HEADER
import no.nav.syfo.util.bearerCredentials
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.client.ExpectedCount
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers
import org.springframework.test.web.client.response.MockRestResponseCreators
import org.springframework.web.util.UriComponentsBuilder

fun mockAndExpectBehandlendeEnhetRequest(
    azureTokenEndpoint: String,
    mockRestServiceServerAzureAD: MockRestServiceServer,
    mockRestServiceServer: MockRestServiceServer,
    behandlendeenhetUrl: String,
    fnr: String,
) {
    val uriString = UriComponentsBuilder.fromUriString(behandlendeenhetUrl)
        .path(BEHANDLENDEENHET_PATH)
        .toUriString()
    val behandlendeEnhet = BehandlendeEnhet(
        geografiskEnhet = EnhetDTO(UserConstants.NAV_ENHET, UserConstants.NAV_ENHET_NAVN),
        oppfolgingsenhetDTO = null,
    )

    val systemToken = generateAzureAdV2TokenResponse()

    mockAndExpectAzureADV2(mockRestServiceServerAzureAD, azureTokenEndpoint, systemToken)

    try {
        val json = ObjectMapper().writeValueAsString(behandlendeEnhet)

        mockRestServiceServer.expect(ExpectedCount.once(), MockRestRequestMatchers.requestTo(uriString))
            .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
            .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, bearerCredentials(systemToken.access_token)))
            .andExpect(MockRestRequestMatchers.header(NAV_PERSONIDENT_HEADER, fnr))
            .andRespond(MockRestResponseCreators.withSuccess(json, MediaType.APPLICATION_JSON))
    } catch (e: JsonProcessingException) {
        e.printStackTrace()
    }
}

fun mockAndExpectBehandlendeEnhetRequestWithTilgangskontroll(
    azureTokenEndpoint: String,
    mockRestServiceServerAzureAD: MockRestServiceServer,
    mockRestServiceServer: MockRestServiceServer,
    behandlendeenhetUrl: String,
    tilgangskontrollUrl: String,
    fnr: String,
) {
    val uriString = UriComponentsBuilder.fromUriString(behandlendeenhetUrl)
        .path(BEHANDLENDEENHET_PATH)
        .toUriString()
    val behandlendeEnhet = BehandlendeEnhet(
        geografiskEnhet = EnhetDTO(UserConstants.NAV_ENHET, UserConstants.NAV_ENHET_NAVN),
        oppfolgingsenhetDTO = null,
    )

    val systemToken = generateAzureAdV2TokenResponse()

    mockAndExpectAzureADV2(mockRestServiceServerAzureAD, azureTokenEndpoint, systemToken)
    mockSvarFraIstilgangskontrollTilgangTilBruker(
        azureTokenEndpoint,
        tilgangskontrollUrl,
        mockRestServiceServerAzureAD,
        mockRestServiceServer,
        fnr,
        HttpStatus.OK,
    )

    try {
        val json = ObjectMapper().writeValueAsString(behandlendeEnhet)

        mockRestServiceServer.expect(ExpectedCount.manyTimes(), MockRestRequestMatchers.requestTo(uriString))
            .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
            .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, bearerCredentials(systemToken.access_token)))
            .andExpect(MockRestRequestMatchers.header(NAV_PERSONIDENT_HEADER, fnr))
            .andRespond(MockRestResponseCreators.withSuccess(json, MediaType.APPLICATION_JSON))
    } catch (e: JsonProcessingException) {
        e.printStackTrace()
    }
}
