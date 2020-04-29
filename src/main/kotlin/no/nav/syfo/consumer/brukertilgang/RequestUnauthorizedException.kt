package no.nav.syfo.consumer.brukertilgang

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(code = HttpStatus.UNAUTHORIZED)
class RequestUnauthorizedException(message: String) : RuntimeException(message)
