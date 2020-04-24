package no.nav.syfo.oppfolgingstilfelle.syketilfelle

import java.time.LocalDate

data class KSyketilfelledag(
        val dag: LocalDate,
        val prioritertSyketilfellebit: KSyketilfellebit?
)
