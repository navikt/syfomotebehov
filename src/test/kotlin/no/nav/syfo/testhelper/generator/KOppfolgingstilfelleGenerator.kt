package no.nav.syfo.testhelper.generator

import no.nav.syfo.consumer.pdl.fullName
import no.nav.syfo.motebehov.motebehovstatus.DAYS_END_SVAR_BEHOV
import no.nav.syfo.motebehov.motebehovstatus.DAYS_START_SVAR_BEHOV
import no.nav.syfo.oppfolgingstilfelle.kafka.KOversikthendelsetilfelle
import no.nav.syfo.testhelper.UserConstants.ARBEIDSTAKER_FNR
import no.nav.syfo.testhelper.UserConstants.NAV_ENHET
import no.nav.syfo.testhelper.UserConstants.VIRKSOMHETSNAVN
import no.nav.syfo.testhelper.UserConstants.VIRKSOMHETSNUMMER
import java.time.LocalDate
import java.time.LocalDateTime

val generateOversikthendelsetilfelle =
        KOversikthendelsetilfelle(
                fnr = ARBEIDSTAKER_FNR,
                navn = generatePdlHentPerson(null, null).fullName()!!,
                enhetId = NAV_ENHET,
                virksomhetsnummer = VIRKSOMHETSNUMMER,
                gradert = false,
                fom = LocalDate.now().minusDays(DAYS_START_SVAR_BEHOV),
                tom = LocalDate.now().plusDays(DAYS_END_SVAR_BEHOV),
                tidspunkt = LocalDateTime.now(),
                virksomhetsnavn = VIRKSOMHETSNAVN
        ).copy()
