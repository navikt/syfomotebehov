package no.nav.syfo.motebehov.database

import no.nav.syfo.LocalApplication
import no.nav.syfo.testhelper.UserConstants.ARBEIDSTAKER_AKTORID
import no.nav.syfo.testhelper.UserConstants.LEDER_AKTORID
import no.nav.syfo.testhelper.UserConstants.VIRKSOMHETSNUMMER
import no.nav.syfo.testhelper.generator.MotebehovGenerator
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.temporal.ChronoUnit
import javax.inject.Inject

@ExtendWith(SpringExtension::class)
@SpringBootTest(classes = [LocalApplication::class])
@DirtiesContext
class MotebehovDAOTest {
    @Inject
    private lateinit var jdbcTemplate: JdbcTemplate

    @Inject
    private lateinit var motebehovDAO: MotebehovDAO

    private val motebehovGenerator = MotebehovGenerator()

    @BeforeEach
    fun cleanup() {
        val sqlDeleteAll = "DELETE FROM MOTEBEHOV"
        jdbcTemplate.update(sqlDeleteAll)
    }

    private fun insertPMotebehov(motebehov: PMotebehov) {
        val sqlInsert =
            "INSERT INTO MOTEBEHOV VALUES('bae778f2-a085-11e8-98d0-529269fb1459', '" + motebehov.opprettetDato + "', '" + motebehov.opprettetAv + "', '" + motebehov.aktoerId + "', '" + motebehov.virksomhetsnummer + "', '" + '1' + "', '" + motebehov.forklaring + "', '" + motebehov.tildeltEnhet + "', null, null, null)"
        jdbcTemplate.update(sqlInsert)
    }

    @Test
    @Throws(java.lang.Exception::class)
    fun hentMotebehovListeForAktoer() {
        val pMotebehov = motebehovGenerator.generatePmotebehov()
        insertPMotebehov(pMotebehov)
        val motebehovListe = motebehovDAO.hentMotebehovListeForAktoer(ARBEIDSTAKER_AKTORID)
        assertThat(motebehovListe.size).isEqualTo(1)
        val motebehovFraDb = motebehovListe[0]
        assertThat(motebehovFraDb.opprettetDato.truncatedTo(ChronoUnit.SECONDS)).isEqualTo(
            pMotebehov.opprettetDato.truncatedTo(
                ChronoUnit.SECONDS
            )
        )
        assertThat(motebehovFraDb.opprettetAv).isEqualTo(pMotebehov.opprettetAv)
        assertThat(motebehovFraDb.aktoerId).isEqualTo(pMotebehov.aktoerId)
        assertThat(motebehovFraDb.virksomhetsnummer).isEqualTo(pMotebehov.virksomhetsnummer)
        assertThat(motebehovFraDb.harMotebehov).isEqualTo(pMotebehov.harMotebehov)
        assertThat(motebehovFraDb.forklaring).isEqualTo(pMotebehov.forklaring)
        assertThat(motebehovFraDb.tildeltEnhet).isEqualTo(pMotebehov.tildeltEnhet)
        assertThat(motebehovFraDb.skjemaType).isEqualTo(pMotebehov.skjemaType)
    }

    @Test
    @Throws(java.lang.Exception::class)
    fun hentMotebehovListeForOgOpprettetAvArbeidstakerIkkeGyldig() {
        val pMotebehov = motebehovGenerator.generatePmotebehov().copy(
            opprettetDato = motebehovGenerator.getOpprettetDato(false),
            opprettetAv = ARBEIDSTAKER_AKTORID
        )
        insertPMotebehov(pMotebehov)
        val motebehovListe = motebehovDAO.hentMotebehovListeForOgOpprettetAvArbeidstaker(ARBEIDSTAKER_AKTORID)
        assertThat(motebehovListe.size).isEqualTo(0)
    }

