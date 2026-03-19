---
description: Sett opp autentisering i en Nav-applikasjon — Azure AD, TokenX, ID-porten, Maskinporten konfigurering og beste praksis
---
<!-- Managed by esyfo-cli. Do not edit manually. Changes will be overwritten.
     For repo-specific customizations, create your own files without this header. -->

# Authentication Overview — Nav

Oversikt over autentiseringsmekanismer i Nav. Bruk denne som referanse ved oppsett av auth i nye eller eksisterende tjenester.

## Autentiseringstyper

### 1. Azure AD / Entra ID (interne Nav-brukere)
```yaml
azure:
  application:
    enabled: true
    tenant: nav.no
```
Env vars: `AZURE_APP_CLIENT_ID`, `AZURE_APP_CLIENT_SECRET`, `AZURE_APP_WELL_KNOWN_URL`, `AZURE_OPENID_CONFIG_JWKS_URI`

### 2. TokenX (service-to-service, on-behalf-of)
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

### 3. ID-porten (innbyggere)
```yaml
idporten:
  enabled: true
  sidecar:
    enabled: true
    level: Level4
```

### 4. Maskinporten (eksterne organisasjoner)
```yaml
maskinporten:
  enabled: true
  scopes:
    consumes:
      - name: "nav:example/scope"
```

### 5. Systembruker via Maskinporten (Altinn 3)

Systembruker er en mekanisme i Altinn 3 der eksterne virksomheter oppretter en systembruker som gir tilgang til Nav-tjenester via Maskinporten. Brukes blant annet i syfo-dokumentporten.

Se [Altinn 3 systembruker-dokumentasjon](https://docs.altinn.studio/authentication/what-do-you-get/systemuser/) for oppsett.

## Tilnærming

1. Les NAIS-manifest for å identifisere hvilke auth-mekanismer som er konfigurert
2. Søk i kodebasen etter eksisterende auth-oppsett og følg samme mønster
3. Se språkspesifikke auth-instruksjoner (`.github/instructions/auth-kotlin.instructions.md` eller `auth-typescript.instructions.md`) for bibliotekdetaljer

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
