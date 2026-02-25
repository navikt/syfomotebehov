---
name: observability-setup
description: Setting up Prometheus metrics, structured logging, and health endpoints for this Spring Boot app
---

# Observability Setup Skill

Patterns for observability in this Spring Boot / NAIS application.

## Health Endpoints

Managed by Spring Boot Actuator. Configured in `application.yaml`:

```yaml
management:
  endpoints.web:
    base-path: /internal
    exposure.include: prometheus
    path-mapping.prometheus: /prometheus
```

## Prometheus Metrics

Uses Micrometer with Prometheus registry (Spring Boot Actuator auto-configuration).

### Custom Business Metrics

This app uses a `Metric` bean for tracking:

```kotlin
@Component
class Metric(private val registry: MeterRegistry) {
    fun countEndpointRequest(endpoint: String) {
        registry.counter("endpoint_request", "endpoint", endpoint).increment()
    }
}
```

## Structured Logging

Uses Logstash JSON encoder:

```kotlin
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(MyClass::class.java)

log.info("Processing motebehov for virksomhet")
log.error("Failed to process motebehov", exception)
```

## Alert Configuration

Alerts in `nais/alerts-gcp.yaml`, notifications to `#veden-alerts` Slack channel.
