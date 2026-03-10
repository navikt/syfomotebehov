package no.nav.syfo.varsel

import no.nav.syfo.consumer.narmesteleder.NarmesteLederRelasjonDTO
import no.nav.syfo.consumer.narmesteleder.NarmesteLederService
import no.nav.syfo.varsel.esyfovarsel.EsyfovarselService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import javax.inject.Inject

@Service
class VarselServiceV2 @Inject constructor(
    private val esyfovarselService: EsyfovarselService,
    private val narmesteLederService: NarmesteLederService,
) {
    fun ferdigstillSvarMotebehovVarselForNarmesteLeder(ansattFnr: String, virksomhetsnummer: String) {
        narmesteLeder(ansattFnr, virksomhetsnummer)?.let {
            log.info("Ferdigstiller varsel til virksomhet ${it.virksomhetsnummer}")
            ferdigstillSvarMotebehovVarselForNarmesteLeder(ansattFnr, it.narmesteLederPersonIdentNumber, it.virksomhetsnummer)
        }
    }

    fun ferdigstillSvarMotebehovVarselForNarmesteLeder(
        ansattFnr: String,
        naermesteLederFnr: String,
        virksomhetsnummer: String
    ) {
        esyfovarselService.ferdigstillSvarMotebehovForArbeidsgiver(naermesteLederFnr, ansattFnr, virksomhetsnummer)
    }

    fun ferdigstillSvarMotebehovVarselForArbeidstaker(ansattFnr: String) {
        esyfovarselService.ferdigstillSvarMotebehovForArbeidstaker(ansattFnr)
    }

    private fun narmesteLeder(ansattFnr: String, virksomhetsnummer: String): NarmesteLederRelasjonDTO? {
        return narmesteLederService.getAllNarmesteLederRelations(ansattFnr)
            ?.firstOrNull { virksomhetsnummer == it.virksomhetsnummer }
    }

    companion object {
        private val log = LoggerFactory.getLogger(VarselServiceV2::class.java)
    }
}
