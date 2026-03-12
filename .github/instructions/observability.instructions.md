---
applyTo: "**/*"
---
<!-- Managed by esyfo-cli. Do not edit manually. Changes will be overwritten.
     For repo-specific customizations, create your own files without this header. -->

# Observability Standards

## NAV Observability Stack
- **Prometheus**: Metrics collection (pull-based scraping)
- **Grafana**: Visualization (https://grafana.nav.cloud.nais.io)
- **Grafana Loki**: Log aggregation
- **Grafana Tempo**: Distributed tracing (OpenTelemetry)
- **Alert Manager**: Alert routing (Slack integration)

## Health Endpoints
NAIS apps expose health and metrics endpoints (paths vary per repo — check existing NAIS manifests and application config).

## Metric Naming
- Use `snake_case` with unit suffix (`_seconds`, `_bytes`, `_total`)
- Add `_total` suffix to counters
- Avoid high-cardinality labels (`user_id`, `email`, `transaction_id`)

## Structured Logging
Log to stdout/stderr with structured JSON. Follow the existing logging pattern in the codebase — look for structured argument patterns, helpers, or context propagation already in use.

## Boundaries

### ✅ Always
- Use snake_case for metric names with unit suffix
- Ensure health and metrics endpoints exist
- Log to stdout/stderr (not files)
- Use structured logging (JSON)
- Include `trace_id` in logs

### ⚠️ Ask First
- Changing alert thresholds in production
- Adding new metric labels (cardinality impact)

### 🚫 Never
- Use high-cardinality labels
- Log sensitive data (PII, tokens, passwords)
- Skip exposing a Prometheus metrics scrape endpoint
- Use camelCase for metric names
