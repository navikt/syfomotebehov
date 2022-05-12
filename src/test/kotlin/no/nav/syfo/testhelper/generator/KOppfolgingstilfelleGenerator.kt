package no.nav.syfo.testhelper.generator

import no.nav.syfo.motebehov.motebehovstatus.DAYS_END_SVAR_BEHOV
import no.nav.syfo.motebehov.motebehovstatus.DAYS_START_SVAR_BEHOV
import no.nav.syfo.oppfolgingstilfelle.kafka.domain.KafkaOppfolgingstilfelle
import no.nav.syfo.oppfolgingstilfelle.kafka.domain.KafkaOppfolgingstilfellePerson
import no.nav.syfo.testhelper.UserConstants.ARBEIDSTAKER_FNR
import no.nav.syfo.testhelper.UserConstants.VIRKSOMHETSNUMMER
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

fun generateOppfolgingstilfellePerson(
    start: LocalDate = LocalDate.now().minusDays(DAYS_START_SVAR_BEHOV),
    end: LocalDate = LocalDate.now().plusDays(DAYS_END_SVAR_BEHOV),
    virksomhetsnummerList: List<String> = listOf(VIRKSOMHETSNUMMER)
): KafkaOppfolgingstilfellePerson {
    return KafkaOppfolgingstilfellePerson(
        uuid = UUID.randomUUID().toString(),
        createdAt = OffsetDateTime.now(),
        personIdentNumber = ARBEIDSTAKER_FNR,
        oppfolgingstilfelleList = listOf(
            KafkaOppfolgingstilfelle(
                arbeidstakerAtTilfelleEnd = true,
                start = start,
                end = end,
                virksomhetsnummerList = virksomhetsnummerList
            )
        ),
        referanseTilfelleBitUuid = UUID.randomUUID().toString(),
        referanseTilfelleBitInntruffet = OffsetDateTime.now()
    ).copy()
}
