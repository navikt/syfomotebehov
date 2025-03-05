package no.nav.syfo.consumer.narmesteleder

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.syfo.testhelper.UserConstants.ARBEIDSTAKER_FNR
import no.nav.syfo.testhelper.UserConstants.ARBEIDSTAKER_FNR_2
import no.nav.syfo.testhelper.UserConstants.LEDER_FNR
import no.nav.syfo.testhelper.UserConstants.VIRKSOMHETSNUMMER
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class NarmesteLederServiceTest : DescribeSpec({
    val narmesteLederClient: NarmesteLederClient = mockk()
    val narmesteLederService = NarmesteLederService(narmesteLederClient)

    it("duplicate nærmesteleadere should be merged") {
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
            narmesteLederService.getAllNarmesteLederRelations(ARBEIDSTAKER_FNR)

        allNarmesteLederRelations?.size shouldBe 1
    }

    it("will only get the nærmesteleder for the sykmeldte") {
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
            narmesteLederService.getAllNarmesteLederRelations(ARBEIDSTAKER_FNR)

        allNarmesteLederRelations?.size shouldBe 1
    }
})

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
