package no.nav.syfo.brukertilgang

import no.nav.syfo.aktorregister.domain.Fodselsnummer
import no.nav.syfo.config.CacheConfig
import no.nav.syfo.pdl.PdlConsumer
import no.nav.syfo.pdl.isKode6
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import javax.inject.Inject
import javax.ws.rs.ForbiddenException

@Service
class BrukertilgangService @Inject constructor(
        private val brukertilgangConsumer: BrukertilgangConsumer,
        private val pdlConsumer: PdlConsumer
) {
    fun harTilgangTilOppslaattBruker(innloggetIdent: String, fnr: String): Boolean {
        return try {
            !(sporOmNoenAndreEnnSegSelvEllerEgneAnsatte(innloggetIdent, fnr)
                    || pdlConsumer.person(Fodselsnummer(fnr))?.isKode6() == true)
        } catch (e: ForbiddenException) {
            false
        }
    }

    @Cacheable(cacheNames = [CacheConfig.CACHENAME_TILGANG_IDENT], key = "#innloggetIdent.concat(#oppslaattFnr)", condition = "#innloggetIdent != null && #oppslaattFnr != null")
    fun sporOmNoenAndreEnnSegSelvEllerEgneAnsatte(innloggetIdent: String, oppslaattFnr: String): Boolean {
        return !(oppslaattFnr == innloggetIdent || brukertilgangConsumer.hasAccessToAnsatt(oppslaattFnr))
    }

}
