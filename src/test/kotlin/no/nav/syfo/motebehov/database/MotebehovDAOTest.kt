package no.nav.syfo.motebehov.database

import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.syfo.IntegrationTest
import no.nav.syfo.LocalApplication
import no.nav.syfo.motebehov.MotebehovInnmelderType
import no.nav.syfo.motebehov.formSnapshot.MOCK_ARBEIDSGIVER_SVAR_ONSKER_SYKMELDER_BEGRUNNELSE
import no.nav.syfo.motebehov.formSnapshot.MOCK_ARBEIDSGIVER_SVAR_SPRAK
import no.nav.syfo.motebehov.formSnapshot.MOCK_ARRBEIDSGIVER_SVAR_BEGRUNNELSE
import no.nav.syfo.motebehov.formSnapshot.convertFormSnapshotToJsonString
import no.nav.syfo.testhelper.UserConstants.ARBEIDSTAKER_AKTORID
import no.nav.syfo.testhelper.UserConstants.LEDER_AKTORID
import no.nav.syfo.testhelper.UserConstants.VIRKSOMHETSNUMMER
import no.nav.syfo.testhelper.generator.MotebehovGenerator
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.test.context.jdbc.Sql
import java.sql.ResultSet
import java.time.LocalDateTime
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
            it("should create møtebehov and retrieve it back with same values using hentMotebehovListeForAktoer") {
                val pMotebehov = motebehovGenerator.generatePmotebehov()

                val uuid = motebehovDAO.create(pMotebehov)
                dbUpdateOpprettetDato(uuid.toString(), pMotebehov.opprettetDato)

                val motebehovListe = motebehovDAO.hentMotebehovListeForAktoer(ARBEIDSTAKER_AKTORID)
                motebehovListe.size shouldBe 1
                val motebehovFraDb = motebehovListe[0]

                motebehovFraDb.uuid shouldBe uuid
                motebehovFraDb.opprettetDato.truncatedTo(ChronoUnit.SECONDS) shouldBe
                    pMotebehov.opprettetDato.truncatedTo(ChronoUnit.SECONDS)
                motebehovFraDb.opprettetAv shouldBe pMotebehov.opprettetAv
                motebehovFraDb.aktoerId shouldBe pMotebehov.aktoerId
                motebehovFraDb.virksomhetsnummer shouldBe pMotebehov.virksomhetsnummer
                motebehovFraDb.sykmeldtFnr shouldBe pMotebehov.sykmeldtFnr
                motebehovFraDb.harMotebehov shouldBe pMotebehov.harMotebehov
                motebehovFraDb.forklaring shouldBe (pMotebehov.forklaring ?: "") // "" is stored instead of null in the
                // database, this was probably not intended. We won't use forklaring for new motebehov with formSnapshot
                // though.
                motebehovFraDb.tildeltEnhet shouldBe pMotebehov.tildeltEnhet
                motebehovFraDb.skjemaType shouldBe pMotebehov.skjemaType
                motebehovFraDb.innmelderType shouldBe pMotebehov.innmelderType
                motebehovFraDb.formSnapshot shouldBe pMotebehov.formSnapshot
            }

            it("hentMotebehovListeForOgOpprettetAvArbeidstakerIkkeGyldig") {
                val pMotebehov = motebehovGenerator.generatePmotebehov().copy(
                    opprettetDato = motebehovGenerator.getOpprettetDato(false),
                    opprettetAv = ARBEIDSTAKER_AKTORID,
                    innmelderType = MotebehovInnmelderType.ARBEIDSTAKER
                )

                val uuid = motebehovDAO.create(pMotebehov)
                dbUpdateOpprettetDato(uuid.toString(), pMotebehov.opprettetDato)

                val motebehovListe = motebehovDAO.hentMotebehovListeForOgOpprettetAvArbeidstaker(ARBEIDSTAKER_AKTORID)
                motebehovListe.size shouldBe 0
            }

            it("Hent møtebehov liste for og opprettet av arbeidstaker") {
                val pMotebehov = motebehovGenerator.generatePmotebehov().copy(
                    opprettetDato = motebehovGenerator.getOpprettetDato(true),
                    opprettetAv = ARBEIDSTAKER_AKTORID,
                    innmelderType = MotebehovInnmelderType.ARBEIDSTAKER
                )

                val uuid = motebehovDAO.create(pMotebehov)
                dbUpdateOpprettetDato(uuid.toString(), pMotebehov.opprettetDato)

                val motebehovListe = motebehovDAO.hentMotebehovListeForOgOpprettetAvArbeidstaker(ARBEIDSTAKER_AKTORID)
                motebehovListe.size shouldBe 1
                val motebehovFraDb = motebehovListe[0]

                motebehovFraDb.opprettetDato.truncatedTo(ChronoUnit.SECONDS) shouldBe
                    pMotebehov.opprettetDato.truncatedTo(ChronoUnit.SECONDS)
                motebehovFraDb.opprettetAv shouldBe pMotebehov.opprettetAv
                motebehovFraDb.aktoerId shouldBe pMotebehov.aktoerId
                motebehovFraDb.innmelderType shouldBe pMotebehov.innmelderType
            }

            it("hentMotebehovListeForOgOpprettetAvLederIkkeGyldig") {
                val pMotebehov = motebehovGenerator.generatePmotebehov().copy(
                    opprettetDato = motebehovGenerator.getOpprettetDato(false),
                    opprettetAv = LEDER_AKTORID
                )
                val uuid = motebehovDAO.create(pMotebehov)
                dbUpdateOpprettetDato(uuid.toString(), pMotebehov.opprettetDato)

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
                    opprettetAv = LEDER_AKTORID,
                )
                val uuid = motebehovDAO.create(pMotebehov)
                dbUpdateOpprettetDato(uuid.toString(), pMotebehov.opprettetDato)

                val motebehovListe = motebehovDAO.hentMotebehovListeForArbeidstakerOpprettetAvLeder(
                    ARBEIDSTAKER_AKTORID,
                    false,
                    VIRKSOMHETSNUMMER
                )
                motebehovListe.size shouldBe 1
                val motebehovFraDb = motebehovListe[0]

                motebehovFraDb.opprettetDato.truncatedTo(ChronoUnit.SECONDS) shouldBe
                    pMotebehov.opprettetDato.truncatedTo(ChronoUnit.SECONDS)
                motebehovFraDb.opprettetAv shouldBe LEDER_AKTORID
                motebehovFraDb.aktoerId shouldBe pMotebehov.aktoerId
                motebehovFraDb.innmelderType shouldBe pMotebehov.innmelderType
            }

            it("skalHenteAlleMotebehovForAktorDersomEgenLeder") {
                val pMotebehov = motebehovGenerator.generatePmotebehov().copy(
                    opprettetDato = motebehovGenerator.getOpprettetDato(true),
                    opprettetAv = ARBEIDSTAKER_AKTORID,
                    innmelderType = MotebehovInnmelderType.ARBEIDSTAKER,
                )
                val uuid = motebehovDAO.create(pMotebehov)
                dbUpdateOpprettetDato(uuid.toString(), pMotebehov.opprettetDato)

                val motebehovListe = motebehovDAO.hentMotebehovListeForArbeidstakerOpprettetAvLeder(
                    ARBEIDSTAKER_AKTORID,
                    true,
                    VIRKSOMHETSNUMMER
                )
                motebehovListe.size shouldBe 1
                val motebehovFraDb = motebehovListe[0]

                motebehovFraDb.opprettetDato.truncatedTo(ChronoUnit.SECONDS) shouldBe
                    pMotebehov.opprettetDato.truncatedTo(ChronoUnit.SECONDS)
                motebehovFraDb.opprettetAv shouldBe ARBEIDSTAKER_AKTORID
                motebehovFraDb.aktoerId shouldBe pMotebehov.aktoerId
                motebehovFraDb.innmelderType shouldBe pMotebehov.innmelderType
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
                val motebehovFormValuesFromDb = dbSelectMotebehovFormValues(uuid.toString())

                motebehovFormValuesFromDb.shouldNotBeNull()

                areStringsEqualAsSqlJsonbValues(
                    motebehovFormValuesFromDb.formSnapshotJSON,
                    motebehovToStoreFormSnapshotConvertedToJSON ?: ""
                ) shouldBe true

                motebehovFormValuesFromDb.formIdentifier shouldBe
                    motebehovToStore.formSnapshot?.formIdentifier?.identifier
                motebehovFormValuesFromDb.formSemanticVersion shouldBe
                    motebehovToStore.formSnapshot?.formSemanticVersion
                motebehovFormValuesFromDb.begrunnelse shouldBe MOCK_ARRBEIDSGIVER_SVAR_BEGRUNNELSE
                motebehovFormValuesFromDb.onskerSykmelderDeltar shouldBe true
                motebehovFormValuesFromDb.onskerSykmelderDeltarBegrunnelse shouldBe
                    MOCK_ARBEIDSGIVER_SVAR_ONSKER_SYKMELDER_BEGRUNNELSE
                motebehovFormValuesFromDb.onskerTolk shouldBe true
                motebehovFormValuesFromDb.tolkSprak shouldBe MOCK_ARBEIDSGIVER_SVAR_SPRAK
            }
        }
    }

    /**
     * MotebehovDAO.create() sets opprettetDato to the current time, so this function can be used after create()
     * to update the opprettetDato to a specific time.
     */
    private fun dbUpdateOpprettetDato(motebehovUUID: String, opprettetDato: LocalDateTime) {
        val updateOprrettetDatoSql = "UPDATE motebehov SET opprettet_dato = ? WHERE motebehov_uuid = ?"
        jdbcTemplate.update(updateOprrettetDatoSql, opprettetDato, motebehovUUID)
    }

    private fun dbSelectMotebehovFormValues(motebehovId: String): PMotebehovFormValues? {
        val getRowIdFromMotebehovSql = "SELECT id FROM motebehov WHERE motebehov_uuid = ?"
        val motebehovRowId = jdbcTemplate.queryForObject(getRowIdFromMotebehovSql, Long::class.java, motebehovId)

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

        val motebehovFormValuesSql = "SELECT * FROM motebehov_form_values WHERE motebehov_row_id = ?"
        return jdbcTemplate.queryForObject(motebehovFormValuesSql, motebehovFormValuesRowMapper, motebehovRowId)
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
