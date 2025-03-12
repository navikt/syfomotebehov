package no.nav.syfo.motebehov.database

import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.shouldBe
import no.nav.syfo.IntegrationTest
import no.nav.syfo.LocalApplication
import no.nav.syfo.testhelper.UserConstants.ARBEIDSTAKER_AKTORID
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
            it("Hent møtebehov liste for aktør") {
                val pMotebehov = motebehovGenerator.generatePmotebehov()
//                insertPMotebehov(pMotebehov)
                motebehovDAO.create(pMotebehov)
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

                motebehovFraDb.motebehovFormValues?.formSnapshotJSON shouldBe
                    pMotebehov.motebehovFormValues?.formSnapshotJSON
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

            it("lagre møtebehov") {
                val uuid = motebehovDAO.create(motebehovGenerator.generatePmotebehov())
                val motebehovListe = motebehovDAO.hentMotebehovListeForAktoer(ARBEIDSTAKER_AKTORID)
                motebehovListe.size shouldBe 1
                motebehovListe[0].uuid shouldBe uuid
            }
        }
    }

    private fun insertPMotebehov(motebehov: PMotebehov) {
        val sqlInsert =
            "INSERT INTO MOTEBEHOV VALUES(DEFAULT, 'bae778f2-a085-11e8-98d0-529269fb1459', '" + motebehov.opprettetDato + "', '" + motebehov.opprettetAv + "', '" + motebehov.aktoerId + "', '" + motebehov.virksomhetsnummer + "', '" + '1' + "', '" + motebehov.forklaring + "', '" + motebehov.tildeltEnhet + "', null, null, null, null, null)"
        jdbcTemplate.update(sqlInsert)
    }
}
