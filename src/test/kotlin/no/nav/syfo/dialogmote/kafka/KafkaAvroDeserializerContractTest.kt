package no.nav.syfo.dialogmote.kafka

import io.confluent.kafka.schemaregistry.client.MockSchemaRegistryClient
import io.confluent.kafka.serializers.KafkaAvroDeserializer
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig
import io.confluent.kafka.serializers.KafkaAvroSerializer
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import no.nav.syfo.dialogmote.avro.KDialogmoteStatusEndring
import java.time.Instant

class KafkaAvroDeserializerContractTest :
    DescribeSpec({
        describe("KafkaAvroDeserializer") {
            it("deserializes KDialogmoteStatusEndring as a specific Avro record") {
                val schemaRegistryClient = MockSchemaRegistryClient()
                val serializer =
                    KafkaAvroSerializer(
                        schemaRegistryClient,
                        mapOf(KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG to "mock://dialogmote"),
                    )
                val deserializer =
                    KafkaAvroDeserializer(
                        schemaRegistryClient,
                        mapOf(
                            KafkaAvroDeserializerConfig.SCHEMA_REGISTRY_URL_CONFIG to "mock://dialogmote",
                            KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG to true,
                        ),
                    )
                val expected =
                    KDialogmoteStatusEndring(
                        "00000000-0000-0000-0000-000000000001",
                        Instant.parse("2026-01-15T10:00:00Z"),
                        "INNKALT",
                        Instant.parse("2026-01-01T09:00:00Z"),
                        "synthetic-person-ident",
                        "synthetic-virksomhetsnummer",
                        "synthetic-enhet",
                        "synthetic-nav-ident",
                        Instant.parse("2025-12-01T00:00:00Z"),
                        true,
                        false,
                        false,
                    )

                val serialized = serializer.serialize("synthetic-dialogmote-status", expected)
                val deserialized = deserializer.deserialize("synthetic-dialogmote-status", serialized)
                deserialized.javaClass shouldBe KDialogmoteStatusEndring::class.java
                val actual = deserialized as KDialogmoteStatusEndring

                actual.getDialogmoteUuid() shouldBe expected.getDialogmoteUuid()
                actual.getDialogmoteTidspunkt() shouldBe expected.getDialogmoteTidspunkt()
                actual.getStatusEndringType() shouldBe expected.getStatusEndringType()
                actual.getPersonIdent() shouldBe expected.getPersonIdent()
                actual.getVirksomhetsnummer() shouldBe expected.getVirksomhetsnummer()
                actual.getArbeidstaker() shouldBe expected.getArbeidstaker()
            }
        }
    })
