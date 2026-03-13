---
applyTo: "**/*"
---
<!-- Managed by esyfo-cli. Do not edit manually. Changes will be overwritten.
     For repo-specific customizations, create your own files without this header. -->

# Authentication Standards

## Authentication Types in NAV

### 1. Azure AD / Entra ID (Internal NAV Users)
```yaml
azure:
  application:
    enabled: true
    tenant: nav.no
```
Env vars: `AZURE_APP_CLIENT_ID`, `AZURE_APP_CLIENT_SECRET`, `AZURE_APP_WELL_KNOWN_URL`, `AZURE_OPENID_CONFIG_JWKS_URI`

### 2. TokenX (Service-to-Service, on-behalf-of)
```yaml
tokenx:
  enabled: true
accessPolicy:
  inbound:
    rules:
      - application: calling-service
        namespace: team-calling
```
Env vars: `TOKEN_X_WELL_KNOWN_URL`, `TOKEN_X_CLIENT_ID`, `TOKEN_X_PRIVATE_JWK`

### 3. ID-porten (Citizens)
```yaml
idporten:
  enabled: true
  sidecar:
    enabled: true
    level: Level4
```

### 4. Maskinporten (External Organizations)
```yaml
maskinporten:
  enabled: true
  scopes:
    consumes:
      - name: "nav:example/scope"
```

## Approach

1. Les NAIS-manifest for å identifisere hvilke auth-mekanismer som er konfigurert
2. Søk i kodebasen etter eksisterende auth-oppsett og følg samme mønster
3. Se språkspesifikke auth-instruksjoner for bibliotekdetaljer

## Referanse

Komplett auth-dokumentasjon: https://doc.nais.io/auth/

## Boundaries

### ✅ Always
- Validate JWT issuer, audience, expiration, and signature
- Use HTTPS only for token transmission
- Define explicit `accessPolicy` in NAIS manifest
- Use env vars from NAIS (never hardcode)

### ⚠️ Ask First
- Changing access policies in production
- Modifying token validation rules

### 🚫 Never
- Hardcode client secrets or tokens
- Log full JWT tokens (or any part of token payload containing PII)
- Skip token validation
- Store tokens in localStorage
