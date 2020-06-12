package no.nav.syfo.motebehov.api

import no.nav.security.oidc.api.Unprotected
import no.nav.syfo.motebehov.database.MotebehovDAO
import no.nav.syfo.toggle.Toggle
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import java.util.stream.Stream

@RestController
@Unprotected
@RequestMapping(value = ["/internal"])
class NullstillMotebehovController(private val motebehovDAO: MotebehovDAO) {
    @RequestMapping(value = ["/nullstill/{aktoerId}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun slettMotebehov(@PathVariable aktoerId: String, @Value("\${nais.cluster.name:ukjent}") env: String): String {
        return if (Toggle.enableNullstill || Stream.of("q1", "local").anyMatch { anObject: String -> env.equals(anObject) }) {
            log.info("Sletter alle møtebehov på aktørid: {}", aktoerId)
            val antallSlettedeMotebehov = motebehovDAO.nullstillMotebehov(aktoerId)
            "Slettet $antallSlettedeMotebehov møtebehov."
        } else {
            log.info("Det ble gjort kall mot 'nullstill', men dette endepunktet er togglet av og skal aldri brukes i prod.")
            "Toggle er av, eller er sletting ikke tilgjengelig i $env"
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(NullstillMotebehovController::class.java)
    }
}
