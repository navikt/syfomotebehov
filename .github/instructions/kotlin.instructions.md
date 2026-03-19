---
description: 'Kotlin-standarder — val/var, data classes, extension functions, Gradle'
applyTo: "**/*.kt"
---
<!-- Managed by esyfo-cli. Do not edit manually. Changes will be overwritten.
     For repo-specific customizations, create your own files without this header. -->

> Base Kotlin standards. If a framework-specific file (Ktor/Spring Boot) is also present, its guidance takes precedence for framework-specific concerns.

# Kotlin Development Standards

## General
- Use `val` over `var` where possible
- Prefer data classes for DTOs and value objects
- Use sealed classes for representing restricted hierarchies
- Use extension functions for utility operations

## Dependency Management
- Use Gradle Version Catalog (`gradle/libs.versions.toml`) for dependency versions
- Reference dependencies via catalog aliases in `build.gradle.kts` (e.g., `libs.ktor.server.core`)
- Never hardcode dependency versions directly in `build.gradle.kts` — check the version catalog first

## Configuration Pattern

Follow the existing configuration approach in the codebase. Common patterns:
- Environment data classes with properties
- Sealed class hierarchies for multi-environment config

```kotlin
// Check existing config classes before creating new ones
// Follow whichever pattern the repo already uses
```

## Database Access

- Check `build.gradle.kts` for actual dependencies — do not assume any specific ORM
- Parameterized queries always — never string interpolation in SQL
- Use Flyway for all schema migrations
- **Follow the repo's existing data access pattern** (Repository interfaces, extension functions, etc.)

## Observability

```kotlin
// Structured logging — check existing log statements in the codebase to match the repo's pattern
private val logger = LoggerFactory.getLogger(MyClass::class.java)

// SLF4J placeholder format (always available)
logger.info("Processing event: eventId={}", eventId)
logger.error("Failed to process event: eventId={}", eventId, exception)

// If logstash-logback-encoder is on the classpath, structured fields via kv()/keyValue():
// logger.info("Processing event {}", kv("event_id", eventId))

// Prometheus metrics with Micrometer
val requestCounter = Counter.builder("http_requests_total")
    .tag("method", "GET")
    .register(meterRegistry)
```

## Error Handling
- Use Kotlin Result type or sealed classes for expected failures
- Throw exceptions only for unexpected/unrecoverable errors
- Always log errors with structured context

## Testing
- Check `build.gradle.kts` for actual test dependencies before writing tests
- Use descriptive test names: `` `should create user when valid data provided` ``
- Use MockOAuth2Server for auth testing

## Boundaries

### ✅ Always
- Follow existing patterns in the codebase for config and data access
- Add Prometheus metrics for business operations
- Use Flyway for database migrations

### ⚠️ Ask First
- Changing database schema
- Modifying Kafka event schemas
- Adding new dependencies
- Changing authentication configuration

### 🚫 Never
- Skip database migration versioning
- Bypass authentication checks
- Use `!!` operator without null checks
- Commit configuration secrets
- Use string interpolation in SQL
- Hardcode dependency versions in `build.gradle.kts` (use version catalog)
