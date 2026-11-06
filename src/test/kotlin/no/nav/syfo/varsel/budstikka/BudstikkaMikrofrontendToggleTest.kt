package no.nav.syfo.varsel.budstikka

import io.kotest.core.spec.style.DescribeSpec
import io.mockk.mockk
import io.mockk.verify

class BudstikkaMikrofrontendToggleTest :
    DescribeSpec({
        val utsendingDAO = mockk<BudstikkaMikrofrontendDAO>(relaxed = true)
        val service =
            BudstikkaMikrofrontendService(
                utsendingDAO = utsendingDAO,
                dialogmoteDAO = mockk(relaxed = true),
                producer = mockk(relaxed = true),
                metric = mockk(relaxed = true),
                transactionManager = mockk(relaxed = true),
                viaBudstikka = false,
            )

        describe("Når syfo-dialog fortsatt går via esyfovarsel") {
            it("legges ingen kommandoer i kø") {
                service.vis("12345678912")
                service.skjulHvisIngenAktivtDialogmote("12345678912")

                verify(exactly = 0) { utsendingDAO.create(any(), any()) }
            }
        }
    })
