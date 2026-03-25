package no.nav.syfo.dialogmotekandidat.database

import java.time.LocalDateTime
import java.util.UUID

data class DialogmoteKandidatEndring(
    val uuid: UUID,
    val dialogmotekandidatExternUUID: UUID,
    val personIdentNumber: String,
    val kandidat: Boolean,
    val arsak: DialogmotekandidatEndringArsak,
    val createdAt: LocalDateTime,
    val databaseUpdatedAt: LocalDateTime,
)

enum class DialogmotekandidatEndringArsak {
    STOPPUNKT,
    DIALOGMOTE_FERDIGSTILT,
    DIALOGMOTE_LUKKET,
    UNNTAK,
    IKKE_AKTUELL,
    LUKKET,
}
