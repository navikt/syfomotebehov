---
description: 'Sikkerhetsstandarder — hemmeligheter, inputvalidering, accessPolicy, PII'
applyTo: "**/*"
---
<!-- Managed by esyfo-cli. Do not edit manually. Changes will be overwritten.
     For repo-specific customizations, create your own files without this header. -->

# Security — Nav

## Golden Path (sikkerhet.nav.no)

1. **Platform**: Use NAIS defaults for auth. Control secrets (never copy prod secrets locally).
2. **Scanning**: Dependabot for dependencies. Trivy for Docker images.
3. **Development**: Chainguard/Distroless base images. No sensitive data in logs (FNR, JWT tokens, connection strings). Prefer OAuth/Maskinporten for new M2M (service users are legacy).

## Network Policies

**Default Deny** — all traffic blocked unless explicitly allowed via `accessPolicy`:
```yaml
accessPolicy:
  outbound:
    rules:
      - application: user-service
        namespace: team-user
    external:
      - host: api.external.com
  inbound:
    rules:
      - application: frontend
        namespace: team-web
```

## Boundaries

### ✅ Always
- Parameterized SQL queries
- Input validation at all boundaries
- `accessPolicy` defined for every service

### ⚠️ Ask First
- Modifying `accessPolicy` in production
- Changing authentication mechanisms

### 🚫 Never
- Commit secrets to git
- Log FNR, JWT tokens, passwords, or connection strings
- Skip input validation
