package no.nav.syfo.stubs

import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.token.support.core.api.Unprotected
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Profile("local")
@RestController
@Unprotected
@RequestMapping(value = ["/local/mocktoken"])
class MockTokenController(val mockOauth: MockOAuth2Server) {
    val logger = LoggerFactory.getLogger(MockTokenController::class.java)
    init {
        logger.info("MockTokenController initialized with MockOAuth2Server on port: ${mockOauth.config.httpServer.port()}")
    }

    @GetMapping(
        value = ["/tokenx"],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun getMockTokenx(@RequestParam fnr: String?, @RequestParam clientId :String?): ResponseEntity<String> {
        logger.info("MockTokenController returned token")
        return ResponseEntity.ok(mockOauth.issueToken(
            "tokenx",
            SUBJECT,
            "clientID",
            claims = mapOf(
            "acr" to "idporten-loa-high",
            "pid" to (fnr ?: "123456789"),
            "client_id" to (clientId ?: "dialogmote-frontend")
        )).serialize())
    }

    @GetMapping(
        value = ["/azuread"],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun getMockTokenAD(@RequestParam fnr: String?, @RequestParam clientId: String?, @RequestParam navIdent: String?): ResponseEntity<String> {
        logger.info("MockTokenController returned token")
        return ResponseEntity.ok(mockOauth.issueToken(
            "internazureadv2",
            SUBJECT,
            "clientID",
            claims = mapOf(
                "acr" to "Level4",
                "pid" to (fnr ?: "123456789"),
                "client_id" to (clientId ?: "dialogmote-frontend"),
                "NAVident" to (navIdent ?: "Z999999")
            )).serialize())
    }

    companion object {
        const val SUBJECT = "motebehov-frontend"
    }
}
