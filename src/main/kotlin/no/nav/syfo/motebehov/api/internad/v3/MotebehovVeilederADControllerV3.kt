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
    fun hentMotebehovListe() = ResponseEntity<Unit>(HttpStatus.MOVED_PERMANENTLY)

    @GetMapping("/historikk")
    fun hentMotebehovHistorikk() = ResponseEntity<Unit>(HttpStatus.MOVED_PERMANENTLY)

    @PostMapping("/motebehov/tilbakemelding")
    fun sendTilbakemelding() = ResponseEntity<Unit>(HttpStatus.MOVED_PERMANENTLY)

    @PostMapping("/motebehov/behandle")
    fun behandleMotebehov() = ResponseEntity<Unit>(HttpStatus.MOVED_PERMANENTLY)
}
