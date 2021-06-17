package no.nav.syfo.testhelper

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import no.nav.syfo.consumer.behandlendeenhet.BehandlendeEnhet
import no.nav.syfo.consumer.veiledertilgang.VeilederTilgangConsumer
import no.nav.syfo.testhelper.UserConstants.STS_TOKEN
import no.nav.syfo.testhelper.generator.generateStsToken
import no.nav.syfo.util.basicCredentials
import no.nav.syfo.util.bearerCredentials
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.*
import org.springframework.test.web.client.ExpectedCount
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers
import org.springframework.test.web.client.response.MockRestResponseCreators
import org.springframework.web.util.UriComponentsBuilder

private val objectMapper = ObjectMapper()
    .registerModule(JavaTimeModule())
    .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)

@Value("\${syfomoteadmin.url}")
private lateinit var syfomoteadminUrl: String

fun mockAndExpectBrukertilgangRequest(mockRestServiceServer: MockRestServiceServer, brukertilgangUrl: String, ansattFnr: String) {
    val uriString = UriComponentsBuilder.fromHttpUrl(brukertilgangUrl)
        .path("/api/v1/tilgang/ansatt/")
        .path(ansattFnr)
        .toUriString()
    try {
        val json = ObjectMapper().writeValueAsString(true)

        mockRestServiceServer.expect(ExpectedCount.manyTimes(), MockRestRequestMatchers.requestTo(uriString))
            .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
            .andRespond(MockRestResponseCreators.withSuccess(json, MediaType.APPLICATION_JSON))
    } catch (e: JsonProcessingException) {
        e.printStackTrace()
    }
}

fun mockAndExpectBehandlendeEnhetRequest(mockRestServiceServer: MockRestServiceServer, behandlendeenhetUrl: String, fnr: String) {
    val uriString = UriComponentsBuilder.fromHttpUrl(behandlendeenhetUrl)
        .path("/api/")
        .path(fnr)
        .toUriString()
    val behandlendeEnhet = BehandlendeEnhet(
        UserConstants.NAV_ENHET,
        UserConstants.NAV_ENHET_NAVN
    )
    try {
        val json = ObjectMapper().writeValueAsString(behandlendeEnhet)

        mockRestServiceServer.expect(ExpectedCount.manyTimes(), MockRestRequestMatchers.requestTo(uriString))
            .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
            .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, bearerCredentials(STS_TOKEN)))
            .andRespond(MockRestResponseCreators.withSuccess(json, MediaType.APPLICATION_JSON))
    } catch (e: JsonProcessingException) {
        e.printStackTrace()
    }
}

fun mockAndExpectMoteadminHarAktivtMote(
    mockRestServiceServer: MockRestServiceServer,
    harAktivtMote: Boolean
) {
    val svarFraSyfomoteadminJson = objectMapper.writeValueAsString(harAktivtMote)
    val url = UriComponentsBuilder.fromHttpUrl(syfomoteadminUrl)
        .pathSegment("system", UserConstants.ARBEIDSTAKER_AKTORID, "harAktivtMote")
        .toUriString()
    mockRestServiceServer.expect(ExpectedCount.once(), MockRestRequestMatchers.requestTo(url))
        .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
        .andRespond(MockRestResponseCreators.withSuccess(svarFraSyfomoteadminJson, MediaType.APPLICATION_JSON))
}

fun mockAndExpectMoteadminIsMoteplanleggerActive(
    mockRestServiceServer: MockRestServiceServer,
    isMoteplanleggerActive: Boolean
) {
    val responseJson = objectMapper.writeValueAsString(isMoteplanleggerActive)
    val url = UriComponentsBuilder.fromHttpUrl(syfomoteadminUrl)
        .path("/system/moteplanlegger/aktiv")
        .toUriString()
    mockRestServiceServer.expect(ExpectedCount.once(), MockRestRequestMatchers.requestTo(url))
        .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
        .andRespond(MockRestResponseCreators.withSuccess(responseJson, MediaType.APPLICATION_JSON))
}

fun mockAndExpectSTSService(
    mockRestServiceServer: MockRestServiceServer,
    stsUrl: String,
    username: String,
    password: String
) {
    val uriString = UriComponentsBuilder.fromHttpUrl(stsUrl)
        .path("/rest/v1/sts/token")
        .queryParam("grant_type", "client_credentials")
        .queryParam("scope", "openid")
        .toUriString()

    val stsToken = generateStsToken()

    val json = "{ \"access_token\" : \"$STS_TOKEN\", \"token_type\" : \"${stsToken.token_type}\", \"expires_in\" : \"${stsToken.expires_in}\" }"
    mockRestServiceServer.expect(ExpectedCount.manyTimes(), MockRestRequestMatchers.requestTo(uriString))
        .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
        .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, basicCredentials(username, password)))
        .andRespond(MockRestResponseCreators.withSuccess(json, MediaType.APPLICATION_JSON))
}

fun mockAndExpectSyfoTilgangskontroll(
    mockRestServiceServer: MockRestServiceServer,
    tilgangskontrollUrl: String,
    idToken: String,
    fnr: String,
    status: HttpStatus
) {
    val uriString = UriComponentsBuilder.fromHttpUrl(tilgangskontrollUrl)
        .path(VeilederTilgangConsumer.TILGANG_TIL_BRUKER_VIA_AZURE_PATH)
        .queryParam(VeilederTilgangConsumer.FNR, fnr)
        .toUriString()
    mockRestServiceServer.expect(ExpectedCount.manyTimes(), MockRestRequestMatchers.requestTo(uriString))
        .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
        .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, "Bearer $idToken"))
        .andRespond(MockRestResponseCreators.withStatus(status))
}
