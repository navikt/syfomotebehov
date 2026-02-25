---
name: nais-manifest
description: Generate a production-ready Nais application manifest for Kubernetes deployment
---

You are creating a Nais application manifest for deploying to Nav's Kubernetes platform.

## Required Configuration

Generate a complete Nais manifest with:

- **Application name and namespace**: `syfomotebehov` in `teamsykefravr`
- **Container image**: Use `{{image}}` placeholder (replaced by CI/CD)
- **Port**: 8080
- **Prometheus metrics**: Enabled at `/syfomotebehov/internal/prometheus`

## Health Checks

```yaml
liveness:
  path: /syfomotebehov/internal/isAlive
  initialDelay: 30
  timeout: 1
readiness:
  path: /syfomotebehov/internal/isReady
  initialDelay: 30
  timeout: 1
```

## Resources

```yaml
resources:
  requests:
    cpu: 50m
    memory: 256Mi
  limits:
    memory: 512Mi
```

## This App's Components

This app uses:

1. **PostgreSQL database** (GCP Cloud SQL)
2. **Kafka** (Aiven pool)
3. **Azure AD authentication** (for veileder/internal users)
4. **TokenX** (for arbeidstaker/arbeidsgiver external users)

## Reference

Check existing manifests in `nais/nais-dev.yaml` and `nais/nais-prod.yaml` for the current configuration.
