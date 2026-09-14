package no.nav.syfo.consumer.brukertilgang

import jakarta.ws.rs.ForbiddenException
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.syfo.api.auth.tokenX.TokenXUtil
import no.nav.syfo.metric.BrukertilgangOutcome
import no.nav.syfo.metric.Metric
import org.springframework.stereotype.Service
import java.util.UUID
import javax.inject.Inject

@Service
class BrukertilgangService
    @Inject
    constructor(
        private val contextHolder: TokenValidationContextHolder,
        private val dineSykmeldteConsumer: IDineSykmeldteConsumer,
        private val metric: Metric,
    ) {
        fun kastExceptionHvisIkkeTilgangTilAnsatt(
            fnr: String,
            virksomhetsnummer: String,
            narmesteLederId: UUID,
        ) {
            val innloggetIdent = TokenXUtil.fnrFromIdportenTokenX(contextHolder)

            val harTilgang =
                harTilgangTilOppslaattBruker(
                    innloggetIdent = innloggetIdent,
                    ansattFnr = fnr,
                    virksomhetsnummer = virksomhetsnummer,
                    narmesteLederId = narmesteLederId,
                )
            if (!harTilgang) {
                throw ForbiddenException("Ikke tilgang til arbeidstaker")
            }
        }

        fun harTilgangTilOppslaattBruker(
            innloggetIdent: String,
            ansattFnr: String,
            virksomhetsnummer: String,
            narmesteLederId: UUID,
        ): Boolean {
            if (innloggetIdent == ansattFnr) {
                metric.tellBrukertilgangArbeidsgiver(BrukertilgangOutcome.ALLOWED)
                return true
            }

            val sykmeldt =
                try {
                    dineSykmeldteConsumer.getSykmeldt(narmesteLederId)
                } catch (exception: Exception) {
                    metric.tellBrukertilgangArbeidsgiver(BrukertilgangOutcome.TECHNICAL_ERROR)
                    throw exception
                }
            val harTilgang = sykmeldt?.fnr == ansattFnr && sykmeldt.orgnummer == virksomhetsnummer
            metric.tellBrukertilgangArbeidsgiver(
                if (harTilgang) {
                    BrukertilgangOutcome.ALLOWED
                } else {
                    BrukertilgangOutcome.DENIED
                },
            )
            return harTilgang
        }
    }
