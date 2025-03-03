package no.nav.syfo.motebehov

import com.ninjasquad.springmockk.MockkBean
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.syfo.IntegrationTest
import no.nav.syfo.LocalApplication
import no.nav.syfo.motebehov.database.MotebehovDAO
import no.nav.syfo.personoppgavehendelse.PersonoppgavehendelseService
import no.nav.syfo.testhelper.generator.MotebehovGenerator
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.jdbc.core.JdbcTemplate
import java.time.LocalDate

@TestConfiguration
@SpringBootTest(classes = [LocalApplication::class])
class MotebehovServiceTest : IntegrationTest() {

    @MockkBean(relaxed = true)
    private lateinit var personoppgavehendelseService: PersonoppgavehendelseService

    @Autowired
    private lateinit var motebehovDAO: MotebehovDAO

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    private lateinit var motebehovService: MotebehovService

    private val veilederIdent = "testVeileder"

    init {
        extensions(SpringExtension)
        beforeTest {
            val sqlDeleteAll = "DELETE FROM MOTEBEHOV"
            jdbcTemplate.update(sqlDeleteAll)
        }

        describe("Møtebehov Service") {
            it("skalFerdigstilleMotebehovOpprettetForDato") {
                val pMotebehov = MotebehovGenerator().generatePmotebehov()
                val uuid = motebehovDAO.create(pMotebehov)
                val count = motebehovService.behandleUbehandledeMotebehovOpprettetTidligereEnnDato(
                    LocalDate.now().plusWeeks(1),
                    veilederIdent
                )
                val motebehov = motebehovDAO.hentMotebehov(uuid.toString())
                count shouldBe 1
                motebehov.first().behandletVeilederIdent shouldBe veilederIdent
                motebehov.first().behandletTidspunkt.shouldNotBeNull()
            }

            it("skalIkkeFerdigstilleMotebehovOpprettetEtterDato") {
                val pMotebehov = MotebehovGenerator().generatePmotebehov()
                val uuid = motebehovDAO.create(pMotebehov)
                val count = motebehovService.behandleUbehandledeMotebehovOpprettetTidligereEnnDato(
                    LocalDate.now().minusWeeks(1),
                    veilederIdent
                )
                val motebehov = motebehovDAO.hentMotebehov(uuid.toString())
                count shouldBe 0
                motebehov.first().behandletVeilederIdent.shouldBeNull()
                motebehov.first().behandletTidspunkt.shouldBeNull()
            }
        }
    }
}
