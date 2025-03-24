package no.nav.syfo.motebehov.database

import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.syfo.IntegrationTest
import no.nav.syfo.LocalApplication
import no.nav.syfo.motebehov.formSnapshot.MOCK_FORM_SNAPSHOT_JSON_ARBEIDSTAKER_SVAR
import no.nav.syfo.testhelper.UserConstants.ARBEIDSTAKER_AKTORID
import no.nav.syfo.testhelper.UserConstants.ARBEIDSTAKER_FNR
import no.nav.syfo.testhelper.UserConstants.LEDER_AKTORID
import no.nav.syfo.testhelper.UserConstants.VIRKSOMHETSNUMMER
import no.nav.syfo.testhelper.generator.MotebehovGenerator
import org.junit.jupiter.api.Assertions.assertEquals
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.jdbc.Sql
import java.time.temporal.ChronoUnit

@TestConfiguration
@SpringBootTest(classes = [LocalApplication::class])
@Sql(statements = ["DELETE FROM MOTEBEHOV"])
class MotebehovDAOTest : IntegrationTest() {

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    private lateinit var motebehovDAO: MotebehovDAO

    private val motebehovGenerator = MotebehovGenerator()

    init {
        extensions(SpringExtension)

        describe("Møtebehov DAO") {
            it("Hent møtebehov liste for aktør og sammenlign inserted med hentet") {
                val pMotebehov = motebehovGenerator.generatePmotebehov()
                insertPMotebehov(pMotebehov)
                val motebehovListe = motebehovDAO.hentMotebehovListeForAktoer(ARBEIDSTAKER_AKTORID)
                motebehovListe.size shouldBe 1
                val motebehovFraDb = motebehovListe[0]

                motebehovFraDb.opprettetDato.truncatedTo(ChronoUnit.SECONDS) shouldBe
                    pMotebehov.opprettetDato.truncatedTo(
                        ChronoUnit.SECONDS
                    )

                motebehovFraDb.opprettetAv shouldBe pMotebehov.opprettetAv
                motebehovFraDb.aktoerId shouldBe pMotebehov.aktoerId
                motebehovFraDb.virksomhetsnummer shouldBe pMotebehov.virksomhetsnummer
                motebehovFraDb.harMotebehov shouldBe pMotebehov.harMotebehov
                motebehovFraDb.forklaring shouldBe pMotebehov.forklaring
                motebehovFraDb.tildeltEnhet shouldBe pMotebehov.tildeltEnhet
                motebehovFraDb.skjemaType shouldBe pMotebehov.skjemaType

                areStringsEqualAsSqlJsonbValues(
                    pMotebehov.formValues?.formSnapshotJSON!!,
                    motebehovFraDb.formValues?.formSnapshotJSON!!
                ) shouldBe true
                motebehovFraDb.formValues?.begrunnelse shouldBe pMotebehov.formValues?.begrunnelse
                motebehovFraDb.formValues?.onskerSykmelderDeltar shouldBe
                    pMotebehov.formValues?.onskerSykmelderDeltar
                motebehovFraDb.formValues?.onskerSykmelderDeltar shouldBe
                    pMotebehov.formValues?.onskerSykmelderDeltar
                motebehovFraDb.formValues?.onskerSykmelderDeltarBegrunnelse shouldBe
                    pMotebehov.formValues?.onskerSykmelderDeltarBegrunnelse
                motebehovFraDb.formValues?.onskerTolk shouldBe
                    pMotebehov.formValues?.onskerTolk
                motebehovFraDb.formValues?.tolkSprak shouldBe pMotebehov.formValues?.tolkSprak
            }

            it("hentMotebehovListeForOgOpprettetAvArbeidstakerIkkeGyldig") {
                val pMotebehov = motebehovGenerator.generatePmotebehov().copy(
                    opprettetDato = motebehovGenerator.getOpprettetDato(false),
                    opprettetAv = ARBEIDSTAKER_AKTORID
                )
                insertPMotebehov(pMotebehov)
                val motebehovListe = motebehovDAO.hentMotebehovListeForOgOpprettetAvArbeidstaker(ARBEIDSTAKER_AKTORID)
                motebehovListe.size shouldBe 0
            }

            it("Hent møtebehov liste for og opprettet av arbeidstaker") {
                val pMotebehov = motebehovGenerator.generatePmotebehov().copy(
                    opprettetDato = motebehovGenerator.getOpprettetDato(true),
                    opprettetAv = ARBEIDSTAKER_AKTORID
                )
                insertPMotebehov(pMotebehov)
                val motebehovListe = motebehovDAO.hentMotebehovListeForOgOpprettetAvArbeidstaker(ARBEIDSTAKER_AKTORID)
                motebehovListe.size shouldBe 1
                val motebehovFraDb = motebehovListe[0]
                motebehovFraDb.opprettetDato.truncatedTo(ChronoUnit.SECONDS) shouldBe
                    pMotebehov.opprettetDato.truncatedTo(
                        ChronoUnit.SECONDS
                    )

                motebehovFraDb.opprettetAv shouldBe pMotebehov.opprettetAv
                motebehovFraDb.aktoerId shouldBe pMotebehov.aktoerId
            }

            it("hentMotebehovListeForOgOpprettetAvLederIkkeGyldig") {
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
                motebehovListe.size shouldBe 0
            }

            it("hentMotebehovListeForArbeidstakerOpprettetAvLederGyldig") {
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
                motebehovListe.size shouldBe 1
                val motebehovFraDb = motebehovListe[0]
                assertEquals(
                    motebehovFraDb.opprettetDato.truncatedTo(ChronoUnit.SECONDS),
                    pMotebehov.opprettetDato.truncatedTo(
                        ChronoUnit.SECONDS
                    )
                )
                motebehovFraDb.opprettetAv shouldBe LEDER_AKTORID
                motebehovFraDb.aktoerId shouldBe pMotebehov.aktoerId
            }

            it("skalHenteAlleMotebehovForAktorDersomEgenLeder") {
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
                motebehovListe.size shouldBe 1
                val motebehovFraDb = motebehovListe[0]
                motebehovFraDb.opprettetDato.truncatedTo(ChronoUnit.SECONDS) shouldBe
                    pMotebehov.opprettetDato.truncatedTo(
                        ChronoUnit.SECONDS
                    )
                motebehovFraDb.opprettetAv shouldBe ARBEIDSTAKER_AKTORID
                motebehovFraDb.aktoerId shouldBe pMotebehov.aktoerId
            }

            it("create møtebehov and retrieve it back") {
                val uuid = motebehovDAO.create(motebehovGenerator.generatePmotebehov())
                val motebehovListe = motebehovDAO.hentMotebehovListeForAktoer(ARBEIDSTAKER_AKTORID)
                motebehovListe.size shouldBe 1

                val retrievedMotebehov = motebehovListe[0]

                retrievedMotebehov.uuid shouldBe uuid
                retrievedMotebehov.harMotebehov shouldBe true
                retrievedMotebehov.aktoerId shouldBe ARBEIDSTAKER_AKTORID
                retrievedMotebehov.sykmeldtFnr shouldBe ARBEIDSTAKER_FNR

                retrievedMotebehov.formValues.shouldNotBeNull()
                areStringsEqualAsSqlJsonbValues(
                    MOCK_FORM_SNAPSHOT_JSON_ARBEIDSTAKER_SVAR,
                    retrievedMotebehov.formValues?.formSnapshotJSON!!
                ) shouldBe true
            }
        }
    }

