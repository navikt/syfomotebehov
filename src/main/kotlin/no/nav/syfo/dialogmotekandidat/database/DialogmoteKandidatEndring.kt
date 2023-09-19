package no.nav.syfo.dialogmotekandidat.database

import java.time.LocalDateTime
import java.util.*

data class DialogmoteKandidatEndring(
    val uuid: UUID,
    val dialogmotekandidatExternUUID: UUID,
    val personIdentNumber: String,
    val kandidat: Boolean,
    val arsak: DialogmotekandidatEndringArsak,
    val createdAt: LocalDateTime,
    val databaseUpdatedAt: LocalDateTime
)

enum class DialogmotekandidatEndringArsak {
    STOPPUNKT,
    DIALOGMOTE_FERDIGSTILT,
    UNNTAK,
    LUKKET,
}
