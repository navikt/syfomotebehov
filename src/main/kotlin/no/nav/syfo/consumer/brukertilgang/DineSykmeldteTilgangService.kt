package no.nav.syfo.consumer.brukertilgang

import jakarta.ws.rs.ForbiddenException
import no.nav.syfo.metric.BrukertilgangOutcome
import no.nav.syfo.metric.Metric
import org.springframework.stereotype.Service
import java.util.UUID
import javax.inject.Inject

@Service
class DineSykmeldteTilgangService
    @Inject
    constructor(
        private val dineSykmeldteConsumer: IDineSykmeldteConsumer,
        private val metric: Metric,
    ) {
        fun hentSykmeldtMedTilgang(narmesteLederId: UUID): DineSykmeldteResponse =
            try {
                val sykmeldt = dineSykmeldteConsumer.getSykmeldt(narmesteLederId)
                if (sykmeldt == null) {
                    metric.tellBrukertilgangArbeidsgiver(BrukertilgangOutcome.DENIED)
                    throw ForbiddenException("Ikke tilgang til arbeidstaker")
                }

                metric.tellBrukertilgangArbeidsgiver(BrukertilgangOutcome.ALLOWED)
                sykmeldt
            } catch (exception: ForbiddenException) {
                throw exception
            } catch (exception: DineSykmeldteRequestException) {
                metric.tellBrukertilgangArbeidsgiver(BrukertilgangOutcome.TECHNICAL_ERROR)
                throw exception
            }
    }
