---
description: 'Autentisering i Kotlin med Texas-sidecar — token, exchange, introspect'
applyTo: "**/*.kt"
---
<!-- Managed by esyfo-cli. Do not edit manually. Changes will be overwritten.
     For repo-specific customizations, create your own files without this header. -->

# Authentication — Kotlin (Texas sidecar)

Bruk Texas-sidecaren for token-operasjoner fra Kotlin-backends. Texas kjører som sidecar i Kubernetes og eksponerer HTTP-endepunkter på `localhost:3000`.

Sjekk eksisterende kode i repoet for etablert auth-mønster før du legger til nytt.

## Token (M2M)

```http
POST http://localhost:3000/api/v1/token
Content-Type: application/json

{ "identity_provider": "azuread", "target": "api://cluster.namespace.app/.default" }
```

For Maskinporten: `"identity_provider": "maskinporten", "target": "nav:scope"`

## Token Exchange (OBO)

```http
POST http://localhost:3000/api/v1/token/exchange
Content-Type: application/json

{ "identity_provider": "tokenx", "target": "cluster:namespace:app", "user_token": "<token>" }
```

## Introspect (validering)

```http
POST http://localhost:3000/api/v1/introspect
Content-Type: application/json

{ "identity_provider": "tokenx", "token": "<token>" }
```

Respons: `{ "active": true/false, "sub": "...", "exp": ..., ... }`

## Respons-format

**Suksess (token-endepunkter):**
```json
{ "access_token": "<jwt>", "token_type": "Bearer", "expires_in": 3599 }
```

**Feil:**
```json
{ "error": "invalid_request", "error_description": "..." }
```

## Caching

Texas cacher tokens automatisk med smart TTL (60s preemptiv refresh). Bruk `"skip_cache": true` i request-body for å tvinge fornyelse.

## Boundaries

### ✅ Always
- Håndter feilrespons fra Texas (`error`-feltet)
- Bruk `identity_provider`-feltet korrekt for riktig flyt

### 🚫 Never
- Hardkode token-endepunkt-URL (bruk alltid `localhost:3000`)
- Implementer egen token-caching (Texas håndterer dette)
