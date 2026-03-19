---
description: 'Spring Kafka-spesifikke mønstre — @KafkaListener, ContainerFactory, retry'
applyTo: "**/*Kafka*.kt,**/*Consumer*.kt,**/*Producer*.kt,**/*Listener*.kt,**/*Event*.kt"
---
<!-- Managed by esyfo-cli. Do not edit manually. Changes will be overwritten.
     For repo-specific customizations, create your own files without this header. -->

> Spring Kafka-spesifikke patterns. Disse utvider (og tar presedens over) kafka.instructions.md for Spring-spesifikke konsepter.

# Kafka — Spring Kafka Patterns

## Configuration

```yaml
# application.yml
spring:
  kafka:
    bootstrap-servers: ${KAFKA_BROKERS}
    consumer:
      group-id: ${KAFKA_CONSUMER_GROUP_ID}
      auto-offset-reset: earliest
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.apache.kafka.common.serialization.StringSerializer
    security:
      protocol: SSL
    ssl:
      trust-store-location: file:${KAFKA_TRUSTSTORE_PATH}
      key-store-location: file:${KAFKA_KEYSTORE_PATH}
```

## Consumer

```kotlin
@Component
class EventConsumer(
    private val service: EventService
) {
    private val logger = LoggerFactory.getLogger(EventConsumer::class.java)

    @KafkaListener(topics = ["\${kafka.topic.input}"], groupId = "\${spring.kafka.consumer.group-id}")
    fun consume(record: ConsumerRecord<String, String>) {
        val event = objectMapper.readValue(record.value(), EventDTO::class.java)
        logger.info("Received event: ${event.id}")

        if (service.alreadyProcessed(event.id)) {
            logger.info("Event ${event.id} already processed, skipping")
            return
        }

        service.process(event)
    }
}
```

## Producer

```kotlin
@Component
class EventProducer(
    private val kafkaTemplate: KafkaTemplate<String, String>
) {
    private val logger = LoggerFactory.getLogger(EventProducer::class.java)

    fun publish(topic: String, key: String, event: Any) {
        val payload = objectMapper.writeValueAsString(event)
        kafkaTemplate.send(topic, key, payload)
            .whenComplete { result, ex ->
                if (ex != null) {
                    logger.error("Failed to publish to $topic", ex)
                } else {
                    logger.info("Published to $topic at offset ${result.recordMetadata.offset()}")
                }
            }
    }
}
```

## Event Design

- Past tense for event names: `user_created`, `payment_processed`
- Snake_case for all field names
- Include event ID for idempotency

## Testing

```kotlin
@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = ["test-topic"])
class EventConsumerTest {
    @Autowired
    private lateinit var embeddedKafkaBroker: EmbeddedKafkaBroker

    @Test
    fun `should consume event`() {
        // Send test message and verify processing
    }
}
```

## Boundaries

### ✅ Always
- Implement idempotency — check event ID before processing
- Use NAIS-provided SSL config for Kafka connection
- Log with event ID for traceability
- Handle deserialization errors gracefully

### ⚠️ Ask First
- Creating new Kafka topics
- Changing consumer group IDs
- Modifying event schemas (breaking changes)

### 🚫 Never
- Hardcode broker URLs (use NAIS env vars)
- Skip SSL configuration
- Publish PII in event payloads without encryption
