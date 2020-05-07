package no.nav.syfo.testhelper.generator

import no.nav.syfo.consumer.aktorregister.domain.Fodselsnummer
import no.nav.syfo.motebehov.motebehovstatus.DAYS_START_SVAR_BEHOV
import no.nav.syfo.oppfolgingstilfelle.database.PersonOppfolgingstilfelle
import no.nav.syfo.oppfolgingstilfelle.kafka.KOversikthendelsetilfelle
import no.nav.syfo.testhelper.UserConstants.ARBEIDSTAKER_FNR
import no.nav.syfo.testhelper.UserConstants.NAV_ENHET
import no.nav.syfo.testhelper.UserConstants.PERSON_FULL_NAME
import no.nav.syfo.testhelper.UserConstants.VIRKSOMHETSNAVN
import no.nav.syfo.testhelper.UserConstants.VIRKSOMHETSNUMMER
import java.time.LocalDate
import java.time.LocalDateTime

val generateKOversikthendelsetilfelle = KOversikthendelsetilfelle(
        fnr = ARBEIDSTAKER_FNR,
        navn = PERSON_FULL_NAME,
        enhetId = NAV_ENHET,
        virksomhetsnummer = VIRKSOMHETSNUMMER,
        virksomhetsnavn = VIRKSOMHETSNAVN,
        gradert = false,
        fom = LocalDate.now(),
        tom = LocalDate.now(),
        tidspunkt = LocalDateTime.now().minusMinutes(10)
).copy()

val generateKOversikthendelsetilfelleMeldBehovFirstPeriod = generateKOversikthendelsetilfelle.copy(
        fom = LocalDate.now().minusDays(DAYS_START_SVAR_BEHOV).plusDays(1),
        tom = LocalDate.now().plusDays(1)
).copy()

val generateKOversikthendelsetilfelleSvarBehov = generateKOversikthendelsetilfelle.copy(
        fom = LocalDate.now().minusDays(DAYS_START_SVAR_BEHOV),
        tom = LocalDate.now().plusDays(1)
).copy()

val generatePersonOppfolgingstilfelle = PersonOppfolgingstilfelle(
        fnr = Fodselsnummer(ARBEIDSTAKER_FNR),
        fom = LocalDate.now(),
        tom = LocalDate.now()
).copy()

val generatePersonOppfolgingstilfelleMeldBehovFirstPeriod = generatePersonOppfolgingstilfelle.copy(
        fnr = Fodselsnummer(ARBEIDSTAKER_FNR),
        fom = LocalDate.now().minusDays(DAYS_START_SVAR_BEHOV).plusDays(1),
        tom = LocalDate.now().plusDays(1)
).copy()

val generatePersonOppfolgingstilfelleSvarBehov = generatePersonOppfolgingstilfelle.copy(
        fnr = Fodselsnummer(ARBEIDSTAKER_FNR),
        fom = LocalDate.now().minusDays(DAYS_START_SVAR_BEHOV),
        tom = LocalDate.now().plusDays(1)
).copy()