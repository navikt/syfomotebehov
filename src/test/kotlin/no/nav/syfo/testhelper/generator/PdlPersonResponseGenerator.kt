package no.nav.syfo.testhelper.generator

import no.nav.syfo.consumer.pdl.*
import no.nav.syfo.testhelper.UserConstants

fun generatePdlPersonNavn(): PdlPersonNavn {
    return PdlPersonNavn(
        fornavn = UserConstants.PERSON_NAME_FIRST,
        mellomnavn = UserConstants.PERSON_NAME_MIDDLE,
        etternavn = UserConstants.PERSON_NAME_LAST
    ).copy()
}

fun generateAdressebeskyttelse(): Adressebeskyttelse {
    return Adressebeskyttelse(
        gradering = Gradering.UGRADERT
    ).copy()
}

fun generatePdlHentPerson(
    pdlPersonNavn: PdlPersonNavn?,
    adressebeskyttelse: Adressebeskyttelse?
): PdlHentPerson {
    return PdlHentPerson(
        hentPerson = PdlPerson(
            navn = listOf(
                pdlPersonNavn ?: generatePdlPersonNavn()
            ),
            adressebeskyttelse = listOf(
                adressebeskyttelse ?: generateAdressebeskyttelse()
            )
        )
    ).copy()
}
