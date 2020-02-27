package no.nav.syfo.pdl

import no.nav.syfo.util.lowerCapitalize
import java.io.Serializable

data class PdlPersonResponse(
        val errors: List<PdlError>?,
        val data: PdlHentPerson?
)

data class PdlError(
        val message: String,
        val locations: List<PdlErrorLocation>,
        val path: List<String>?,
        val extensions: PdlErrorExtension
)

data class PdlErrorLocation(
        val line: Int?,
        val column: Int?
)

data class PdlErrorExtension(
        val code: String?,
        val classification: String
)

data class PdlHentPerson(
        val hentPerson: PdlPerson?
) : Serializable

data class PdlPerson(
        val navn: List<PdlPersonNavn>,
        val adressebeskyttelse: List<Adressebeskyttelse>?
) : Serializable

data class PdlPersonNavn(
        val fornavn: String,
        val mellomnavn: String?,
        val etternavn: String
) : Serializable

data class Adressebeskyttelse(
        val gradering: Gradering
) : Serializable

enum class Gradering : Serializable {
    STRENGT_FORTROLIG,
    FORTROLIG,
    UGRADERT
}


fun PdlHentPerson.diskresjonskode(): String {
    val adressebeskyttelse = this.hentPerson?.adressebeskyttelse
    if (adressebeskyttelse.isNullOrEmpty()) {
        return ""
    } else {
        val gradering: Gradering = adressebeskyttelse.first().gradering
        return when {
            gradering === Gradering.STRENGT_FORTROLIG -> {
                "6"
            }
            gradering === Gradering.FORTROLIG -> {
                "7"
            }
            else -> {
                ""
            }
        }
    }
}

fun PdlHentPerson.isKode6(): Boolean {
    val adressebeskyttelse = this.hentPerson?.adressebeskyttelse
    return if (adressebeskyttelse.isNullOrEmpty()) {
        false
    } else {
        return adressebeskyttelse.any {
            it.gradering == Gradering.STRENGT_FORTROLIG
        }
    }
}

fun PdlHentPerson.isKode6Or7(): Boolean {
    val adressebeskyttelse = this.hentPerson?.adressebeskyttelse
    return if (adressebeskyttelse.isNullOrEmpty()) {
        false
    } else {
        return adressebeskyttelse.any {
            it.gradering == Gradering.STRENGT_FORTROLIG || it.gradering == Gradering.FORTROLIG
        }
    }
}

fun PdlHentPerson.fullName(): String? {
    val nameList = this.hentPerson?.navn
    if (nameList.isNullOrEmpty()) {
        return null
    }
    nameList[0].let {
        val firstName = it.fornavn.lowerCapitalize()
        val middleName = it.mellomnavn
        val surName = it.etternavn.lowerCapitalize()

        return if (middleName.isNullOrBlank()) {
            "$firstName $surName"
        } else {
            "$firstName ${middleName.lowerCapitalize()} $surName"
        }
    }
}

fun PdlError.errorMessage(): String {
    return "${this.message} with code: ${extensions.code} and classification: ${extensions.classification}"
}
