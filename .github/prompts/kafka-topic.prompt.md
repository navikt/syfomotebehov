---
description: Set up a Kafka topic and consumer for team-esyfo
---
<!-- Managed by esyfo-cli. Do not edit manually. Changes will be overwritten.
     For repo-specific customizations, create your own files without this header. -->

# Set Up Kafka Topic and Consumer

Create a new Kafka topic configuration and consumer.

## Steps

1. Read existing NAIS manifest for Kafka pool configuration
2. Search codebase for existing Kafka consumer implementations to follow established patterns
3. Check build.gradle.kts for Kafka dependencies and inspect existing consumer/producer implementations

## Checklist

- [ ] Add Kafka pool to NAIS manifest (if not already present)
- [ ] Create consumer class following existing patterns in the repo
- [ ] Define message payload and key types matching the topic schema
- [ ] Implement idempotent processing where needed
- [ ] Add error handling and logging consistent with existing consumers
- [ ] Add metrics (events processed counter, processing duration timer)
- [ ] Add structured logging with relevant identifiers
- [ ] Write tests following existing Kafka test patterns in the repo
