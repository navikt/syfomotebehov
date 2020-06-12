package no.nav.syfo.consumer.aktorregister

import no.nav.syfo.consumer.aktorregister.domain.Identinfo
import no.nav.syfo.consumer.aktorregister.domain.IdentinfoListe
import no.nav.syfo.consumer.aktorregister.exceptions.*

fun currentIdentFromAktorregisterResponse(response: Map<String, IdentinfoListe?>, desiredUsersIdent: String, desiredIdentType: IdentType): String {
    val identinfoListe = response[desiredUsersIdent]
    throwExceptionIfErrorOrNoUser(identinfoListe)
    val currentIdentinfo = identinfoListe!!.identer.stream()
        .filter { identinfo: Identinfo -> identinfo.gjeldende && desiredIdentType.name == identinfo.identgruppe }
        .findAny()
        .orElse(null)
    if (currentIdentinfo == null || currentIdentinfo.ident.isEmpty()) {
        throw NoCurrentIdentForAktor("Tried getting ident for aktor")
    } else {
        return currentIdentinfo.ident
    }
}

private fun throwExceptionIfErrorOrNoUser(identinfoListe: IdentinfoListe?) {
    if (identinfoListe == null) {
        throw NoResponseForDesiredUser("Tried getting info about user from aktorregisteret. Tremendous FAIL!")
    }
    if (identinfoListe.feilmelding != null) {
        throw AktorGotError(identinfoListe.feilmelding)
    }
}
