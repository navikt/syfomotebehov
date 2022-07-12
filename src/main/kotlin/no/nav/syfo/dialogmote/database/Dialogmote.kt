package no.nav.syfo.oppfolgingstilfelle.database

import java.time.LocalDateTime
import java.util.*

data class Dialogmote(
    val uuid: UUID,
    val dialogmoteExternUuid: UUID,
    val moteTidspunkt: LocalDateTime,
    val statusEndringTidspunkt: LocalDateTime,
    val statusEndringType: DialogmoteStatusEndringType,
    val personIdent: String,
    val virksomhetsnummer: String,
)

enum class DialogmoteStatusEndringType {
    INNKALT,
    AVLYST,
    FERDIGSTILT,
    NYTT_TID_STED,
}
