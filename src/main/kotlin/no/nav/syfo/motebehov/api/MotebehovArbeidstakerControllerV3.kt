package no.nav.syfo.motebehov.api

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(value = ["/api/v3/arbeidstaker"])
class MotebehovArbeidstakerControllerV3 {
    @GetMapping("/motebehov")
    fun motebehovStatusArbeidstaker() = ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY).build<Void>()

    @GetMapping("/motebehov/all")
    fun motebehovStatusArbeidstakerWithCodeSixUsers() = ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY).build<Void>()

    @PostMapping("/motebehov")
    fun submitMotebehovArbeidstaker(
    ) = ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY).build<Void>()

    @PostMapping("/motebehov/ferdigstill")
    fun ferdigstillMotebehovArbeidstaker() = ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY).build<Void>()
}
