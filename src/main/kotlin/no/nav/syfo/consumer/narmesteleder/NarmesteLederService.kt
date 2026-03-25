package no.nav.syfo.consumer.narmesteleder

import org.springframework.stereotype.Service
import javax.inject.Inject

@Service
class NarmesteLederService
    @Inject
    constructor(
        private val narmesteLederClient: NarmesteLederClient,
    ) {
        fun getAllNarmesteLederRelations(ansattFnr: String): List<NarmesteLederRelasjonDTO>? {
            val allRelations = narmesteLederClient.getNarmesteledere(ansattFnr)

            // isnarmesteleder returnerer alle relasjoner, også der sykmeldte er leder for noen andre. Må derfor filtrere for å kun få lederne til den sykmeldte.
            return allRelations
                ?.filter { it.status == NarmesteLederRelasjonStatus.INNMELDT_AKTIV }
                ?.filter { it.arbeidstakerPersonIdentNumber == ansattFnr }
                ?.distinctBy { it.narmesteLederPersonIdentNumber }
        }
    }
