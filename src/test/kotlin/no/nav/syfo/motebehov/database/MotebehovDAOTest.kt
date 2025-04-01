package no.nav.syfo.motebehov.database

import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.syfo.IntegrationTest
import no.nav.syfo.LocalApplication
import no.nav.syfo.motebehov.extractFormValuesFromFormSnapshot
import no.nav.syfo.motebehov.formSnapshot.FORM_IDENTIFIER_ARBEIDSTAKER_SVAR
import no.nav.syfo.motebehov.formSnapshot.MOCK_ARBEIDSTAKER_SVAR_SPRAK
import no.nav.syfo.motebehov.formSnapshot.MOCK_ARRBEIDSTAKER_SVAR_BEGRUNNELSE
import no.nav.syfo.motebehov.formSnapshot.MOCK_SNAPSHOTS_FORM_SEMANTIC_VERSION
import no.nav.syfo.motebehov.formSnapshot.convertFormSnapshotToJsonString
import no.nav.syfo.motebehov.formSnapshot.mockArbeidstakerSvarJaFormSnapshot
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
import org.springframework.jdbc.core.RowMapper
import org.springframework.test.context.jdbc.Sql
import java.sql.ResultSet
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
            it("Hent møtebehov liste for aktør") {
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
                motebehovFraDb.formSnapshot shouldBe pMotebehov.formSnapshot
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

            it("should create møtebehov and retrieve it back with same values") {
                val motebehovToStore = motebehovGenerator.generatePmotebehov()
                val uuid = motebehovDAO.create(motebehovToStore)
                val motebehovListe = motebehovDAO.hentMotebehovListeForAktoer(ARBEIDSTAKER_AKTORID)
                motebehovListe.size shouldBe 1

                val retrievedMotebehov = motebehovListe[0]

                retrievedMotebehov.uuid shouldBe uuid
                retrievedMotebehov.harMotebehov shouldBe true
                retrievedMotebehov.aktoerId shouldBe ARBEIDSTAKER_AKTORID
                retrievedMotebehov.sykmeldtFnr shouldBe ARBEIDSTAKER_FNR

                retrievedMotebehov.formSnapshot.shouldNotBeNull()
                retrievedMotebehov.formSnapshot shouldBe mockArbeidstakerSvarJaFormSnapshot
            }

            it(
                "should store the correct values in motebehov_form_values when creating a motebehov " +
                    "with a formSnapshot"
            ) {
                val motebehovToStore = motebehovGenerator.generatePmotebehov()
                val motebehovToStoreFormSnapshotConvertedToJSON = motebehovToStore.formSnapshot?.let {
                    convertFormSnapshotToJsonString(
                        it
                    )
                }

                val uuid = motebehovDAO.create(motebehovToStore)
                val motebehovFormValuesFromDb = readMotebehovFormValuesFromDb(uuid.toString())

                motebehovFormValuesFromDb.shouldNotBeNull()

                areStringsEqualAsSqlJsonbValues(
                    motebehovFormValuesFromDb.formSnapshotJSON,
                    motebehovToStoreFormSnapshotConvertedToJSON ?: ""
                ) shouldBe true

                motebehovFormValuesFromDb.formIdentifier shouldBe FORM_IDENTIFIER_ARBEIDSTAKER_SVAR
                motebehovFormValuesFromDb.formSemanticVersion shouldBe MOCK_SNAPSHOTS_FORM_SEMANTIC_VERSION
                motebehovFormValuesFromDb.begrunnelse shouldBe MOCK_ARRBEIDSTAKER_SVAR_BEGRUNNELSE
                motebehovFormValuesFromDb.onskerSykmelderDeltar shouldBe false
                motebehovFormValuesFromDb.onskerSykmelderDeltarBegrunnelse.shouldBeNull()
                motebehovFormValuesFromDb.onskerTolk shouldBe true
                motebehovFormValuesFromDb.tolkSprak shouldBe MOCK_ARBEIDSTAKER_SVAR_SPRAK
            }
        }
    }

    private fun insertPMotebehov(motebehov: PMotebehov) {
        val motebehovId = "bae778f2-a085-11e8-98d0-529269fb1459"

        val sqlMotebehovInsert = """
            INSERT INTO MOTEBEHOV (id, motebehov_uuid, opprettet_dato, opprettet_av, aktoer_id, virksomhetsnummer,
                har_motebehov, forklaring, tildelt_enhet, behandlet_tidspunkt, behandlet_veileder_ident, skjematype,
                sm_fnr, opprettet_av_fnr)
            VALUES (DEFAULT, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? ,?, ?, ?)
        """.trimIndent()
        jdbcTemplate.update(
            sqlMotebehovInsert,
            motebehovId,
            motebehov.opprettetDato,
            motebehov.opprettetAv,
            motebehov.aktoerId,
            motebehov.virksomhetsnummer,
            motebehov.harMotebehov,
            motebehov.forklaring,
            motebehov.tildeltEnhet,
            motebehov.behandletTidspunkt,
            motebehov.behandletVeilederIdent,
            motebehov.skjemaType,
            motebehov.sykmeldtFnr,
            motebehov.opprettetAvFnr
        )

        val formSnapshot = motebehov.formSnapshot

        formSnapshot?.let {
            val formSnapshotJSON = convertFormSnapshotToJsonString(formSnapshot)
            val formValues = extractFormValuesFromFormSnapshot(formSnapshot)

            val sqlFormValuesInsert = """
                INSERT INTO motebehov_form_values (motebehov_uuid, form_identifier, form_semantic_version,
                    form_snapshot, begrunnelse, onsker_sykmelder_deltar, onsker_sykmelder_deltar_begrunnelse,
                    onsker_tolk, tolk_sprak)
                VALUES (?, ?, ?, ?::jsonb, ?, ?, ?, ?, ?)
            """.trimIndent()
            jdbcTemplate.update(
                sqlFormValuesInsert,
                motebehovId,
                formValues.formIdentifier,
                formValues.formSemanticVersion,
                formSnapshotJSON,
                formValues.begrunnelse,
                formValues.onskerSykmelderDeltar,
                formValues.onskerSykmelderDeltarBegrunnelse,
                formValues.onskerTolk,
                formValues.tolkSprak
            )
        }
    }

    private fun readMotebehovFormValuesFromDb(motebehovId: String): PMotebehovFormValues? {
        val motebehovFormValuesRowMapper: RowMapper<PMotebehovFormValues> = RowMapper { rs: ResultSet, _: Int ->
            PMotebehovFormValues(
                formIdentifier = rs.getString("form_identifier"),
                formSemanticVersion = rs.getString("form_semantic_version"),
                formSnapshotJSON = rs.getString("form_snapshot"),
                begrunnelse = rs.getString("begrunnelse")?.takeIf { it.isNotEmpty() },
                onskerSykmelderDeltar = rs.getBoolean("onsker_sykmelder_deltar"),
                onskerSykmelderDeltarBegrunnelse = rs.getString(
                    "onsker_sykmelder_deltar_begrunnelse"
                )?.takeIf { it.isNotEmpty() },
                onskerTolk = rs.getBoolean("onsker_tolk"),
                tolkSprak = rs.getString("tolk_sprak")?.takeIf { it.isNotEmpty() },
            )
        }

        val sql = "SELECT * FROM motebehov_form_values WHERE motebehov_uuid = ?"
        return jdbcTemplate.queryForObject(sql, motebehovFormValuesRowMapper, motebehovId)
    }

    fun areStringsEqualAsSqlJsonbValues(jsonb1: String, jsonb2: String): Boolean {
        val sql = "SELECT ?::jsonb = ?::jsonb"
        return jdbcTemplate.queryForObject(sql, Boolean::class.java, jsonb1, jsonb2) ?: false
    }

    companion object {
        // Beside formSnapshotJSON, these fields are not read out of the database (in this application).
        // That is why this data class is only defined inside of this test class.
        data class PMotebehovFormValues(
            val formSnapshotJSON: String,
            val formIdentifier: String,
            val formSemanticVersion: String,
            val begrunnelse: String?,
            val onskerSykmelderDeltar: Boolean,
            val onskerSykmelderDeltarBegrunnelse: String?,
            val onskerTolk: Boolean,
            val tolkSprak: String?,
        )
    }
}
