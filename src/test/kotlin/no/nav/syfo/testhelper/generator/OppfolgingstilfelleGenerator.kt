package no.nav.syfo.testhelper.generator

import no.nav.syfo.motebehov.motebehovstatus.DAYS_START_SVAR_BEHOV
import no.nav.syfo.oppfolgingstilfelle.database.PersonOppfolgingstilfelle
import no.nav.syfo.testhelper.UserConstants.ARBEIDSTAKER_FNR
import java.time.LocalDate

val generatePersonOppfolgingstilfelle = PersonOppfolgingstilfelle(
    fnr = ARBEIDSTAKER_FNR,
    fom = LocalDate.now(),
    tom = LocalDate.now()
).copy()

val generatePersonOppfolgingstilfelleMeldBehovFirstPeriod = generatePersonOppfolgingstilfelle.copy(
    fnr = ARBEIDSTAKER_FNR,
    fom = LocalDate.now().minusDays(DAYS_START_SVAR_BEHOV).plusDays(1),
    tom = LocalDate.now().plusDays(1)
).copy()

val generatePersonOppfolgingstilfelleSvarBehov = generatePersonOppfolgingstilfelle.copy(
    fnr = ARBEIDSTAKER_FNR,
    fom = LocalDate.now().minusDays(DAYS_START_SVAR_BEHOV),
    tom = LocalDate.now().plusDays(1)
).copy()
