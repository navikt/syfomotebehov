package no.nav.syfo.testhelper

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.syfo.aktorregister.IdentType
import no.nav.syfo.aktorregister.domain.Identinfo
import no.nav.syfo.aktorregister.domain.IdentinfoListe
import no.nav.syfo.testhelper.UserConstants.ARBEIDSTAKER_FNR
import no.nav.syfo.util.bearerCredentials
import org.springframework.http.*
import org.springframework.test.web.client.ExpectedCount
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers
import org.springframework.test.web.client.response.MockRestResponseCreators
import org.springframework.web.util.UriComponentsBuilder

private const val MOCK_AKTORID_PREFIX = "10"

fun getFnrFromMockedAktorId(aktorId: String): String {
    return aktorId.replace(MOCK_AKTORID_PREFIX, "")
}

fun mockAktorId(fnr: String): String {
    return "$MOCK_AKTORID_PREFIX$fnr"
}

fun mockAndExpectAktorregRequest(mockRestServiceServer: MockRestServiceServer, baseUrl: String) {
    val uriString = UriComponentsBuilder.fromHttpUrl(baseUrl)
            .path("/identer")
            .queryParam("gjeldende", true)
            .toUriString()
    try {
        val map = mapOf(ARBEIDSTAKER_FNR to IdentinfoListe(
                listOf(Identinfo(
                        ident = ARBEIDSTAKER_FNR,
                        identgruppe = IdentType.NorskIdent.name,
                        gjeldende = true
                )),
                feilmelding = null
        ))

        val json = ObjectMapper().writeValueAsString(map)

        mockRestServiceServer.expect(ExpectedCount.manyTimes(), MockRestRequestMatchers.requestTo(uriString))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, bearerCredentials(UserConstants.STS_TOKEN)))
                .andRespond(MockRestResponseCreators.withSuccess(json, MediaType.APPLICATION_JSON))
    } catch (e: JsonProcessingException) {
        e.printStackTrace()
    }
}
