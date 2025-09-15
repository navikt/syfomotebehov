package no.nav.syfo.motebehov.api

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(value = ["/api/v3"])
class MotebehovArbeidsgiverControllerV3 {
    @GetMapping("/motebehov")
    fun motebehovStatusArbeidsgiver() = ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY).build<Void>()

    @PostMapping("/motebehov")
    fun lagreMotebehovArbeidsgiver() = ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY).build<Void>()
}
