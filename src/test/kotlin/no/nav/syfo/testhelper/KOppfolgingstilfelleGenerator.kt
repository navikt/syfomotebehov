package no.nav.syfo.testhelper

import no.nav.syfo.motebehov.DAYS_END_DIALOGMOTE2
import no.nav.syfo.motebehov.DAYS_START_DIALOGMOTE2
import no.nav.syfo.oppfolgingstilfelle.syketilfelle.KOppfolgingstilfelle
import no.nav.syfo.oppfolgingstilfelle.syketilfelle.KSyketilfelledag
import no.nav.syfo.testhelper.UserConstants.ARBEIDSTAKER_AKTORID
import no.nav.syfo.testhelper.UserConstants.VIRKSOMHETSNUMMER
import java.time.LocalDate
import java.time.LocalDateTime

fun generateKSyketilfelledag(): KSyketilfelledag {
    return KSyketilfelledag(
            dag = LocalDate.now().minusDays(DAYS_START_DIALOGMOTE2),
            prioritertSyketilfellebit = null
    ).copy()
}

fun generateKOppfolgingstilfelle(): KOppfolgingstilfelle {
    return KOppfolgingstilfelle(
            aktorId = ARBEIDSTAKER_AKTORID,
            orgnummer = VIRKSOMHETSNUMMER,
            tidslinje = listOf(
                    generateKSyketilfelledag().copy(
                            dag = LocalDate.now().minusDays(DAYS_START_DIALOGMOTE2)
                    ),
                    generateKSyketilfelledag().copy(
                            dag = LocalDate.now().plusDays(DAYS_END_DIALOGMOTE2)
                    )
            ),
            sisteDagIArbeidsgiverperiode = generateKSyketilfelledag().copy(
                    dag = LocalDate.now().minusDays(DAYS_START_DIALOGMOTE2).plusDays(16)
            ),
            antallBrukteDager = 0,
            oppbruktArbeidsgvierperiode = false,
            utsendelsestidspunkt = LocalDateTime.now().minusDays(1)
    ).copy()
}
