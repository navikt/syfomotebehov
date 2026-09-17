package no.nav.syfo.consumer.brukertilgang

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import jakarta.ws.rs.ForbiddenException
import no.nav.syfo.metric.BrukertilgangOutcome
import no.nav.syfo.metric.Metric
import no.nav.syfo.testhelper.UserConstants.ARBEIDSTAKER_FNR
import no.nav.syfo.testhelper.UserConstants.NARMESTE_LEDER_ID
import no.nav.syfo.testhelper.UserConstants.VIRKSOMHETSNUMMER
import java.util.UUID

class DineSykmeldteTilgangServiceTest :
    DescribeSpec({
        val dineSykmeldteConsumer = FakeDineSykmeldteConsumer()
        val registry = SimpleMeterRegistry()
        val tilgangService = DineSykmeldteTilgangService(dineSykmeldteConsumer, Metric(registry))

        beforeTest {
            dineSykmeldteConsumer.reset()
            registry.clear()
        }

        describe("DineSykmeldteTilgangService") {
            it("returnerer arbeidstakeren fra den autoriserte nærmeste-leder-relasjonen") {
                val sykmeldt =
                    DineSykmeldteResponse(
                        fnr = ARBEIDSTAKER_FNR,
                        orgnummer = VIRKSOMHETSNUMMER,
                    )
                dineSykmeldteConsumer.response = sykmeldt

                tilgangService.hentSykmeldtMedTilgang(NARMESTE_LEDER_ID) shouldBe sykmeldt
                registry.assertAccessOutcome(BrukertilgangOutcome.ALLOWED)
            }

            it("avviser når nærmeste-leder-relasjonen ikke finnes") {
                shouldThrow<ForbiddenException> {
                    tilgangService.hentSykmeldtMedTilgang(NARMESTE_LEDER_ID)
                }
                registry.assertAccessOutcome(BrukertilgangOutcome.DENIED)
            }

            it("propagerer tekniske feil fra Dine sykmeldte") {
                dineSykmeldteConsumer.exception = DineSykmeldteRequestException("Downstream error")

                shouldThrow<DineSykmeldteRequestException> {
                    tilgangService.hentSykmeldtMedTilgang(NARMESTE_LEDER_ID)
                }
                registry.assertAccessOutcome(BrukertilgangOutcome.TECHNICAL_ERROR)
            }
        }
    })

private class FakeDineSykmeldteConsumer : IDineSykmeldteConsumer {
    var response: DineSykmeldteResponse? = null
    var exception: DineSykmeldteRequestException? = null

    override fun getSykmeldt(narmesteLederId: UUID): DineSykmeldteResponse? {
        exception?.let { throw it }
        return response
    }

    fun reset() {
        response = null
        exception = null
    }
}

private fun SimpleMeterRegistry.assertAccessOutcome(outcome: BrukertilgangOutcome) {
    get("syfomotebehov_brukertilgang_arbeidsgiver")
        .tag("type", "info")
        .tag("outcome", outcome.metricValue)
        .counter()
        .count() shouldBe 1.0
    meters.size shouldBe 1
}
