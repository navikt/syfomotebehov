---
applyTo: "**/.nais/**/*.yaml,**/.nais/**/*.yml,**/nais/**/*.yaml,**/nais/**/*.yml,**/nais.yaml,**/nais.yml"
---
<!-- Managed by esyfo-cli. Do not edit manually. Changes will be overwritten.
     For repo-specific customizations, create your own files without this header. -->

# NAIS Platform Standards

## NAIS Manifest Structure

```yaml
apiVersion: nais.io/v1alpha1
kind: Application
metadata:
  name: app-name
  namespace: team-esyfo
  labels:
    team: team-esyfo
spec:
  image: {{ image }}
  port: 8080  # Check existing manifests — varies per repo (e.g. 3000 for frontend)

  # Paths vary per repo — check existing manifests for actual values
  prometheus:
    enabled: true
    path: /metrics  # or /internal/prometheus
  liveness:
    path: /isalive  # or /internal/health/livenessState
    initialDelay: 5
  readiness:
    path: /isready  # or /internal/health/readinessState
    initialDelay: 5

  resources:
    requests:
      cpu: 50m
      memory: 256Mi
    limits:
      memory: 512Mi
```

## Adding PostgreSQL Database

```yaml
gcp:
  sqlInstances:
    - type: POSTGRES_15  # Check repo's existing manifests for actual version
      databases:
        - name: myapp-db
          envVarPrefix: DB
```

Provides: `DB_HOST`, `DB_PORT`, `DB_DATABASE`, `DB_USERNAME`, `DB_PASSWORD`

## Configuring Kafka

```yaml
kafka:
  pool: nav-dev  # or nav-prod
```

## Azure AD Authentication

```yaml
azure:
  application:
    enabled: true
    tenant: nav.no
```

## TokenX for Service-to-Service

```yaml
tokenx:
  enabled: true

accessPolicy:
  inbound:
    rules:
      - application: calling-app
        namespace: calling-namespace
  outbound:
    rules:
      - application: downstream-app
        namespace: downstream-namespace
```

## Ingress

```yaml
ingresses:
  - https://myapp.intern.dev.nav.no   # Internal dev
  - https://myapp.dev.nav.no          # External dev
```

## Scaling

```yaml
replicas:
  min: 2
  max: 4
  cpuThresholdPercentage: 80
```

## Auto-Instrumentation (Observability)

```yaml
spec:
  observability:
    autoInstrumentation:
      enabled: true
      runtime: java  # or nodejs, python
```

## Boundaries

### ✅ Always
- Include liveness, readiness, and metrics endpoints
- Set memory limits
- Define explicit `accessPolicy`
- Use environment-specific manifests (`app-dev.yaml`, `app-prod.yaml`)

### ⚠️ Ask First
- Changing production resource limits or replicas
- Adding new GCP resources (cost implications)
- Modifying network policies
- Changing CPU limits (can cause throttling — follow existing manifests)

### 🚫 Never
- Store secrets in Git
- Deploy without CI/CD pipeline
- Skip health endpoints
