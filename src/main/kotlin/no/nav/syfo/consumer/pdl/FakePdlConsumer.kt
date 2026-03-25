package no.nav.syfo.consumer.pdl

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Profile("local")
@Component
class FakePdlConsumer : IPdlConsumer {
    val person =
        mutableMapOf(
            "123456789" to
                PdlPerson(
                    navn =
                        listOf(
                            PdlPersonNavn(
                                fornavn = "Ola",
                                etternavn = "Nordmann",
                                mellomnavn = null,
                            ),
                        ),
                    adressebeskyttelse = emptyList(),
                ),
        )

    override fun person(ident: String): PdlHentPerson? = person[ident]?.let { PdlHentPerson(it) }

    override fun aktorid(fnr: String): String = fnr

    override fun fnr(aktorid: String): String = aktorid

    override fun isKode6(fnr: String): Boolean = false
}
