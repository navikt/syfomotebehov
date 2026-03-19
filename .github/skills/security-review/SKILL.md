---
description: Sikkerhetsgjennomgang før commit/push/PR — OWASP Top 10, Dependabot, Trivy, hemmeligheter, inputvalidering
---
<!-- Managed by esyfo-cli. Do not edit manually. Changes will be overwritten.
     For repo-specific customizations, create your own files without this header. -->
# Security Review

Pre-commit og pre-PR sikkerhetssjekk for Nav-applikasjoner. Dekker secret scanning, sårbarhetsskanning og OWASP Top 10.

## Automatiserte skanninger

Kjør disse i terminalen:

```bash
# Skann repo for kjente sårbarheter og secrets
trivy repo .

# Skann Docker image for HIGH/CRITICAL CVE-er
trivy image <image-name> --severity HIGH,CRITICAL

# Skann GitHub Actions workflows for usikre mønstre
zizmor .github/workflows/

# Rask søk etter secrets i git-historikk
git log -p --all -S 'password' -- '*.kt' '*.ts' | head -100
git log -p --all -S 'secret' -- '*.kt' '*.ts' | head -100
```

## Parameteriserte SQL-spørringer

```kotlin
// ✅ Parameterisert spørring
fun findBruker(fnr: String): Bruker? =
    jdbcTemplate.queryForObject(
        "SELECT * FROM bruker WHERE fnr = ?",
        brukerRowMapper,
        fnr
    )

// ❌ SQL injection
fun findBrukerUnsafe(fnr: String): Bruker? =
    jdbcTemplate.queryForObject(
        "SELECT * FROM bruker WHERE fnr = '$fnr'",
        brukerRowMapper
    )
```

## Ingen PII i logger

```kotlin
// ✅ Logg korrelasjons-ID, ikke PII
log.info("Behandler sak for bruker", kv("sakId", sak.id), kv("tema", sak.tema))

// ❌ Aldri logg FNR, navn eller annen PII
log.info("Behandler sak for bruker ${bruker.fnr}")
```

## Secrets fra miljøvariabler

```kotlin
// ✅ Les fra miljø (Nais injiserer via Secret)
val dbPassword = System.getenv("DB_PASSWORD")
    ?: throw IllegalStateException("DB_PASSWORD mangler")

// ❌ Hardkodet secret
val dbPassword = "supersecret123"
```

## Network Policy (Nais)

```yaml
spec:
  accessPolicy:
    inbound:
      rules:
        - application: frontend-app
    outbound:
      rules:
        - application: pdl-api
          namespace: pdl
          cluster: prod-gcp
      external:
        - host: api.external-service.no
```

## OWASP Top 10

### A01: Broken Access Control

```kotlin
// ✅ Sjekk at bruker har tilgang til ressursen
@GetMapping("/api/vedtak/{id}")
fun getVedtak(@PathVariable id: UUID): ResponseEntity<VedtakDTO> {
    val bruker = hentInnloggetBruker()
    val vedtak = vedtakService.findById(id)
    if (vedtak.brukerId != bruker.id) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
    }
    return ResponseEntity.ok(vedtak.toDTO())
}

// ❌ Ingen tilgangskontroll (IDOR)
@GetMapping("/api/vedtak/{id}")
fun getVedtak(@PathVariable id: UUID) = vedtakService.findById(id)
```

### A03: Injection

```kotlin
// ✅ Parameterisert spørring
jdbcTemplate.query("SELECT * FROM bruker WHERE fnr = ?", mapper, fnr)

// ❌ String-sammenslåing
jdbcTemplate.query("SELECT * FROM bruker WHERE fnr = '$fnr'", mapper)
```

### A05: Security Misconfiguration

```kotlin
// ✅ CORS kun for kjente domener
@Bean
fun corsFilter() = CorsFilter(CorsConfiguration().apply {
    allowedOrigins = listOf("https://my-app.intern.nav.no")
    allowedMethods = listOf("GET", "POST")
})

// ❌ Åpen CORS
allowedOrigins = listOf("*")
```

### A07: Cross-Site Scripting (XSS)

```tsx
// ✅ React escaper automatisk
<BodyShort>{bruker.navn}</BodyShort>

// ❌ Raw HTML injection
<div dangerouslySetInnerHTML={{ __html: userInput }} />
```

## Filopplasting

```kotlin
// ✅ Valider filtype, størrelse og magic bytes
fun validateUpload(file: MultipartFile) {
    require(file.size <= 10 * 1024 * 1024) { "Fil for stor (maks 10 MB)" }
    require(file.contentType in ALLOWED_TYPES) { "Ugyldig filtype" }
    val bytes = file.bytes.take(8).toByteArray()
    require(verifyMagicBytes(bytes, file.contentType!!)) { "Filinnhold matcher ikke type" }
}
```

## Avhengigheter

```bash
# Kotlin
./gradlew dependencyUpdates
./gradlew dependencyCheckAnalyze

# Node/TypeScript
npm audit
npm audit fix
```

## Sjekkliste

- [ ] SQL-spørringer er parameteriserte
- [ ] Ingen PII i logger (FNR, navn, adresse)
- [ ] Secrets kun fra miljøvariabler
- [ ] Nais accessPolicy er eksplisitt (ingen åpen inbound)
- [ ] CORS begrenset til kjente domener
- [ ] Input validert og sanitert
- [ ] Tilgangskontroll sjekker eierskap (ikke bare autentisering)
- [ ] Filopplasting validerer type, størrelse og innhold
- [ ] Avhengigheter oppdatert og sårbarhetsskannet
- [ ] `trivy repo .` uten HIGH/CRITICAL funn
- [ ] `zizmor` godkjent for alle GitHub Actions workflows
- [ ] Git-historikk ren for committede secrets

## Referanser

| Ressurs | Bruksområde |
|---------|-------------|
| [sikkerhet.nav.no](https://sikkerhet.nav.no) | Navs Golden Path for sikkerhet |
| auth-overview skill | JWT-validering, TokenX, ID-porten, Maskinporten |
