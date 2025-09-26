package no.nav.syfo.motebehov.api.internad.v3

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(value = ["/api/internad/v3/veileder"])
class MotebehovVeilederADControllerV3 {
    @GetMapping("/motebehov")
    fun hentMotebehovListe() = ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY).build<Void>()

    @GetMapping("/historikk")
    fun hentMotebehovHistorikk() = ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY).build<Void>()

    @PostMapping("/motebehov/tilbakemelding")
    fun sendTilbakemelding() = ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY).build<Void>()

    @PostMapping("/motebehov/behandle")
    fun behandleMotebehov() = ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY).build<Void>()
}
