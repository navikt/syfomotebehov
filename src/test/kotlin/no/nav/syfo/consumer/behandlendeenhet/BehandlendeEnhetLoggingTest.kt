package no.nav.syfo.consumer.behandlendeenhet

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.syfo.consumer.azuread.v2.IAzureAdV2TokenConsumer
import no.nav.syfo.metric.Metric
import no.nav.syfo.testhelper.captureApplicationLogs
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.RestTemplate

class BehandlendeEnhetLoggingTest :
    FunSpec({
        test("HTTP client emits one strictly parsed upstream status without response body") {
            val tokens = mockk<IAzureAdV2TokenConsumer>()
            every { tokens.getSystemToken(any()) } returns "PRIVATE_TOKEN"
            val template = mockk<RestTemplate>()
            every {
                template.exchange(any<String>(), HttpMethod.GET, any<HttpEntity<*>>(), BehandlendeEnhet::class.java)
            } throws HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE, "PRIVATE_RESPONSE")
            val consumer = BehandlendeEnhetConsumer(tokens, mockk<Metric>(relaxed = true), "unit", "https://unit", template)
            val logs =
                captureApplicationLogs {
                    shouldThrow<HttpServerErrorException> {
                        consumer.getBehandlendeEnhet("12345678910", null)
                    }
                }
            val event = logs.single()
            event["event_type"].asText() shouldBe "behandlende_enhet_fetch_failed"
            event["upstream_status"].asInt() shouldBe 503
            event.toString().contains("PRIVATE_") shouldBe false
            event.toString().contains("12345678910") shouldBe false
        }
    })
