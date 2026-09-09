---
description: "Nav-spesifikke Kotlin-standarder — Gradle Version Catalog, Flyway, logging, metrikker"
applyTo: "**/*.kt"
---

# Kotlin — Nav-spesifikke standarder

- Avhengigheter via Gradle Version Catalog — sjekk `libs.versions.toml`
- Database: Flyway for migreringer, parameteriserte spørringer (aldri string-interpolasjon i SQL)
- Logging: Sjekk eksisterende loggemønster i repoet (`KotlinLogging`, `kv()`-felter, MDC)
- Metrikker: Micrometer / Prometheus
- Autentiseringstesting: MockOAuth2Server

## Rammeverk og lokal kontekst

Les `build.gradle.kts`, eksisterende ruter og tester for å fastslå rammeverket.
Følg relevante lokale Kotlin-/Spring-/Ktor-instruksjoner når de finnes. Ved
bruk av en skill, velg den fra den aktive sesjonens katalog.

## Bevar eksisterende struktur

Bevar eksisterende kodestruktur. Endre kun det oppgaven eksplisitt krever. Hvis diffen blir uforholdsmessig stor sammenlignet med oppgavens omfang, stopp og forklar før du fortsetter — ikke refaktorer på siden.
