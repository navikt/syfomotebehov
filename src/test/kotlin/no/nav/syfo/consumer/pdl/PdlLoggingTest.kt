package no.nav.syfo.consumer.pdl

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.syfo.consumer.azuread.v2.IAzureAdV2TokenConsumer
import no.nav.syfo.metric.Metric
import no.nav.syfo.testhelper.captureApplicationLogs
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestTemplate

class PdlLoggingTest :
    FunSpec({
        test("a failed person lookup preserves all PDL errors in one event without person or token data") {
            val client = mockk<RestTemplate>()
            val tokens = mockk<IAzureAdV2TokenConsumer>()
            every { tokens.getSystemToken(any()) } returns "PRIVATE_TOKEN"
            every {
                client.exchange(
                    any<String>(),
                    HttpMethod.POST,
                    any<HttpEntity<*>>(),
                    any<ParameterizedTypeReference<PdlPersonResponse>>(),
                )
            } returns
                ResponseEntity.ok(
                    PdlPersonResponse(
                        errors =
                            listOf(
                                PdlError(
                                    "Useful PDL explanation",
                                    emptyList(),
                                    listOf("hentPerson"),
                                    PdlErrorExtension("unauthorized", "ExecutionAborted"),
                                ),
                                PdlError(
                                    "Second PDL explanation",
                                    emptyList(),
                                    listOf("hentPerson"),
                                    PdlErrorExtension("timeout", "ExecutionAborted"),
                                ),
                            ),
                        data = null,
                    ),
                )
            val consumer = PdlConsumer(mockk<Metric>(relaxed = true), "pdl", "https://pdl", client, tokens)
            val logs =
                captureApplicationLogs {
                    consumer.person("12345678910") shouldBe null
                }
            logs.size shouldBe 1
            val event = logs.single()
            event["event_type"].asString() shouldBe "pdl_lookup_failed"
            event["operation"].asString() shouldBe "person_fetch"
            event["upstream"].asString() shouldBe "pdl"
            event["pdl_errors"].size() shouldBe 2
            event["pdl_errors"][0]["message"].asString() shouldBe "Useful PDL explanation"
            event.toString().contains("12345678910") shouldBe false
            event.toString().contains("PRIVATE_TOKEN") shouldBe false
        }
    })
