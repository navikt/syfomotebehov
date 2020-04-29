package no.nav.syfo.consumer.pdl

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(code = HttpStatus.INTERNAL_SERVER_ERROR)
class PdlRequestFailedException(message: String = "Request to get Person from PDL Failed") : RuntimeException(message)
