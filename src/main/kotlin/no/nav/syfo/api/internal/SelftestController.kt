package no.nav.syfo.api.internal

import no.nav.security.oidc.api.Unprotected
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(value = ["/internal"])
class SelftestController {
    @Unprotected
    @RequestMapping(value = ["/isAlive"], produces = [MediaType.TEXT_PLAIN_VALUE])
    fun isAlive(): String {
        return "Application is ready!"
    }

    @Unprotected
    @RequestMapping(value = ["/isReady"], produces = [MediaType.TEXT_PLAIN_VALUE])
    fun isReady(): String {
        return "Application is ready!"
    }
}
