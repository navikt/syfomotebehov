package no.nav.syfo.testhelper

import no.nav.syfo.pdl.*

fun generatePdlPersonNavn(): PdlPersonNavn {
    return PdlPersonNavn(
            fornavn = UserConstants.PERSON_NAME_FIRST,
            mellomnavn = UserConstants.PERSON_NAME_MIDDLE,
            etternavn = UserConstants.PERSON_NAME_LAST
    )
}

fun generateAdressebeskyttelse(): Adressebeskyttelse {
    return Adressebeskyttelse(
            gradering = Gradering.UGRADERT
    )
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
    )
}
