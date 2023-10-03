package no.nav.syfo.motebehov

import com.ninjasquad.springmockk.MockkBean
import no.nav.syfo.LocalApplication
import no.nav.syfo.motebehov.database.MotebehovDAO
import no.nav.syfo.personoppgavehendelse.PersonoppgavehendelseService
import no.nav.syfo.testhelper.generator.MotebehovGenerator
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.LocalDate
import java.util.*
import javax.inject.Inject

@ExtendWith(SpringExtension::class)
@SpringBootTest(classes = [LocalApplication::class])
@DirtiesContext
class MotebehovServiceTest {

    @MockkBean(relaxed = true)
    private lateinit var personoppgavehendelseService: PersonoppgavehendelseService

    @Inject
    private lateinit var motebehovDAO: MotebehovDAO

    @Inject
    private lateinit var jdbcTemplate: JdbcTemplate

    @Inject
    private lateinit var motebehovService: MotebehovService

    private val veilederIdent = "testVeileder"

    @BeforeEach
    fun cleanup() {
        val sqlDeleteAll = "DELETE FROM MOTEBEHOV"
        jdbcTemplate.update(sqlDeleteAll)
    }

    @Test
    fun skalFerdigstilleMotebehovOpprettetForDato() {
        val pMotebehov = MotebehovGenerator().generatePmotebehov()
        val uuid = motebehovDAO.create(pMotebehov)
        val count = motebehovService.behandleUbehandledeMotebehovOpprettetTidligereEnnDato(LocalDate.now().plusWeeks(1), veilederIdent)
        val motebehov = motebehovDAO.hentMotebehov(uuid.toString())
        assertEquals(1, count)
        assertEquals(veilederIdent, motebehov.first().behandletVeilederIdent)
        assertNotNull(motebehov.first().behandletTidspunkt)
    }

    @Test
    fun skalIkkeFerdigstilleMotebehovOpprettetEtterDato() {
        val pMotebehov = MotebehovGenerator().generatePmotebehov()
        val uuid = motebehovDAO.create(pMotebehov)
        val count = motebehovService.behandleUbehandledeMotebehovOpprettetTidligereEnnDato(LocalDate.now().minusWeeks(1), veilederIdent)
        val motebehov = motebehovDAO.hentMotebehov(uuid.toString())
        assertEquals(0, count)
        assertNull(motebehov.first().behandletVeilederIdent)
        assertNull(motebehov.first().behandletTidspunkt)
    }
}
