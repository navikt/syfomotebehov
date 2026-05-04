package no.nav.syfo.dialogmotekandidat

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.extensions.ApplyExtension
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.mockk
import io.mockk.verify
import no.nav.syfo.IntegrationTest
import no.nav.syfo.LocalApplication
import no.nav.syfo.dialogmotekandidat.database.DialogmotekandidatVarselStatusDao
import no.nav.syfo.dialogmotekandidat.database.DialogmotekandidatVarselType
import no.nav.syfo.dialogmotekandidat.kafka.DialogmotekandidatListener
import no.nav.syfo.dialogmotekandidat.kafka.KafkaDialogmotekandidatDeserializer
import no.nav.syfo.dialogmotekandidat.kafka.KafkaDialogmotekandidatEndring
import no.nav.syfo.testhelper.UserConstants
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.errors.SerializationException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.kafka.support.Acknowledgment
import org.springframework.test.context.ActiveProfiles
import java.time.OffsetDateTime
import java.time.ZoneId
import java.util.UUID

@TestConfiguration
@ActiveProfiles("local")
@SpringBootTest(classes = [LocalApplication::class])
@ApplyExtension(SpringExtension::class)
internal class DialogmotekandidatKafkaIntegrationTest : IntegrationTest() {
    @Autowired
    private lateinit var dialogmotekandidatService: DialogmotekandidatService

    @Autowired
    private lateinit var dialogmotekandidatVarselStatusDao: DialogmotekandidatVarselStatusDao

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    private val listener by lazy { DialogmotekandidatListener(dialogmotekandidatService) }

    init {
        beforeTest {
            jdbcTemplate.update("DELETE FROM DIALOGMOTEKANDIDAT")
            jdbcTemplate.update("DELETE FROM dialogkandidat_varsel_status")
        }

        describe("DialogmotekandidatListener") {
            it("oppretter PENDING VARSEL-rad for kandidat-melding og ack-er") {
                val ack = mockk<Acknowledgment>(relaxed = true)
                val melding = generateEndring(kandidat = true)

                listener.dialogmotekandidatEndringListener(consumerRecord(melding), ack)

                dialogmotekandidatVarselStatusDao.getPendingByType(DialogmotekandidatVarselType.VARSEL).apply {
                    shouldHaveSize(1)
                    first().kafkaMeldingUuid shouldBe melding.uuid
                    first().fnr shouldBe UserConstants.ARBEIDSTAKER_FNR
                }
                verify(exactly = 1) { ack.acknowledge() }
            }

            it("oppretter FERDIGSTILL-rad nar kandidat=false") {
                val ack = mockk<Acknowledgment>(relaxed = true)
                val melding = generateEndring(kandidat = false)

                listener.dialogmotekandidatEndringListener(consumerRecord(melding), ack)

                dialogmotekandidatVarselStatusDao.getPendingByType(DialogmotekandidatVarselType.FERDIGSTILL).apply {
                    shouldHaveSize(1)
                    first().type shouldBe DialogmotekandidatVarselType.FERDIGSTILL
                }
                verify(exactly = 1) { ack.acknowledge() }
            }

            it("oppretter kun en outbox-rad ved duplikat Kafka-melding (idempotens)") {
                val ack = mockk<Acknowledgment>(relaxed = true)
                val melding = generateEndring(kandidat = true)

                listener.dialogmotekandidatEndringListener(consumerRecord(melding, offset = 0L), ack)
                listener.dialogmotekandidatEndringListener(consumerRecord(melding, offset = 1L), ack)

                dialogmotekandidatVarselStatusDao.getPendingByType(DialogmotekandidatVarselType.VARSEL) shouldHaveSize 1
                verify(exactly = 2) { ack.acknowledge() }
            }
        }

        describe("KafkaDialogmotekandidatDeserializer") {
            it("deserialiserer gyldig JSON korrekt") {
                val createdAt = OffsetDateTime.now(ZoneId.of("Europe/Oslo")).withNano(0)
                val uuid = UUID.randomUUID().toString()
                val json =
                    """
                    {
                      "uuid": "$uuid",
                      "createdAt": "$createdAt",
                      "personIdentNumber": "${UserConstants.ARBEIDSTAKER_FNR}",
                      "kandidat": true,
                      "arsak": "STOPPUNKT"
                    }
                    """.trimIndent()

                val dto =
                    KafkaDialogmotekandidatDeserializer().deserialize(
                        DialogmotekandidatListener.DIALOGMOTEKANDIDAT_TOPIC,
                        json.toByteArray(),
                    )

                dto.uuid shouldBe uuid
                dto.kandidat shouldBe true
                dto.personIdentNumber shouldBe UserConstants.ARBEIDSTAKER_FNR
            }

            it("kaster SerializationException for ugyldig JSON (poison pill)") {
                val exception =
                    shouldThrow<SerializationException> {
                        KafkaDialogmotekandidatDeserializer().deserialize(
                            DialogmotekandidatListener.DIALOGMOTEKANDIDAT_TOPIC,
                            "this is not valid json".toByteArray(),
                        )
                    }
                exception.message shouldContain "Error when deserializing"
            }

            it("kaster SerializationException for JSON med manglende obligatoriske felter") {
                val incompleteJson = """{"uuid": "abc"}"""
                shouldThrow<SerializationException> {
                    KafkaDialogmotekandidatDeserializer().deserialize(
                        DialogmotekandidatListener.DIALOGMOTEKANDIDAT_TOPIC,
                        incompleteJson.toByteArray(),
                    )
                }
            }
        }
    }

    private fun consumerRecord(
        melding: KafkaDialogmotekandidatEndring,
        offset: Long = 0L,
    ): ConsumerRecord<String, KafkaDialogmotekandidatEndring> =
        ConsumerRecord(
            DialogmotekandidatListener.DIALOGMOTEKANDIDAT_TOPIC,
            0,
            offset,
            UserConstants.ARBEIDSTAKER_FNR,
            melding,
        )

    private fun generateEndring(kandidat: Boolean): KafkaDialogmotekandidatEndring =
        KafkaDialogmotekandidatEndring(
            uuid = UUID.randomUUID().toString(),
            createdAt = OffsetDateTime.now(ZoneId.of("Europe/Oslo")).minusMinutes(1).withNano(0),
            personIdentNumber = UserConstants.ARBEIDSTAKER_FNR,
            kandidat = kandidat,
            arsak = if (kandidat) "STOPPUNKT" else "UNNTAK",
        )
}
