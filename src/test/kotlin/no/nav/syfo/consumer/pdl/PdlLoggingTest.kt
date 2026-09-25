package no.nav.syfo.consumer.pdl

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.syfo.api.exception.ControllerExceptionHandler
import no.nav.syfo.consumer.azuread.v2.IAzureAdV2TokenConsumer
import no.nav.syfo.metric.Metric
import no.nav.syfo.testhelper.captureApplicationLogs
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.RestTemplate
import org.springframework.web.context.request.WebRequest

class PdlLoggingTest :
    FunSpec({
        for (operation in listOf("person_fetch", "aktorid_fetch", "personident_fetch")) {
            test("$operation HTTP failure produces one terminal API event with the original cause and status") {
                val client = mockk<RestTemplate>()
                val cause =
                    HttpServerErrorException.create(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "PRIVATE_RESPONSE",
                        HttpHeaders(),
                        "PRIVATE_BODY".toByteArray(),
                        Charsets.UTF_8,
                    )
                every {
                    client.exchange(
                        any<String>(),
                        HttpMethod.POST,
                        any<HttpEntity<*>>(),
                        any<ParameterizedTypeReference<Any>>(),
                    )
                } throws cause
                val consumer = consumer(client)
                val handler = ControllerExceptionHandler(mockk<Metric>(relaxed = true))
                val logs =
                    captureApplicationLogs {
                        val error =
                            shouldThrow<PdlRequestFailedException> {
                                when (operation) {
                                    "person_fetch" -> consumer.person("12345678910")
                                    "aktorid_fetch" -> consumer.aktorid("12345678910")
                                    else -> consumer.fnr("12345678910")
                                }
                            }
                        (error.cause === cause) shouldBe true
                        handler.handleException(error, mockk<WebRequest>(relaxed = true)).statusCode shouldBe
                            HttpStatus.INTERNAL_SERVER_ERROR
                    }
                logs.size shouldBe 1
                val event = logs.single()
                event["event_type"].asString() shouldBe "pdl_lookup_failed"
                event["operation"].asString() shouldBe operation
                event["upstream"].asString() shouldBe "pdl"
                event["failure_stage"].asString() shouldBe "upstream_request"
                event["failure_kind"].asString() shouldBe "http"
                event["upstream_status"].asInt() shouldBe 503
                event["exception_type"].asString() shouldBe "PdlRequestFailedException"
                event["pdl_operation"].asString() shouldBe operation
                event["stack_trace"].asString().contains("PdlConsumer") shouldBe true
                event["error_code"].asString() shouldBe "UPSTREAM_HTTP_ERROR"
                event.toString().contains("PRIVATE_") shouldBe false
                event.toString().contains("12345678910") shouldBe false
            }
        }

        for (operation in listOf("aktorid_fetch", "personident_fetch", "is_kode6")) {
            test("$operation GraphQL failure preserves the complete errors in one terminal API event") {
                val client = mockk<RestTemplate>()
                val errors =
                    listOf(
                        PdlError(
                            "Useful PDL explanation",
                            emptyList(),
                            listOf("hentIdenter"),
                            PdlErrorExtension("unauthorized", "ExecutionAborted"),
                        ),
                        PdlError(
                            "Second PDL explanation",
                            emptyList(),
                            listOf("hentIdenter"),
                            PdlErrorExtension("timeout", "ExecutionAborted"),
                        ),
                    )
                every {
                    client.exchange(
                        any<String>(),
                        HttpMethod.POST,
                        any<HttpEntity<*>>(),
                        any<ParameterizedTypeReference<Any>>(),
                    )
                } returns
                    ResponseEntity.ok(if (operation == "is_kode6") PdlPersonResponse(errors, null) else PdlIdenterResponse(errors, null))
                val consumer = consumer(client)
                val handler = ControllerExceptionHandler(mockk<Metric>(relaxed = true))
                val logs =
                    captureApplicationLogs {
                        val error =
                            shouldThrow<PdlRequestFailedException> {
                                when (operation) {
                                    "aktorid_fetch" -> consumer.aktorid("12345678910")
                                    "personident_fetch" -> consumer.fnr("12345678910")
                                    else -> consumer.isKode6("12345678910")
                                }
                            }
                        error.pdlErrors shouldBe errors
                        handler.handleException(error, mockk<WebRequest>(relaxed = true)).statusCode shouldBe
                            HttpStatus.INTERNAL_SERVER_ERROR
                    }
                logs.size shouldBe 1
                val event = logs.single()
                event["event_type"].asString() shouldBe "pdl_lookup_failed"
                event["operation"].asString() shouldBe if (operation == "is_kode6") "person_fetch" else operation
                event["upstream"].asString() shouldBe "pdl"
                event["failure_stage"].asString() shouldBe "graphql_response"
                event["error_code"].asString() shouldBe "PDL_GRAPHQL_ERROR"
                event["exception_type"].asString() shouldBe "PdlRequestFailedException"
                event["pdl_operation"].asString() shouldBe if (operation == "is_kode6") "person_fetch" else operation
                event["pdl_errors"].size() shouldBe 2
                event["pdl_errors"][0]["message"].asString() shouldBe errors[0].message
                event["pdl_errors"][1]["message"].asString() shouldBe errors[1].message
                event.toString().contains("12345678910") shouldBe false
                event.toString().contains("PRIVATE_TOKEN") shouldBe false
            }
        }

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

private fun consumer(client: RestTemplate): PdlConsumer {
    val tokens = mockk<IAzureAdV2TokenConsumer>()
    every { tokens.getSystemToken(any()) } returns "PRIVATE_TOKEN"
    return PdlConsumer(mockk<Metric>(relaxed = true), "pdl", "https://pdl", client, tokens)
}
