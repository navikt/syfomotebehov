package no.nav.syfo.consumer.narmesteleder

import io.mockk.every
import io.mockk.mockk
import no.nav.syfo.consumer.aktorregister.domain.Fodselsnummer
import no.nav.syfo.testhelper.UserConstants.ARBEIDSTAKER_FNR
import no.nav.syfo.testhelper.UserConstants.ARBEIDSTAKER_FNR_2
import no.nav.syfo.testhelper.UserConstants.LEDER_FNR
import no.nav.syfo.testhelper.UserConstants.VIRKSOMHETSNUMMER
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal class NarmesteLederServiceTest {
    private val narmesteLederClient: NarmesteLederClient = mockk()
    private val narmesteLederService: NarmesteLederService = NarmesteLederService(narmesteLederClient)

    @Test
    fun duplikateNarmesteLedereSkalSlaasSammen() {
        val relasjoner = listOf(
            createNarmesteLederRelasjonDTO(
                virksomhetsnummer = VIRKSOMHETSNUMMER,
                arbeidstakerPersonIdentNumber = ARBEIDSTAKER_FNR,
                narmesteLederPersonIdentNumber = LEDER_FNR,
                status = NarmesteLederRelasjonStatus.INNMELDT_AKTIV
            ),
            createNarmesteLederRelasjonDTO(
                virksomhetsnummer = VIRKSOMHETSNUMMER,
                arbeidstakerPersonIdentNumber = ARBEIDSTAKER_FNR,
                narmesteLederPersonIdentNumber = LEDER_FNR,
                status = NarmesteLederRelasjonStatus.INNMELDT_AKTIV
            )
        )

        every { narmesteLederClient.getNarmesteledere(any()) } returns relasjoner

        val allNarmesteLederRelations =
            narmesteLederService.getAllNarmesteLederRelations(Fodselsnummer(ARBEIDSTAKER_FNR))

        assertThat(allNarmesteLederRelations?.size).isEqualTo(1)
    }

    @Test
    fun skalKunHenteDenSykmeldtesLedere() {
        val relasjoner = listOf(
            createNarmesteLederRelasjonDTO(
                virksomhetsnummer = VIRKSOMHETSNUMMER,
                arbeidstakerPersonIdentNumber = ARBEIDSTAKER_FNR,
                narmesteLederPersonIdentNumber = LEDER_FNR,
                status = NarmesteLederRelasjonStatus.INNMELDT_AKTIV
            ),
            createNarmesteLederRelasjonDTO(
                virksomhetsnummer = VIRKSOMHETSNUMMER,
                arbeidstakerPersonIdentNumber = ARBEIDSTAKER_FNR_2, // Den sykmeldte er NL for denne
                narmesteLederPersonIdentNumber = ARBEIDSTAKER_FNR,
                status = NarmesteLederRelasjonStatus.INNMELDT_AKTIV
            )
        )

        every { narmesteLederClient.getNarmesteledere(any()) } returns relasjoner

        val allNarmesteLederRelations =
            narmesteLederService.getAllNarmesteLederRelations(Fodselsnummer(ARBEIDSTAKER_FNR))

        assertThat(allNarmesteLederRelations?.size).isEqualTo(1)
    }

    fun createNarmesteLederRelasjonDTO(
        virksomhetsnummer: String,
        arbeidstakerPersonIdentNumber: String,
        narmesteLederPersonIdentNumber: String,
        status: NarmesteLederRelasjonStatus
    ): NarmesteLederRelasjonDTO {
        return NarmesteLederRelasjonDTO(
            uuid = UUID.randomUUID().toString(),
            arbeidstakerPersonIdentNumber = arbeidstakerPersonIdentNumber,
            virksomhetsnavn = "Yolomasters",
            virksomhetsnummer = virksomhetsnummer,
            narmesteLederPersonIdentNumber = narmesteLederPersonIdentNumber,
            narmesteLederTelefonnummer = "99",
            narmesteLederEpost = "9@9.mooo",
            narmesteLederNavn = "Hei heisann",
            aktivFom = LocalDate.now().minusYears(1),
            aktivTom = null,
            arbeidsgiverForskutterer = null,
            timestamp = LocalDateTime.now(),
            status = status
        )
    }
}
