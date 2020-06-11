package no.nav.syfo.consumer.brukertilgang

import no.nav.security.oidc.context.OIDCRequestContextHolder
import no.nav.syfo.api.auth.OIDCUtil
import no.nav.syfo.cache.CacheConfig
import no.nav.syfo.consumer.aktorregister.domain.Fodselsnummer
import no.nav.syfo.consumer.pdl.PdlConsumer
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import javax.inject.Inject
import javax.ws.rs.ForbiddenException

@Service
class BrukertilgangService @Inject constructor(
    private val contextHolder: OIDCRequestContextHolder,
    private val brukertilgangConsumer: BrukertilgangConsumer,
    private val pdlConsumer: PdlConsumer
) {
    fun kastExceptionHvisIkkeTilgang(fnr: String) {
        val innloggetIdent = OIDCUtil.fnrFraOIDCEkstern(contextHolder).value
        val harTilgang = harTilgangTilOppslaattBruker(innloggetIdent, fnr)
        if (!harTilgang) {
            throw ForbiddenException("Ikke tilgang")
        }
    }

    fun harTilgangTilOppslaattBruker(innloggetIdent: String, fnr: String): Boolean {
        return try {
            !(sporOmNoenAndreEnnSegSelvEllerEgneAnsatte(innloggetIdent, fnr) ||
                pdlConsumer.isKode6(Fodselsnummer(fnr)))
        } catch (e: ForbiddenException) {
            false
        }
    }

    @Cacheable(cacheNames = [CacheConfig.CACHENAME_TILGANG_IDENT], key = "#innloggetIdent.concat(#oppslaattFnr)", condition = "#innloggetIdent != null && #oppslaattFnr != null")
    fun sporOmNoenAndreEnnSegSelvEllerEgneAnsatte(innloggetIdent: String, oppslaattFnr: String): Boolean {
        return !(oppslaattFnr == innloggetIdent || brukertilgangConsumer.hasAccessToAnsatt(oppslaattFnr))
    }
}
