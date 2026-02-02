package no.nav.syfo.motebehov.api

import no.nav.syfo.metric.Metric
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(value = ["/api/v3"])
class MotebehovArbeidsgiverControllerV3(
    private val metric: Metric,
) {
    @GetMapping("/motebehov")
    fun motebehovStatusArbeidsgiver() = ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY).build<Void>().also {
        metric.tellEndepunktKall("call_endpoint_motebehovstatus_arbeidsgiver_v3")
    }

    @PostMapping("/motebehov")
    fun lagreMotebehovArbeidsgiver() = ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY).build<Void>().also {
        metric.tellEndepunktKall("call_endpoint_save_motebehov_arbeidsgiver_v3")
    }
}
