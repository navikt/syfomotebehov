package no.nav.syfo.oversikthendelse

import java.io.Serializable
import java.time.LocalDateTime

data class KOversikthendelse(
    val fnr: String,
    val hendelseId: String,
    val enhetId: String,
    val tidspunkt: LocalDateTime
) : Serializable
