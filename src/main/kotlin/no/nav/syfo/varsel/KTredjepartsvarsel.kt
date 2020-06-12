package no.nav.syfo.varsel

import java.io.Serializable
import java.time.LocalDateTime

data class KTredjepartsvarsel(
    val type: String,
    val ressursId: String,
    val aktorId: String,
    val orgnummer: String,
    val utsendelsestidspunkt: LocalDateTime
) : Serializable
