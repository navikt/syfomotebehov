package no.nav.syfo.consumer.brukertilgang

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import no.nav.security.token.support.core.context.TokenValidationContext
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.syfo.metric.BrukertilgangOutcome
import no.nav.syfo.metric.Metric
import no.nav.syfo.testhelper.UserConstants.ARBEIDSTAKER_FNR
import no.nav.syfo.testhelper.UserConstants.LEDER_FNR
import no.nav.syfo.testhelper.UserConstants.NARMESTE_LEDER_ID
import no.nav.syfo.testhelper.UserConstants.VIRKSOMHETSNUMMER
import java.util.UUID

class BrukertilgangskontrollServiceTest :
    DescribeSpec({
        val dineSykmeldteConsumer = FakeDineSykmeldteConsumer()
        val registry = SimpleMeterRegistry()
        val tilgangskontrollService =
            BrukertilgangService(
                UnusedTokenValidationContextHolder,
                dineSykmeldteConsumer,
                Metric(registry),
            )

        beforeTest {
            dineSykmeldteConsumer.reset()
            registry.clear()
        }

        describe("BrukertilgangService") {
            it("gir tilgang til egen ident uten downstream-oppslag") {
                val tilgang =
                    tilgangskontrollService.harTilgangTilOppslaattBruker(
                        innloggetIdent = ARBEIDSTAKER_FNR,
                        ansattFnr = ARBEIDSTAKER_FNR,
                        virksomhetsnummer = VIRKSOMHETSNUMMER,
                        narmesteLederId = NARMESTE_LEDER_ID,
                    )

                tilgang shouldBe true
                dineSykmeldteConsumer.callCount shouldBe 0
                registry.assertAccessOutcome(BrukertilgangOutcome.ALLOWED)
            }

            it("gir tilgang når leder, arbeidstaker og virksomhet samsvarer") {
                dineSykmeldteConsumer.response =
                    DineSykmeldteResponse(
                        fnr = ARBEIDSTAKER_FNR,
                        orgnummer = VIRKSOMHETSNUMMER,
                    )

                val tilgang =
                    tilgangskontrollService.harTilgangTilOppslaattBruker(
                        innloggetIdent = LEDER_FNR,
                        ansattFnr = ARBEIDSTAKER_FNR,
                        virksomhetsnummer = VIRKSOMHETSNUMMER,
                        narmesteLederId = NARMESTE_LEDER_ID,
                    )

                tilgang shouldBe true
                registry.assertAccessOutcome(BrukertilgangOutcome.ALLOWED)
            }

            it("avviser når arbeidstaker ikke samsvarer") {
                dineSykmeldteConsumer.response =
                    DineSykmeldteResponse(
                        fnr = "10987654321",
                        orgnummer = VIRKSOMHETSNUMMER,
                    )

                tilgangskontrollService.harTilgangTilOppslaattBruker(
                    innloggetIdent = LEDER_FNR,
                    ansattFnr = ARBEIDSTAKER_FNR,
                    virksomhetsnummer = VIRKSOMHETSNUMMER,
                    narmesteLederId = NARMESTE_LEDER_ID,
                ) shouldBe false
                registry.assertAccessOutcome(BrukertilgangOutcome.DENIED)
            }

            it("avviser når virksomhet ikke samsvarer") {
                dineSykmeldteConsumer.response =
                    DineSykmeldteResponse(
                        fnr = ARBEIDSTAKER_FNR,
                        orgnummer = "987654321",
                    )

                tilgangskontrollService.harTilgangTilOppslaattBruker(
                    innloggetIdent = LEDER_FNR,
                    ansattFnr = ARBEIDSTAKER_FNR,
                    virksomhetsnummer = VIRKSOMHETSNUMMER,
                    narmesteLederId = NARMESTE_LEDER_ID,
                ) shouldBe false
                registry.assertAccessOutcome(BrukertilgangOutcome.DENIED)
            }

            it("avviser når oppslaget ikke finnes") {
                tilgangskontrollService.harTilgangTilOppslaattBruker(
                    innloggetIdent = LEDER_FNR,
                    ansattFnr = ARBEIDSTAKER_FNR,
                    virksomhetsnummer = VIRKSOMHETSNUMMER,
                    narmesteLederId = NARMESTE_LEDER_ID,
                ) shouldBe false
                registry.assertAccessOutcome(BrukertilgangOutcome.DENIED)
            }

            it("propagerer tekniske feil fra Dine sykmeldte") {
                dineSykmeldteConsumer.exception = IllegalStateException("Downstream error")

                shouldThrow<IllegalStateException> {
                    tilgangskontrollService.harTilgangTilOppslaattBruker(
                        innloggetIdent = LEDER_FNR,
                        ansattFnr = ARBEIDSTAKER_FNR,
                        virksomhetsnummer = VIRKSOMHETSNUMMER,
                        narmesteLederId = NARMESTE_LEDER_ID,
                    )
                }
                registry.assertAccessOutcome(BrukertilgangOutcome.TECHNICAL_ERROR)
            }
        }
    })

private class FakeDineSykmeldteConsumer : IDineSykmeldteConsumer {
    var response: DineSykmeldteResponse? = null
    var exception: RuntimeException? = null
    var callCount: Int = 0

    override fun getSykmeldt(narmesteLederId: UUID): DineSykmeldteResponse? {
        callCount++
        exception?.let { throw it }
        return response
    }

    fun reset() {
        response = null
        exception = null
        callCount = 0
    }
}

private object UnusedTokenValidationContextHolder : TokenValidationContextHolder {
    override fun getTokenValidationContext(): TokenValidationContext =
        error("Token context is not used by harTilgangTilOppslaattBruker")

    override fun setTokenValidationContext(tokenValidationContext: TokenValidationContext?) =
        error("Token context is not used by harTilgangTilOppslaattBruker")
}

private fun SimpleMeterRegistry.assertAccessOutcome(outcome: BrukertilgangOutcome) {
    get("syfomotebehov_brukertilgang_arbeidsgiver")
        .tag("type", "info")
        .tag("outcome", outcome.metricValue)
        .counter()
        .count() shouldBe 1.0
    meters.size shouldBe 1
}