    @Test
    @Throws(java.lang.Exception::class)
    fun hentMotebehovListeForOgOpprettetAvArbeidstakerGyldig() {
        val pMotebehov = motebehovGenerator.generatePmotebehov().copy(
            opprettetDato = motebehovGenerator.getOpprettetDato(true),
            opprettetAv = ARBEIDSTAKER_AKTORID
        )
        insertPMotebehov(pMotebehov)
        val motebehovListe = motebehovDAO.hentMotebehovListeForOgOpprettetAvArbeidstaker(ARBEIDSTAKER_AKTORID)
        assertThat(motebehovListe.size).isEqualTo(1)
        val motebehovFraDb = motebehovListe[0]
        assertThat(motebehovFraDb.opprettetDato.truncatedTo(ChronoUnit.SECONDS)).isEqualTo(
            pMotebehov.opprettetDato.truncatedTo(
                ChronoUnit.SECONDS
            )
        )
        assertThat(motebehovFraDb.opprettetAv).isEqualTo(pMotebehov.opprettetAv)
        assertThat(motebehovFraDb.aktoerId).isEqualTo(pMotebehov.aktoerId)
    }

    @Test
    @Throws(java.lang.Exception::class)
    fun hentMotebehovListeForArbeidstakerOpprettetAvLederIkkeGyldig() {
        val pMotebehov = motebehovGenerator.generatePmotebehov().copy(
            opprettetDato = motebehovGenerator.getOpprettetDato(false),
            opprettetAv = LEDER_AKTORID
        )
        insertPMotebehov(pMotebehov)
        val motebehovListe = motebehovDAO.hentMotebehovListeForArbeidstakerOpprettetAvLeder(
            ARBEIDSTAKER_AKTORID,
            false,
            VIRKSOMHETSNUMMER
        )
        assertThat(motebehovListe.size).isEqualTo(0)
    }

    @Test
    @Throws(java.lang.Exception::class)
    fun hentMotebehovListeForArbeidstakerOpprettetAvLederGyldig() {
        val pMotebehov = motebehovGenerator.generatePmotebehov().copy(
            opprettetDato = motebehovGenerator.getOpprettetDato(true),
            opprettetAv = LEDER_AKTORID
        )
        insertPMotebehov(pMotebehov)
        val motebehovListe = motebehovDAO.hentMotebehovListeForArbeidstakerOpprettetAvLeder(
            ARBEIDSTAKER_AKTORID,
            false,
            VIRKSOMHETSNUMMER
        )
        assertThat(motebehovListe.size).isEqualTo(1)
        val motebehovFraDb = motebehovListe[0]
        assertThat(motebehovFraDb.opprettetDato.truncatedTo(ChronoUnit.SECONDS)).isEqualTo(
            pMotebehov.opprettetDato.truncatedTo(
                ChronoUnit.SECONDS
            )
        )
        assertThat(motebehovFraDb.opprettetAv).isEqualTo(LEDER_AKTORID)
        assertThat(motebehovFraDb.aktoerId).isEqualTo(pMotebehov.aktoerId)
    }

    @Test
    fun skalHenteAlleMotebehovForAktorDersomEgenLeder() {
        val pMotebehov = motebehovGenerator.generatePmotebehov().copy(
            opprettetDato = motebehovGenerator.getOpprettetDato(true),
            opprettetAv = ARBEIDSTAKER_AKTORID
        )
        insertPMotebehov(pMotebehov)
        val motebehovListe = motebehovDAO.hentMotebehovListeForArbeidstakerOpprettetAvLeder(
            ARBEIDSTAKER_AKTORID,
            true,
            VIRKSOMHETSNUMMER
        )
        assertThat(motebehovListe.size).isEqualTo(1)
        val motebehovFraDb = motebehovListe[0]
        assertThat(motebehovFraDb.opprettetDato.truncatedTo(ChronoUnit.SECONDS)).isEqualTo(
            pMotebehov.opprettetDato.truncatedTo(
                ChronoUnit.SECONDS
            )
        )
        assertThat(motebehovFraDb.opprettetAv).isEqualTo(ARBEIDSTAKER_AKTORID)
        assertThat(motebehovFraDb.aktoerId).isEqualTo(pMotebehov.aktoerId)
    }

    @Test
    @Throws(java.lang.Exception::class)
    fun lagreMotebehov() {
        val uuid = motebehovDAO.create(motebehovGenerator.generatePmotebehov())
        val motebehovListe = motebehovDAO.hentMotebehovListeForAktoer(ARBEIDSTAKER_AKTORID)
        assertThat(motebehovListe.size).isEqualTo(1)
        assertThat(motebehovListe[0].uuid).isEqualTo(uuid)
    }
}