    private fun insertPMotebehov(motebehov: PMotebehov) {
        val motebehovId = "bae778f2-a085-11e8-98d0-529269fb1459"

        val sqlMotebehovInsert = """
            INSERT INTO MOTEBEHOV VALUES(DEFAULT, '$motebehovId', '${motebehov.opprettetDato}',
            '${motebehov.opprettetAv}', '${motebehov.aktoerId}', '${motebehov.virksomhetsnummer}', TRUE,
            '${motebehov.forklaring}', '${motebehov.tildeltEnhet}', null, null, null, null, null)
        """.trimIndent()
        jdbcTemplate.update(sqlMotebehovInsert)

        motebehov.formValues?.let {
            val sqlFormValuesInsert = """
                INSERT INTO motebehov_form_values (motebehov_uuid, form_snapshot, begrunnelse, onsker_sykmelder_deltar,
                    onsker_sykmelder_deltar_begrunnelse, onsker_tolk, tolk_sprak)
                VALUES (?, ?::jsonb, ?, ?, ?, ?, ?)
            """.trimIndent()
            jdbcTemplate.update(
                sqlFormValuesInsert,
                motebehovId,
                it.formSnapshotJSON,
                it.begrunnelse,
                it.onskerSykmelderDeltar,
                it.onskerSykmelderDeltarBegrunnelse,
                it.onskerTolk,
                it.tolkSprak
            )
        }
    }

    fun areStringsEqualAsSqlJsonbValues(jsonb1: String, jsonb2: String): Boolean {
        val sql = "SELECT ?::jsonb = ?::jsonb"
        return jdbcTemplate.queryForObject(sql, Boolean::class.java, jsonb1, jsonb2) ?: false
    }
}
