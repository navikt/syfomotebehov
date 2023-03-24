package no.nav.syfo.consumer.brukertilgang

import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.syfo.api.auth.tokenX.TokenXUtil
import no.nav.syfo.cache.CacheConfig
import no.nav.syfo.consumer.pdl.PdlConsumer
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import javax.inject.Inject
import javax.ws.rs.ForbiddenException

@Service
class BrukertilgangService @Inject constructor(
    private val contextHolder: TokenValidationContextHolder,
    private val brukertilgangConsumer: BrukertilgangConsumer,
    private val pdlConsumer: PdlConsumer
) {
    fun kastExceptionHvisIkkeTilgangTilSegSelv(fnr: String) {
        if (isBrukerGradertForInformasjon(fnr)) {
            throw ForbiddenException("Ikke tilgang: innlogget person er gradert for informasjon")
        }
    }

    fun kastExceptionHvisIkkeTilgangTilAnsatt(fnr: String) {
        val innloggetIdent = TokenXUtil.fnrFromIdportenTokenX(contextHolder)

        val harTilgang = harTilgangTilOppslaattBruker(innloggetIdent, fnr)
        if (!harTilgang) {
            throw ForbiddenException("Ikke tilgang til arbeidstaker: inlogget person har ikke tilgang til den ansatte eller den ansatte er gradert for informasjon")
        }
    }

    fun harTilgangTilOppslaattBruker(innloggetIdent: String, ansattFnr: String): Boolean {
        return try {
            !(sporOmNoenAndreEnnSegSelvEllerEgneAnsatte(innloggetIdent, ansattFnr) || isBrukerGradertForInformasjon(ansattFnr))
        } catch (e: ForbiddenException) {
            false
        }
    }

    @Cacheable(
        cacheNames = [CacheConfig.CACHENAME_TILGANG_IDENT],
        key = "#innloggetIdent.concat(#oppslaattFnr)",
        condition = "#innloggetIdent != null && #oppslaattFnr != null"
    )
    fun sporOmNoenAndreEnnSegSelvEllerEgneAnsatte(
        innloggetIdent: String,
        oppslaattFnr: String
    ): Boolean {
        return !(oppslaattFnr == innloggetIdent || brukertilgangConsumer.hasAccessToAnsatt(oppslaattFnr))
    }

    private fun isBrukerGradertForInformasjon(fnr: String): Boolean {
        return pdlConsumer.isKode6(fnr)
    }
}
