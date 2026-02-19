package no.nav.syfo

import io.kotest.core.spec.style.DescribeSpec
import io.mockk.clearAllMocks
import org.springframework.test.annotation.DirtiesContext

@DirtiesContext
abstract class IntegrationTest : DescribeSpec() {
    init {
        afterTest {
            clearAllMocks()
        }
    }
}
