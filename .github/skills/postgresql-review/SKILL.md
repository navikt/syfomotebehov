---
description: PostgreSQL-gjennomgang — EXPLAIN ANALYZE, indekser, N+1-deteksjon, ytelsesproblemer og Flyway-migrasjoner
---
<!-- Managed by esyfo-cli. Do not edit manually. Changes will be overwritten.
     For repo-specific customizations, create your own files without this header. -->
# PostgreSQL Review

Gjennomgang av PostgreSQL-bruk i Nav-applikasjoner. Dekker spørringsoptimalisering, indeksering, anti-mønstre og migrasjoner.

## EXPLAIN-analyse

```sql
-- Alltid kjør EXPLAIN ANALYZE på tunge spørringer
EXPLAIN (ANALYZE, BUFFERS, FORMAT TEXT)
SELECT * FROM vedtak WHERE bruker_id = '...' AND status = 'AKTIV';

-- Se etter:
-- • Seq Scan på store tabeller → mangler indeks
-- • Nested Loop med høye rader → N+1-problem
-- • Sort med external merge → mangler minne eller indeks
-- • Buffers shared read høy → data ikke i cache
```

## Indeksstrategier

```sql
-- ✅ Indeks på kolonner brukt i WHERE, JOIN, ORDER BY
CREATE INDEX idx_vedtak_bruker_status ON vedtak (bruker_id, status);

-- ✅ Partial index for vanlige filtre
CREATE INDEX idx_vedtak_aktiv ON vedtak (bruker_id) WHERE status = 'AKTIV';

-- ✅ Covering index (unngår table lookup)
CREATE INDEX idx_vedtak_covering ON vedtak (bruker_id) INCLUDE (opprettet, status);

-- ✅ GIN-indeks for JSONB
CREATE INDEX idx_metadata_gin ON dokument USING gin (metadata);

-- ❌ Indeks på kolonner med lav kardinalitet
CREATE INDEX idx_vedtak_status ON vedtak (status); -- bare 3-4 verdier
```

## JSONB-mønstre

```sql
-- ✅ Spørringer mot JSONB med GIN-indeks
SELECT * FROM dokument WHERE metadata @> '{"type": "søknad"}';

-- ✅ Spesifikk nøkkeloppslag
SELECT metadata->>'type' FROM dokument WHERE id = '...';

-- ❌ Funksjonsbasert spørring uten indeks
SELECT * FROM dokument WHERE metadata->>'type' = 'søknad';
```

## Anti-mønstre

### N+1-spørringer

```kotlin
// ❌ N+1: én spørring per vedtak
val saker = sakRepository.findAll()
saker.forEach { sak ->
    val vedtak = vedtakRepository.findBySakId(sak.id) // N ekstra spørringer
}

// ✅ Batch-henting
val saker = sakRepository.findAll()
val vedtakBySakId = vedtakRepository.findBySakIdIn(saker.map { it.id })
    .groupBy { it.sakId }
```

### SELECT *

```sql
-- ❌ Henter alle kolonner
SELECT * FROM vedtak WHERE bruker_id = '...';

-- ✅ Hent kun det du trenger
SELECT id, status, opprettet FROM vedtak WHERE bruker_id = '...';
```

### Manglende LIMIT

```sql
-- ❌ Kan returnere millioner av rader
SELECT * FROM hendelse WHERE type = 'SYKMELDING';

-- ✅ Begrens resultatsettet
SELECT * FROM hendelse WHERE type = 'SYKMELDING'
ORDER BY opprettet DESC LIMIT 100;
```

## Connection pooling

```yaml
# Nais — HikariCP-konfigurasjon
spec:
  gcp:
    sqlInstances:
      - type: POSTGRES_15
        databases:
          - name: myapp-db
            envVarPrefix: DB
```

```kotlin
// ✅ HikariCP-defaults for Nav
HikariConfig().apply {
    maximumPoolSize = 5  // Start lavt, øk ved behov
    minimumIdle = 2
    connectionTimeout = 10_000
    idleTimeout = 300_000
    maxLifetime = 600_000
}
```

## Migrasjoner

For Flyway-migrasjoner og SQL-konvensjoner, se `flyway-migration`-skillen. Nøkkelpunkter:
- Bruk `TIMESTAMPTZ` (ikke `TIMESTAMP`) for alle tidsstempel-kolonner
- Indekser på alle FK-kolonner
- UUID primary keys med `gen_random_uuid()`

## Sjekkliste

- [ ] EXPLAIN ANALYZE kjørt på tunge spørringer
- [ ] Indekser på alle FK-kolonner og hyppig brukte WHERE-kolonner
- [ ] Ingen N+1-spørringer
- [ ] SELECT bare kolonner som trengs
- [ ] LIMIT på spørringer som kan returnere mange rader
- [ ] Connection pool riktig dimensjonert
- [ ] Migrasjoner er reversible der mulig
- [ ] Ingen `SELECT *` i produksjonskode

## Grenser

### ✅ Alltid
- EXPLAIN ANALYZE på tunge spørringer
- Indekser på FK-kolonner og hyppige WHERE-kolonner
- TIMESTAMPTZ for alle tidsstempel-kolonner
- LIMIT på spørringer som kan returnere mange rader

### ⚠️ Spør først
- Nye indekser på store tabeller (låsing i produksjon)
- Endring av connection pool-størrelse

### 🚫 Aldri
- `SELECT *` i produksjonskode
- N+1-spørringer
- `DROP TABLE` i produksjon uten backup-plan
- `TIMESTAMP` uten tidssone (bruk `TIMESTAMPTZ`)
